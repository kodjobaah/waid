package com.whatamidoing.actors.hls

import akka.actor.{Props, ActorRef, ActorLogging, Actor}

import models.Messages._

import com.whatamidoing.utils._
import com.whatamidoing.actors.hls.model.Value.{SampleData, FrameData, AddToSegment}
import org.slf4j.{LoggerFactory, Logger}

import scala.collection.mutable


class VideoEncoderHls(streamName: String, fps:Int) extends Actor  {

  var segmentor: ActorRef = null

  var log: Logger =  LoggerFactory.getLogger(classOf[VideoEncoderHls])

  /*
  object FrameDataJsonProtocol extends DefaultJsonProtocol {
    implicit val jsonFrameData = jsonFormat2(FrameData)
  }
  */

  val audioQueue = new mutable.Queue[SampleData]
  override def receive: Receive = {

    case fr: EncodeFrame =>
      import sun.misc.BASE64Decoder
      try {
        val frame = fr.frame
        val diff = fr.time

        val base64: BASE64Decoder = new BASE64Decoder()
        val bytes64: Array[Byte] = base64.decodeBuffer(frame)
        import java.io.ByteArrayInputStream
        val bais: ByteArrayInputStream = new ByteArrayInputStream(bytes64)
        import javax.imageio.ImageIO
        val bufferedImage = ImageIO.read(bais)

        if (segmentor == null) {
          segmentor =context.actorOf(Segmentor.props(streamName,fps), "segmentor:" + streamName)
        }
        segmentor ! AddToSegment(bufferedImage,diff)

      } catch {
        case ex: Throwable =>
          log.info("PROBLEMS SHOULD STOP:"+ex)
          sender ! ProblemsEncoding

      }

    case sample: SampleData =>
       if (segmentor != null) {
         while(!audioQueue.isEmpty) {
           val leftOver = audioQueue.dequeue
           segmentor ! leftOver
         }
         segmentor ! sample
       } else {
         audioQueue += sample
       }
    case e: EndTransmission =>
        segmentor != e
        context.stop(segmentor)
        segmentor = null

  }

  override def postStop() {

  }

}

object VideoEncoderHls {


  def props(streamName: String, fps: Int) : Props= {
    Props(classOf[VideoEncoderHls],streamName,fps)
  }

}

