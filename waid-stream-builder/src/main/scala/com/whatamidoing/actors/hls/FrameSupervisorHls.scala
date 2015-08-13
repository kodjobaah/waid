package com.whatamidoing.actors.hls

import akka.actor.SupervisorStrategy.Stop
import akka.actor._
import com.whatamidoing.actors.hls.model.Value.FrameData
import models.Messages.{EncodeFrame, EndTransmission, ProblemsEncoding}

class FrameSupervisorHls(streamId: String) extends Actor with ActorLogging {

  var videoEncoder: ActorRef = _

  val Tag: String = "FrameSupervisor"
  var token: String = _

  var theSender: ActorRef = _

  val ServiceStoppedMessage: String = "SERVICE_STOPPED"


  override val supervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 0) {
      case _: Exception =>
        log info "problems with video encoder stopping"
        self ! ProblemsEncoding
        Stop
    }

  override def receive: Receive = {

    case ProblemsEncoding =>
        println("---------- something got wrong---")

    case e:EndTransmission =>
      videoEncoder ! e

    case frame: FrameData =>
      if (videoEncoder == null) {
          videoEncoder = context.actorOf(VideoEncoderHls.props(frame.streamId), "videoencoder:" + frame.streamId)
      }

      videoEncoder ! EncodeFrame(frame.data,frame.time,frame.sequence)

    case x => log debug ("DEFAULT_MESSAGE:" + x.toString)

  }

}

object FrameSupervisorHls {
  def props(streamId: String) = Props(new FrameSupervisorHls(streamId))

}

