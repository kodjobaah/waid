package com.waid.stream.service

import java.io.FileWriter

import com.typesafe.config.ConfigFactory
import com.waid.redis.{KeyPrefixGenerator, RedisReadOperations}
import com.waid.redis.service.RedisUserService

/**
 * Created by kodjobaah on 08/08/2015.
 */
class PlayListService(val streamToken: String, val userReference: String, val host: String, val port: Int) {
  val m3u8Header =
    "#EXTM3U\n#EXT-X-PLAYLIST-TYPE:EVENT\n#EXT-X-TARGETDURATION:10\n#EXT-X-VERSION:3\n#EXT-X-MEDIA-SEQUENCE:0\n"


  val m3u8VodHeader =
    "#EXTM3U\n#EXT-X-PLAYLIST-TYPE:VOD\n#EXT-X-TARGETDURATION:10\n#EXT-X-VERSION:3\n#EXT-X-MEDIA-SEQUENCE:0\n"


  val emptyList = "#EXTM3U\n#EXT-X-PLAYLIST-TYPE:EVENT\n#EXT-X-TARGETDURATION:10\n#EXT-X-VERSION:3\n#EXT-X-MEDIA-SEQUENCE:0\n#EXT-X-ENDLIST\n"

  def generateEndOfList() : String = {
    var sb: StringBuilder = new StringBuilder(emptyList)
    sb.toString
  }

  def generatePlayList(): (String, Boolean, Int) = {
    var playList = emptyList
    var streamEnded = false
    var sequenceCount = 0
    var mediaSequenceNumber = 0
    var playListCount:Option[Int] = None
    val currentSequenceCount = RedisUserService.getStreamSequenceNumber(streamToken, userReference)

    def buildSubPlayList(sb: StringBuilder, pc: Int, start: Int): Unit = {
      for (count <- start until pc) {
        val seg = s"""#EXTINF:10.0,\n"""
        sb.append(seg)
        val strm = "http://" + host + ":" + port + "/stream/%s/item/%s_%s.ts".format(streamToken, streamToken, count)
        sb.append(strm + "\n")
      }
    }

    if (currentSequenceCount != None) {

      var sb: StringBuilder = new StringBuilder()
      playListCount = RedisUserService.getStreamPlayListCount(streamToken)
      //Just need to send an update
      if (playListCount != None) {
        val sequenceFields = currentSequenceCount.get.split(":")
        val start = sequenceFields (0).toInt
        //if (sequenceFields.length > 1) {

        println("---playlist-count["+playListCount+"] currentSequencetCount ["+start+"] mediasequenceCount["+mediaSequenceNumber+"]")
        if (mediaSequenceNumber == 0) {
          sb.append(m3u8Header.replace("mediaSequenceNumber", mediaSequenceNumber.toString))
        }
        if (playListCount.get == start) {
          mediaSequenceNumber = currentSequenceCount.get.split(":")(1).toInt + 1
        }

        if (mediaSequenceNumber > 4) {
          mediaSequenceNumber = 0
        }

        buildSubPlayList(sb, playListCount.get, start)
        playList = sb.toString
        sequenceCount = playListCount.get
      }

    } else {
      val sb: StringBuilder = new StringBuilder()
      sb.append(m3u8Header.replace("mediaSequenceNunber",mediaSequenceNumber.toString))
      playListCount = RedisUserService.getStreamPlayListCount(streamToken)
      val pc: Int = playListCount.getOrElse(0)
      if (pc > 0) {
        //Fist Time access the stream -- so send back
        var start = 20
        if ((pc - start) < 0) {
          start = 0
        } else {
          start = pc - start
        }
        println("start=" + start)
        buildSubPlayList(sb, pc, start)
        playList = sb.toString
        sequenceCount = pc
      }
    }

    //Means stream has ended
    if (playListCount == None) {
        playList = "#EXT-X-ENDLIST\n"
        streamEnded = true

    } else {
        println("------------------sequenceCount:mediaSequenceNumber["+sequenceCount+":"+mediaSequenceNumber+"]")
        RedisUserService.addStreamSequenceNumber(streamToken,userReference,sequenceCount+":"+mediaSequenceNumber)
    }

    (playList, streamEnded, sequenceCount)
  }


  def generateAllPlayList(): String = {
    var playList = emptyList
    var streamEnded = false
    var sequenceCount = 0

    def buildSubPlayList(sb: StringBuilder, pc: Int, start: Int): Unit = {
      for (count <- start until pc) {
        val seg = s"""#EXTINF:10.0,\n"""
        sb.append(seg)
        val strm = "http://" + host + ":" + port + "/stream/%s/reference/%s/item/%s_%s.ts".format(streamToken,userReference, streamToken, count)
        sb.append(strm + "\n")
      }
    }
    val sb: StringBuilder = new StringBuilder(m3u8VodHeader)

    val storedStream = RedisUserService.getStoredStream(userReference,streamToken)
    if (storedStream != None) {
      val pc = storedStream.get.attributes get KeyPrefixGenerator.PlayListCount
      println("start=" + pc)
      buildSubPlayList(sb, pc.toInt, 1)
      sb.append("#EXT-X-ENDLIST\n")
      playList = sb.toString
      sequenceCount = pc.toInt
    }
    playList
  }
}

object PlayListService {

  val config = ConfigFactory.load()

  val host = config.getString("waid.servers.builder.host")
  val port = config.getInt("waid.servers.builder.port")

  def apply(streamToken: String, reference: String) = new PlayListService(streamToken, reference, host, port)
}