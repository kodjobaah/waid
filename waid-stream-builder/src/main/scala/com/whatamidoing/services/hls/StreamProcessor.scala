package com.whatamidoing.services.hls

import java.io.File
import java.nio.charset.Charset
import java.nio.file.{Files, Paths}

import akka.actor._
import com.typesafe.config.ConfigFactory
import com.waid.redis.KeyPrefixGenerator
import com.waid.redis.model.UserNode
import com.waid.redis.service.RedisUserService
import com.whatamidoing.actors.hls.FrameSupervisorHls
import com.whatamidoing.actors.hls.model.Value.FrameData
import com.whatamidoing.utils.{ActorUtils, Compressor}
import models.Messages.EndTransmission
import org.joda.time.Seconds
import org.slf4j.{LoggerFactory, Logger}
import org.zeromq.ZMQ
import spray.caching.{Cache, LruCache}
import sun.misc.BASE64Decoder

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Await}
import scala.concurrent.duration._

/**
 * Created by kodjobaah on 04/08/2015.
 */
class StreamProcessor(context: ZMQ.Context) extends Thread {

  val logger: Logger  = LoggerFactory.getLogger(classOf[StreamProcessor])

  val cl = ActorUtils.getClass.getClassLoader
  val config = ConfigFactory.load()
  val priority = ActorSystem("priority", config, cl)

  val socket = context.socket(ZMQ.PAIR)
  socket.connect("inproc://backend")
  socket.monitor("inproc://monitor.req", ZMQ.EVENT_ALL)

  var framesupervisors: scala.collection.mutable. Map[String, ActorRef] = scala.collection.mutable.Map()

  // and a Cache for its result type
  val userNodeCache: Cache[Option[UserNode]] = LruCache()

  //TODO: Used to limit the amount of time for each stream
  val userFrameSupervisorCache: Cache[FrameSupervisorHls] = LruCache()

  var count = 0
  var theTime:Int = 0

  //The segment directory
  val segmentDirectory = config.getString("segment.directory")

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
    val authId = identifier.substring(0,identifier.indexOf(":"))
    val type_id = identifier.substring(identifier.indexOf(":")+1,identifier.length)
    var action: String = message.tail.head

    action match {
      case "INIT" =>
        logger.debug("INIT-STRING receieved["+identifier+"]")

      case "CONNECT" =>
        logger.debug("CONNECT STRING RECEIVED:["+identifier+"] authId["+authId+"] typeId["+type_id+"]")
        var userNodeFuture = fetchUserNode(authId)

        var streamId:Option[String] = None

        Await.result(userNodeFuture, 5 second).foreach { userNode: UserNode =>
          var email = userNode.attributes get KeyPrefixGenerator.Email

          streamId = Option("audio")
          if (type_id.equalsIgnoreCase("video")) {
          RedisUserService.removeValidStreamUsingEmail(email)
          var userStreamNode = RedisUserService.createStream(authId)
          for (usn <- userStreamNode) {
            RedisUserService.addStreamToUsersList(Option(userNode),userStreamNode)

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
          socket.send(streamId.get+'\0')

        }
      case "END_STREAM" =>
        var streamId = message.tail.tail.head
        logger.info("ENDING STREAM ["+streamId+"]")
        RedisUserService.endStream(streamId)

       val framesupervisor = framesupervisors get streamId
       if (framesupervisor != None) {
         println("size before remvoe:" + framesupervisors.size)
         framesupervisor.get ! EndTransmission()
         framesupervisors -= streamId
         println("size afer remvoe:" + framesupervisors.size)
       }


      case "BROADCAST" =>
        //println("broadcasting stream")
        var streamId = message.tail.tail.head
        var time = message.tail.tail.tail.head
        var data = message.tail.tail.tail.tail.head
        var dataType = message.tail.tail.tail.tail.tail.head;
        //println("streamId ["+streamId+"] time=["+time+"] data["+data.length+"]")


        val framesupervisor = framesupervisors get streamId

        if ("VIDEO".equalsIgnoreCase(type_id)) {
          var frameData = FrameData(streamId, identifier, count, time.toInt, data)
          count = count + 1

          for (fs <- framesupervisor)
            fs ! frameData
        } else {
          val samples = data.split(",").map(_.toInt)
          logger.debug("AUDIO-RECEIVED:["+samples.length+"]")
        }

      case _ => println("--------------MESSAGE NOT MATCH["+action+"]")

    }
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

  case class RequestHeader(id: String, message: String, time: Int)

}
