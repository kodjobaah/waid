package com.whatamidoing.services.hls

import java.io._
import java.nio.charset.Charset
import java.nio.file.{Files, Paths}
import javax.sound.sampled._

import akka.actor._
import com.typesafe.config.ConfigFactory
import com.waid.redis.{RedisReadOperations, KeyPrefixGenerator}
import com.waid.redis.model.UserNode
import com.waid.redis.service.RedisUserService
import com.whatamidoing.actors.hls.FrameSupervisorHls
import com.whatamidoing.actors.hls.model.Value.{SampleData, FrameData}
import com.whatamidoing.utils.{ActorUtils, Compressor}
import models.Messages.EndTransmission
import org.joda.time.Seconds
import org.slf4j.{LoggerFactory, Logger}
import org.zeromq.ZMQ
import spray.caching.{Cache, LruCache}
import sun.misc.BASE64Decoder

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Await}
import scala.concurrent.duration._

/**
 * Created by kodjobaah on 04/08/2015.
 */
class StreamProcessor(context: ZMQ.Context) extends Thread {

  val log: Logger = LoggerFactory.getLogger(classOf[StreamProcessor])

  val cl = ActorUtils.getClass.getClassLoader
  val config = ConfigFactory.load()
  val priority = ActorSystem("priority", config, cl)

  val socket = context.socket(ZMQ.PAIR)
  socket.connect("inproc://backend")
  socket.monitor("inproc://monitor.req", ZMQ.EVENT_ALL)

  var framesupervisors: scala.collection.mutable.Map[String, ActorRef] = scala.collection.mutable.Map()

  // and a Cache for its result type
  val userNodeCache: Cache[Option[UserNode]] = LruCache()

  //TODO: Used to limit the amount of time for each stream
  val userFrameSupervisorCache: Cache[FrameSupervisorHls] = LruCache()

  var count = 0
  var streamCount = 0
  var theTime: Int = 0

  //The segment directory
  val segmentDirectory = config.getString("segment.directory")


  var pcmSamples = scala.collection.mutable.ArrayBuilder.make[Byte]()

  /*
  var audioFormat: AudioFormat  = new AudioFormat( AudioFormat.Encoding.PCM_SIGNED,
    44100, 16, 2, 1, 44100, false )

  val info: DataLine.Info =new DataLine.Info(classOf[SourceDataLine],audioFormat)
  val line = AudioSystem.getLine(info).asInstanceOf[SourceDataLine]
  */
  override def run() {

    var message: List[String] = List()
    while (!Thread.currentThread.isInterrupted) {
      message = retrieveRequest(socket)
      if (!message.isEmpty) {
        performOperation(message, socket)
      }

    }

  }

  // (providing a caching key)
  private def fetchUserNode[T](key: String): Future[Option[UserNode]] = userNodeCache(key) {
    RedisUserService.checkIfTokenIsValid(key)
  }

