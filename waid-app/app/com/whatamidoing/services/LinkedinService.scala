package com.whatamidoing.services

import com.whatamidoing.utils.ActorUtilsReader
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.Play.current

class LinkedinService() {

  def getLinkedInReferers(streamId: String): List[Tuple2[Float, Float]] = {
    val linkedInReferers = ActorUtilsReader.getReferersForLinkedin(streamId)
    var url = "https://freegeoip.net/json/"
    var referersLinkedin = List[(Float, Float)]()
    linkedInReferers.foreach {
      case ip: String =>
        val mult = ip.split(",")
        mult.foreach {
          op =>

            val webserviceCall = url + op.trim
            import scala.concurrent._
            import scala.concurrent.duration._
            import play.api.libs.ws._
            val res = WS.url(webserviceCall).get().map {
              response => ((response.json \ "latitude").as[Float], (response.json \ "longitude").as[Float])
            }

            val result = Await.result(res, 5 seconds)
            referersLinkedin = referersLinkedin ::: List(result)
        }

    }
    referersLinkedin
  }

  def getLinkedInCount(token: String, streamId: String): Tuple3[String, String, String] = {
    //Getting info about linkedin
    val clause = "where s.name=\"" + streamId + "\""
    val linkedinInvites = ActorUtilsReader.countAllLinkedinInvites(token, clause).toInt
    var res = ("", "", "")
    if (linkedinInvites > 0) {
      val linkedinAccept = ActorUtilsReader.getLinkedinAcceptanceCount(token, clause).toInt
      if (linkedinAccept > 0) {
        val accept = "(" + linkedinAccept + ")"
        res = ("LinkedIn", "number of viewers", accept)
      } else {
        res = ("Linkedin", "no viewers", "")

      }
    }
    res
  }

  def getCountOfAllViewers(token: String, streamId: String): Int = {
    val streamClause = "where s.name=\"" + streamId + "\""
    val linkedinAccept = ActorUtilsReader.getLinkedinViewers(token, streamClause).toInt
    linkedinAccept
  }


}


object LinkedinService {
  def apply(): LinkedinService = new LinkedinService()
}