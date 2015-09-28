package com.whatamidoing.actors.hls

import akka.actor.{Actor, ActorLogging, Props}
import com.redis.RedisClient
import com.typesafe.config.ConfigFactory
import com.waid.redis.service.RedisUserService
import com.whatamidoing.actors.hls.model.Value.{AddToSegment, SegmentData, SegmentInfo, TooManyActiveStreams}
import models.Messages._

class  Segmentor(val streamName: String, val fps: Int) extends Actor with ActorLogging {

  import Segmentor.{redisPort, redisServer, segmentTime}

  val redisClient = new RedisClient(redisServer, redisPort)

  var segments: List[SegmentData] = List()
  var createSegment: List[SegmentInfo] = List()
  var segCounter = 1
  var time = 0
  var totalTime = 0
  var prevSegmentCount = 0

  val segmentDirectory: Option[String] = RedisUserService.getSegmentLocationOfStream(streamName)

  override def receive: Receive = {

    case AddToSegment(image, ts) =>
      val actSeg: Option[SegmentData] = getActiveSegment()

      var activeSegment: Segment = null
      if (actSeg == None) {

        for(sd <- segmentDirectory) {
          activeSegment = Segment(totalTime,sd,streamName + "_" + segCounter + ".ts",fps)
          activeSegment.init()
          val segData = SegmentData(activeSegment, segCounter, streamName)
          segCounter = segCounter + 1
          segments = segments :+ segData
          RedisUserService.updateStreamPlayListCount(streamName, segments.size)
        }

      } else {
        activeSegment = actSeg.get.segment
      }
      activeSegment.addImage(image, ts)

      time = ts + time
      totalTime = ts + totalTime
      log.info("-----total time of frames received:[" + totalTime+"] segment time["+time+"]")
      if ((time / 1000) > segmentTime) {
        activeSegment.duration = time / 1000
        activeSegment.close()
        time = 0
        log.info("reseting the time:")
      }

//Check to see if we should create a new segment

    case EndTransmission =>
      val actSeg: Option[SegmentData] = getActiveSegment()

      if (actSeg != None) {
        actSeg.get.segment.close()
        actSeg.get.segment.removeSegment
      }

  }


  def getActiveSegment(): Option[SegmentData] = {

    val activeSegments: List[SegmentData] = for (p <- segments; if p.segment.activeSegment == true) yield (p)

    var activeSegment: Option[SegmentData] = None

    if (activeSegments.size > 1) throw TooManyActiveStreams("NUMBER OF ACTIVE STREAMS=" + activeSegments.size)

    if (activeSegments.size == 1) {
      activeSegment = Option(activeSegments.head)
    }

    activeSegment
  }

}

object Segmentor {

  val config = ConfigFactory.load()
  val playListSize: Int = config.getInt("playlist.size")
  val redisServer = config.getString("redis.server")
  val redisPort = config.getInt("redis.port")
  val segmentTime = config.getInt("segment.time")


  def props(streamName: String,fps:Int): Props = {
    Props(classOf[Segmentor], streamName,fps)
  }

}