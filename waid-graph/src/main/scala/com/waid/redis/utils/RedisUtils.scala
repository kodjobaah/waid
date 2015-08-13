package com.waid.redis.utils

import java.io.{PrintWriter, StringWriter}
import java.util.UUID

import com.fasterxml.uuid.{Generators, EthernetAddress}
import com.fasterxml.uuid.impl.TimeBasedGenerator
import com.waid.redis.KeyPrefixGenerator

/**
 * Created by kodjobaah on 16/07/2015.
 */
object RedisUtils {


  val gen: TimeBasedGenerator  = Generators.timeBasedGenerator(EthernetAddress.fromInterface())

  def getUUID(): UUID = {
    gen.generate()
  }

  def getEpochTime: Long = {
    System.currentTimeMillis()/1000
  }

  def stackTraceToString(e: Exception): String = {
    val sw: StringWriter = new StringWriter()
    e.printStackTrace(new PrintWriter(sw))
    sw.toString()
  }

  def getUserNodeFromUserToken(userToken: String): String = {
     var userId = userToken.split(":")(2)
     KeyPrefixGenerator.UserNodePrefix+userId.substring(1,userId.lastIndexOf("_"))
  }


  def getUserIdFromUserTokenId(userTokenId: String): String = {
    var toks = userTokenId.split(":")
    var id = toks(2)

    id.substring(id.indexOf("_")+1,id.lastIndexOf("_"))
  }

  def getStreamNodeFromUserToken(userToken: String): String = {
    var toks = userToken.split(":")
    var userId = toks(2)
    var tokenId = toks(3)

    KeyPrefixGenerator.UserStreamNodePrefix+userId+":_"+tokenId+"_"
  }

}
