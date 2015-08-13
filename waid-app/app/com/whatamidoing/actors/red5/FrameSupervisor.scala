package com.whatamidoing.actors.red5

import java.io.ByteArrayInputStream
import com.whatamidoing.actors.red5.services.Xuggler

import akka.actor.Actor
import akka.actor.ActorRef
import play.api.Logger

import akka.actor.Props
import javax.imageio.ImageIO
import sun.misc.BASE64Decoder
import com.whatamidoing.utils.ActorUtils
import com.whatamidoing.utils.ActorUtilsReader
import play.api.libs.json._
import com.whatamidoing.cypher.CypherWriterFunction
import scala.concurrent.Await
import scala.concurrent._
import scala.concurrent.duration.DurationInt
import akka.pattern.ask
import akka.pattern.AskTimeoutException
import akka.util.Timeout
import controllers.WhatAmIDoingController
import com.whatamidoing.cypher.CypherReaderFunction

import models.Messages._

class FrameSupervisor extends Actor {

  var videoEncoder : ActorRef = _

  val Tag: String = "FrameSupervisor"

  override def receive: Receive = {

    case RTMPCreateStream(message, token, sn) => {

      ActorUtils.invalidateAllStreams (token)
      val userInformation = ActorUtilsReader.fetchUserInformation(token)

      import play.api.Play
      implicit var currentPlay = Play.current
      val xmppDomain = Play.current.configuration.getString("xmpp.domain").get
      var domId = ""
      if (userInformation.domId == None) {
        domId = java.util.UUID.randomUUID.toString
        ActorUtils.updateUserInformation (token, domId)
        import com.whatamidoing.services.xmpp.AddHocCommands
        val domain = domId + "." + xmppDomain
        ActorUtils.createXmppDomain (domain)
      } else {
        domId = userInformation.domId.get
      }

     val streamName = sn + ".flv"
     // val streamName = sn

      var res = ActorUtils.createStream(token, streamName)

      val roomJid = sn + "@muc." + domId + "." + xmppDomain
      ActorUtils.associateRoomWithStream (token, roomJid)
      ActorUtils.createXmppGroup (roomJid, token)

      Logger ("FrameSupervisor.receive").info ("results from creating stream:" + res)
      videoEncoder = context.actorOf (VideoEncoder.props (streamName), "videoencoder:" + sn)
      videoEncoder ! EncodeFrame(message);
    }

    case RTMPMessage(message, token, streamId) => {
      if (videoEncoder != null)
        videoEncoder ! EncodeFrame(message)
    }

    case StopVideo(token) => {

          Logger(Tag).info("ACTOR FOUND STOPPING VIDEOENCODER[:" + token + "]")
          videoEncoder ! EndTransmission
          context.stop(videoEncoder)

          var streamName = ActorUtilsReader.findActiveStreamForToken(token)
          Logger(Tag).info("stream name:" + streamName)
          if (!streamName.isEmpty()) {
            var roomJid = ActorUtilsReader.getRoomJid(token)
            Logger(Tag).info("ROOM JID [:" + roomJid + "]")
            ActorUtils.removeRoom(roomJid)
            var res = ActorUtils.closeStream(streamName)
          }
          Logger.info("ACTOR FOUND STOPPING SELF-FRAMESUPERVISOR[:" + token + "]")
          context stop self
    }

    case x => Logger("FrameSupervisor.receive").info("DEFAULT_MESSAGE:" + x.toString())
  }
}

object FrameSupervisor {


  def props = Props(new FrameSupervisor())


}

