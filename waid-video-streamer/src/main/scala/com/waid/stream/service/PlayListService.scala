package com.waid.stream.service

import com.waid.redis.RedisReadOperations
import com.waid.redis.service.RedisUserService

/**
 * Created by kodjobaah on 08/08/2015.
 */
class PlayListService(val streamToken: String, val userReference: String) {


  val m3u3Header =
    "#EXT-X-VERSION:3\n#EXTM3U\n#EXT-X-TARGETDURATION:10\n#EXT-X-MEDIA-SEQUENCE:0\n"


  val emptyList = "#EXT-X-VERSION:3\n#EXTM3U\n#EXT-X-TARGETDURATION:10\n#EXT-X-MEDIA-SEQUENCE:0\n#EXT-X-ENDLIST\n"

  def getGenereatePlayList(): (String,Boolean,Int) = {
    var playList = emptyList
    var streamEnded = false
    var sequenceCount = 0

    val currentSequenceCount = RedisUserService.getStreamSequenceNumber(streamToken, userReference)

    def buildSubPlayList(sb: StringBuilder, pc: Int, start: Int): Unit = {
      for (count <- start until pc) {
        val seg = s"""#EXTINF:10.0,\n"""
        sb.append(seg)
        val strm = "/stream/%s/item/%s_%s.ts".format(streamToken,streamToken, count)
        sb.append(strm + "\n")
      }
    }

    if (currentSequenceCount != None) {

      var sb: StringBuilder = new StringBuilder()
      val playListCount = RedisUserService.getStreamPlayListCount(streamToken)
      //Just need to send an update
      for(pc <- playListCount) {
          buildSubPlayList(sb,pc,currentSequenceCount.get)
          playList = sb.toString
          sequenceCount = pc
      }

      if (playListCount == None) {
        playList = "#EXT-X-ENDLIST\n"
        streamEnded = true
      }

    } else {
      val sb: StringBuilder = new StringBuilder(m3u3Header)

      val playListCount = RedisUserService.getStreamPlayListCount(streamToken)
      sb.append(m3u3Header)
      for (pc <- playListCount) {
        //Fist Time access the stream -- so send back

        var start = 20
        if ((pc - start) < 0) {
          start = start - pc
        } else {
          start = pc - start
        }
        buildSubPlayList(sb, pc, start)
        playList = sb.toString
        sequenceCount = pc
      }


      //Means stream has ended
      if (playListCount == None) {
        playList = "#EXT-X-ENDLIST\n"
        streamEnded = true

      }
    }
    (playList,streamEnded,sequenceCount)
  }

}

object PlayListService {

  def apply(streamToken: String, reference: String) = new PlayListService(streamToken,reference)
}