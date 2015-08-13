package com.whatamidoing.actors.hls.model

import java.awt.image.BufferedImage
import com.whatamidoing.actors.hls.Segment
import akka.actor.ActorRef

/**
 * Created by kodjobaah on 07/05/2014.
 */

object Value {

  case class SegmentData(val segment: Segment, val number: Int, val streamName: String)
  case class SegmentInfo(val duration: Long, val streamName: String, val sequenceNumber: Int)
  case class AddToSegment(val image: BufferedImage, val ts: Int)
  case class TooManyActiveStreams(val message: String) extends RuntimeException(message)
  case class GetPlayList(val sender: ActorRef, val streamName: String)
  case class GetTsFile(val sender: ActorRef, val streamName: String)
  case class ReadSegment()
  case class FinnishReadingSegment()
  case class FrameData(val streamId: String, val authToken: String, val sequence: Int, val time: Int, val data: String)
}
