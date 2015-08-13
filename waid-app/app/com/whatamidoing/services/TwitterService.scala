package com.whatamidoing.services

import com.whatamidoing.utils.ActorUtilsReader
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.Play.current

class TwitterService() {

  def getTwitterReferers(streamId: String): List[Tuple2[Float, Float]] = {
    val linkedInReferers = ActorUtilsReader.getReferersForTwitter(streamId)
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


  def getTwitterViewersCount(token: String, streamId: String): Tuple3[String, String, String] = {
    //Getting info about linkedin
    val clause = "where s.name=\"" + streamId + "\""
    val twitterAccept = ActorUtilsReader.getTwitterViewers(token, clause).toInt
    var res = ("", "", "")
    if (twitterAccept > 0) {
      val accept = "(" + twitterAccept + ")"
      res = ("Twitter", "number of viewers", accept)
    } else {
      res = ("Twitter", "no viewers", "")
    }
    return res
  }

  def getTwitterCount(token: String, streamId: String): Tuple3[String, String, String] = {
    //Getting info about linkedin
    val clause = "where s.name=\"" + streamId + "\""
    val twitterInvites = ActorUtilsReader.countAllTwitterInvites(token, clause).toInt
    var res = ("", "", "")
    if (twitterInvites > 0) {
      val twitterAccept = ActorUtilsReader.getTwitterAcceptanceCount(token, clause).toInt
      if (twitterAccept > 0) {
        val accept = "(" + twitterAccept + ")"
        res = ("Twitter", "number of viewers", accept)
      } else {
        res = ("Twitter", "no viewers", "")

      }
    }

    return res
  }

  def getCountOfAllViewers(token: String, streamId: String): Int = {
    val streamClause = "where s.name=\"" + streamId + "\""
    val twitterAccept = ActorUtilsReader.getTwitterViewers(token, streamClause).toInt
    twitterAccept
  }

}


object TwitterService {
  def apply(): TwitterService = new TwitterService()
}