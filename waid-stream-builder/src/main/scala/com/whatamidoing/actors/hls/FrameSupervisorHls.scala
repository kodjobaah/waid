package com.whatamidoing.actors.hls

import akka.actor.SupervisorStrategy.Stop
import akka.actor._
import com.whatamidoing.actors.hls.model.Value.{SampleData, FrameData}
import models.Messages.{EncodeFrame, EndTransmission, ProblemsEncoding}
import org.slf4j.{LoggerFactory, Logger}

import scala.collection.mutable.Queue

class FrameSupervisorHls(streamId: String) extends Actor  {

  var videoEncoder: ActorRef = _

  var log: Logger = LoggerFactory.getLogger(classOf[FrameSupervisorHls])

  val Tag: String = "FrameSupervisor"
  var token: String = _

  var theSender: ActorRef = _

  val ServiceStoppedMessage: String = "SERVICE_STOPPED"

  var audioQueue = new Queue[SampleData]

  override val supervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 0) {
      case _: Exception =>
        log info "problems with video encoder stopping"
        self ! ProblemsEncoding
        Stop
    }

  override def receive: Receive = {

    case ProblemsEncoding =>
        log.error("---------- something went wrong---")

    case e:EndTransmission =>
      videoEncoder ! e

    case sample: SampleData =>
      if (videoEncoder != null) {
        log.debug("-----FRAMESUPERISOER----SAMPLE")
        while (!audioQueue.isEmpty) {
          val leftOver = audioQueue.dequeue()
          videoEncoder ! leftOver
        }
        videoEncoder ! sample
      } else {
          audioQueue += sample
      }
    case frame: FrameData =>
      if (videoEncoder == null) {
          videoEncoder = context.actorOf(VideoEncoderHls.props(frame.streamId,frame.fps), "videoencoder:" + frame.streamId)
      }

      videoEncoder ! EncodeFrame(frame.data,frame.time,frame.sequence)

    case x => log debug ("DEFAULT_MESSAGE:" + x.toString)

  }

}

object FrameSupervisorHls {
  def props(streamId: String) = Props(new FrameSupervisorHls(streamId))

}

