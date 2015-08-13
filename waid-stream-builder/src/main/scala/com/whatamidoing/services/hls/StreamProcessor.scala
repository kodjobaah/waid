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
import org.joda.time.Seconds
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
    var action: String = message.tail.head

    action match {
      case "CONNECT" =>
        println("CONNECT STRING RECEIVED:["+identifier+"]")
        var userNodeFuture = fetchUserNode(identifier)

        var streamId:Option[String] = None

        Await.result(userNodeFuture, 5 second).foreach { userNode: UserNode =>
          var email = userNode.attributes get KeyPrefixGenerator.Email
          println("USER_EMAIL[" + email + "]")
          RedisUserService.removeValidStreamUsingEmail(email)
          var userStreamNode = RedisUserService.createStream(identifier)
          for (usn <- userStreamNode) {
            var foundSid: String = usn.attributes get KeyPrefixGenerator.Token
            println("sid[" + foundSid + "]")
            if (foundSid != null) {
              println("STREAM_ID[" + foundSid + "]")
              val fs = priority.actorOf(FrameSupervisorHls.props(foundSid).withMailbox("prio-mailbox"), "frameSupervisor-" + foundSid)
              framesupervisors += foundSid -> fs
              streamId = Some(foundSid)
              RedisUserService.addStreamSequenceNumber(foundSid,userNode.genId,0.toString)

              val segDirectory = segmentDirectory+"/"+userNode.genId+"/"+usn.genId
              Files.createDirectories(Paths.get(segDirectory))
              RedisUserService.addSegmentLocationToStream(usn.genId,segDirectory)

            }
          }
        }

        socket.send(identifier, ZMQ.SNDMORE)
        println("BEFORE_CHECK["+streamId+"]")
        if (streamId == None) {
            socket.send("TOKEN_NOT_VALID", ZMQ.SNDMORE)
            socket.send("TERMINATE")
        } else {
          socket.send("IS_VALID", ZMQ.SNDMORE)
          socket.send(streamId.get)
        }

      case "END_STREAM" =>
        println("ENDING STREAM")
      /*
       streamId = message.tail.head
       val framesupervisor = framesupervisors get streamId
       if (framesupervisor != None) {
         println("size before remvoe:" + framesupervisors.size)
         framesupervisor.get ! EndTransmission()
         framesupervisors -= streamId
         println("size afer remvoe:" + framesupervisors.size)
       }
       socket.send("STREAM_ENDED")
       */
      case "BROADCAST" =>
        //println("broadcasting stream")
        var streamId = message.tail.tail.head
        var time = message.tail.tail.tail.head
        var data = message.tail.tail.tail.tail.head
        //println("streamId ["+streamId+"] time=["+time+"] data["+data.length+"]")


        val framesupervisor = framesupervisors get streamId
        var frameData = FrameData(streamId,identifier,count,time.toInt,data)
        count  = count + 1

         for(fs <- framesupervisor)
                  fs ! frameData

      case _ => println("--------------MESSAGE NOT MATCH["+action+"]")

      /*
      streamId = java.util.UUID.randomUUID.toString
      if (streamId.length < 1) {
        streamId = java.util.UUID.randomUUID.toString
        val fs = priority.actorOf(FrameSupervisorHls.props(streamId).withMailbox("priority-dispatch"), "frameSupervisor-" + streamId)
        framesupervisors += streamId -> fs
      }


      val framesupervisor = framesupervisors get streamId
      if (framesupervisor == None) {
        socket.send("terminate")
        //socket.close()
      } else {
        val authToken = msg.head
        msg = msg.tail
        val sequence = msg.head.toInt
        msg = msg.tail
        val time = msg.head.toInt
        msg = msg.tail
        val data = msg.head

        val f = FrameData(streamId, authToken, sequence, time, data)
        framesupervisor.get ! f
       socket.send(streamId)

      }*/
      //println("Message head["+message.head+"]")
      //socket.send()
      //socket.send(streamId)
      // println("following stremId sent["+streamId+"]")

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
