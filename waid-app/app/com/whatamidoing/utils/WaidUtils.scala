package com.whatamidoing.utils

import java.io.{PrintWriter, StringWriter}
import java.util.UUID

import com.fasterxml.uuid.impl.TimeBasedGenerator
import com.fasterxml.uuid.{EthernetAddress, Generators}

/**
 * Created by kodjobaah on 16/07/2015.
 */
object WaidUtils {

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
}