  def performOperation(message: List[String], socket: ZMQ.Socket) {
    var identifier: String = message.head
    val authId = identifier.substring(0, identifier.indexOf(":"))
    val type_id = identifier.substring(identifier.indexOf(":") + 1, identifier.length)
    var action: String = message.tail.head

    action match {
      case "INIT" =>
        log.debug("INIT-STRING receieved[" + identifier + "]")

      case "CONNECT" =>
        log.debug("CONNECT STRING RECEIVED:[" + identifier + "] authId[" + authId + "] typeId[" + type_id + "]")
        var userNodeFuture = fetchUserNode(authId)

        var streamId: Option[String] = None

        Await.result(userNodeFuture, 5 second).foreach { userNode: UserNode =>
          var email = userNode.attributes get KeyPrefixGenerator.Email

          streamId = Option("audio")
          if (type_id.equalsIgnoreCase("video")) {

            val prevStreamId = RedisReadOperations.getValidStreamUsingEmail(email)

            if (prevStreamId != None) {
              endStream(prevStreamId.get)
            }

            RedisUserService.removeValidStreamUsingEmail(email)
            var userStreamNode = RedisUserService.createStream(authId)
            for (usn <- userStreamNode) {
              RedisUserService.addStreamToUsersList(Option(userNode), userStreamNode)

              var foundSid: String = usn.attributes get KeyPrefixGenerator.Token
              if (foundSid != null) {
                val fs = priority.actorOf(FrameSupervisorHls.props(foundSid).withMailbox("prio-mailbox"), "frameSupervisor-" + foundSid)
                framesupervisors += foundSid -> fs
                streamId = Some(foundSid)
                RedisUserService.addStreamSequenceNumber(foundSid, userNode.genId, 0.toString)

                val segDirectory = segmentDirectory + "/" + userNode.genId + "/" + usn.genId
                Files.createDirectories(Paths.get(segDirectory))
                RedisUserService.addSegmentLocationToStream(usn.genId, segDirectory)

              }
            }
          }
        }

        socket.send(identifier, ZMQ.SNDMORE)
        if (streamId == None) {
          socket.send("TOKEN_NOT_VALID", ZMQ.SNDMORE)
          socket.send("TERMINATE")
        } else {
          socket.send("IS_VALID" + '\0', ZMQ.SNDMORE)
          socket.send(streamId.get + '\0')

        }
      case "END_STREAM" =>

        val bos = new BufferedOutputStream(new FileOutputStream("/tmp/sound.pcm"))
        Stream.continually(bos.write(pcmSamples.result()))
        bos.close()
        var streamId = message.tail.tail.head
        log.info("ENDING STREAM [" + streamId + "]")
        endStream(streamId)

      case "BROADCAST" =>
        //println("broadcasting stream")
        var streamId = message.tail.tail.head
        var time = message.tail.tail.tail.head
        var data = message.tail.tail.tail.tail.head
        var fps = message.tail.tail.tail.tail.tail.head
        log.debug("type_id[" + type_id + "] streamId [" + streamId + "] time=[" + time + "] data[" + data.length + "] fps[" + fps + "]")

        val framesupervisor = framesupervisors get streamId

        if ("VIDEO".equalsIgnoreCase(type_id)) {

          var frameData = FrameData(streamId, identifier, count, time.toInt, data, fps.toInt)
          count = count + 1

          for (fs <- framesupervisor)
            fs ! frameData
        } else {
          streamCount = streamCount + 1
          val samples = data.split(",").map(_.toShort)

          for (x <- samples) {
            val res = shortToBytes(x, pcmSamples)
          }

          val sampleFrames = SampleData(streamId, "authToken", streamCount, time.toInt, samples)

          for (fs <- framesupervisor)
            fs ! sampleFrames
          /*
          line.open(audioFormat)
          line.write(pcmSamples.result(),0,pcmSamples.result().length)
          */
          log.debug("AUDIO-RECEIVED:[" + samples.length + "]")
          log.debug("AUDIO-RECEIVED:[" + pcmSamples.result().length + "]")
        }

      case _ => log.debug("--------------MESSAGE NOT MATCH[" + action + "]")

    }
  }


  def endStream(streamId: String): Unit = {

    RedisUserService.endStream(streamId)
    val framesupervisor = framesupervisors get streamId
    if (framesupervisor != None) {
      log.debug("size before remvoe:" + framesupervisors.size)
      framesupervisor.get ! EndTransmission()
      framesupervisors -= streamId
      log.debug("size afer remvoe:" + framesupervisors.size)
    }

  }

  def shortToBytes(value: Short, samples: mutable.ArrayBuilder[Byte]) {
    val lower = value & 0xff
    val lowerByte = lower.toByte
    val upper = (value >>> 8) & 0xff
    val upperByte = upper.toByte
    samples += lowerByte
    samples += upperByte
  }

  def retrieveRequest(socket: ZMQ.Socket): List[String] = {
    var message: List[String] = List()
    var more: Boolean = false
    var reply: Array[Byte] = Array[Byte]()
    do {
      reply = socket.recv()
      if (reply != null) {
        val mess = new String(reply, Charset.forName("UTF-8"))
        message = message ::: List(mess)
      } else {
      }
      more = socket.hasReceiveMore
    } while (more)
    message
  }

}
