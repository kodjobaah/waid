package com.waid.stream

import java.util.UUID

import akka.actor.Actor
import akka.event.Logging._
import com.waid.redis.KeyPrefixGenerator
import com.waid.redis.model.UserNode
import com.waid.redis.service.RedisUserService
import com.waid.stream.service.PlayListService
import spray.http.CacheDirectives.`no-cache`
import spray.http.HttpHeaders.{`Cache-Control`, RawHeader, `Content-Type`}
import spray.http.StatusCodes.{ClientError, ServerError}
import spray.routing._
import spray.http._
import MediaTypes._
import spray.routing.directives.LogEntry

// we don't implement our route structure directly in the service actor because
// we want to be able to test it independently, without having to spin up an actor
class MyServiceActor extends Actor with MyService {

  // the HttpService trait defines only one abstract member, which
  // connects the services environment to the enclosing actor or test
  def actorRefFactory = context

  // this actor only runs our route, but you could add
  // other things here, like request stream processing
  // or timeout handling
  def receive = runRoute(myRoute)
}


// this trait defines our service behavior independently from the service actor
trait MyService extends HttpService {

  val Js = "js"
  val Css = "css"
  val Flash = "flash"
  val Img = "img"
  val JwPlayerProviderDir = "web/flash/provider"

  val ImgDir = "web/img"

  val JwaplayerJsDir = "web/js/jwplayer"
  val JqueryJsDir = "web/js/jquery"
  val JqueryUiJsDir = "web/js/jquery-ui"
  val VideoJsDir = "web/js/video-js"
  val FlowplayerJsDir = "web/js/flowplayer"
  val VideoCssDir = "web/css/video-js"
  val MediaElementJsDir = "web/js/mediaelement"
  val MediaElementCssDir = "web/css/mediaelement"
  val FlowplayerCssDir = "web/css/flowplayer/skin"
  val JqueryUiCssDir = "web/css/jquery-ui/1.11.4"

  val crossdomain =
    """<?xml version="1.0"?>
      <!-- http://www.osmf.org/crossdomain.xml -->
      <!DOCTYPE cross-domain-policy SYSTEM "http://www.adobe.com/xml/dtds/cross-domain-policy.dtd">
      <cross-domain-policy>
          <allow-access-from domain="*" />
          <site-control permitted-cross-domain-policies="all"/>
      </cross-domain-policy>
    """.stripMargin
  import spray.http.Uri.Path._
  val myRoute =

    get {

      pathPrefix("validate" / Segment) { userToken: String =>

        val user: Option[UserNode] = RedisUserService.checkIfTokenIsValid(userToken)

        var result = "false"
        if (user != None) {
          result = "true"
        }
        complete {
          result
        }
      } ~
      pathPrefix("resource" / Segments) { segments: List[String] =>

        respondWithHeaders(`Cache-Control`(`no-cache`),
          RawHeader("Access-Control-Allow-Origin", "*"),
          RawHeader("Access-Control-Allow-Credentials", "true"),
          RawHeader("Access-Control-Allow-Methods","POST, GET, PUT, DELETE, OPTIONS")) {
          printf("---segments:" + segments)
          segments.head match {

            case Js => {

              if (segments.contains("video-js")) {

                if (segments.contains("m3u8")) {
                  getFromResource(VideoJsDir + "/" + "m3u8/" + segments.reverse.head)
                } else {
                  getFromResource(VideoJsDir + "/" + segments.reverse.head)
                }
              } else if (segments.contains("jwplayer")) {
                getFromResource(JwaplayerJsDir + "/" + segments.reverse.head)
              } else if (segments.contains("flowplayer")) {
                getFromResource(FlowplayerJsDir + "/" + segments.reverse.head)
              } else if (segments.contains("jquery")) {
                getFromResource(JqueryJsDir + "/" + segments.reverse.head)
              } else if (segments.contains("jquery-ui")) {
                getFromResource(JqueryUiJsDir + "/1.11.4/" + segments.reverse.head)
              } else if (segments.contains("mediaelement")) {
                getFromResource(MediaElementJsDir + "/" + segments.reverse.head)
              } else {
                failWith(new IllegalRequestException(StatusCodes.registerCustom(400, "resource not found", "resource not found").asInstanceOf[ClientError]))
              }

            }

            case Css => {
              if (segments.contains("video-js")) {
                if (segments.contains("font")) {
                  getFromResource(VideoCssDir + "/font/" + segments.reverse.head)
                } else {
                  getFromResource(VideoCssDir + "/" + segments.reverse.head)
                }
              } else if (segments.contains("flowplayer")) {
                if (segments.contains("img")) {
                  getFromResource(FlowplayerCssDir + "/img/" + segments.reverse.head)
                } else if (segments.contains("fonts")) {
                  getFromResource(FlowplayerCssDir + "/fonts/" + segments.reverse.head)
                } else {
                  getFromResource(FlowplayerCssDir + "/" + segments.reverse.head)
                }
              } else if (segments.contains("mediaelement")) {
                getFromResource(MediaElementCssDir + "/" + segments.reverse.head)
              } else if (segments.contains("jquery-ui")) {
                if (segments.contains("images")) {
                  getFromResource(JqueryUiCssDir + "/images/" + segments.reverse.head)
                } else {
                  getFromResource(JqueryUiCssDir + "/" + segments.reverse.head)
                }
              } else {
                failWith(new IllegalRequestException(StatusCodes.registerCustom(400, "resource not found", "resource not found").asInstanceOf[ClientError]))
              }
            }

            case Flash => {
              getFromResource(JwPlayerProviderDir + "/" + segments.reverse.head)
            }
            case Img => {
              if (segments.contains("waid")) {
                getFromResource(ImgDir + "/waid/" + segments.reverse.head)
              } else {
                failWith(new IllegalRequestException(StatusCodes.registerCustom(400, "resource not found", "resource not found").asInstanceOf[ClientError]))
              }
            }

            case _ =>
              failWith(new IllegalRequestException(StatusCodes.registerCustom(400, "resource not found", "resource not found").asInstanceOf[ClientError]))
          }
        }
      } ~
      pathPrefix("crossdomain.xml") {
        respondWithMediaType(`text/xml`) {
          complete {
            crossdomain
          }
        }
      } ~
      pathPrefix("favicon.ico") {
          getFromResource(ImgDir + "/favicon.ico")

      } ~
      pathPrefix("stream" / Segment) { streamId =>

        path("item" / Segment) { file =>
          get {
            var streamLocation = RedisUserService.getSegmentLocationOfStream(streamId)
            if (streamLocation != None){
              val tsFile = streamLocation.get + "/" + file
              respondWithMediaType(MediaType.custom("video/MP2T"))
              getFromFile(tsFile)
            } else {
              complete(" ")
            }
          }
        } ~
         path("reference" / Segment / "item" / Segment ) { (userReference,file) =>
          get {
            val streamNode = RedisUserService.getStoredStream(userReference,streamId)
            if (streamNode != None) {
              var streamLocation = streamNode.get.attributes get KeyPrefixGenerator.SegmentLocation
              val tsFile = streamLocation + "/" + file
              respondWithMediaType(MediaType.custom("video/MP2T"))
              getFromFile(tsFile)
            } else {
              complete(" ")
            }
          }
        } ~
        path("playlist" / Segments) { reference =>
          detach() {
            val playListService = PlayListService(streamId, reference.head)
            val result = playListService.generatePlayList()
            respondWithMediaType(MediaType.custom("application/x-mpegurl")) {
              complete(result._1)
           }
          }
        } ~
        path("playlistall" / Segment / "referred" / Segment) { (userToken,referred) =>
          detach() {
            val playListService = PlayListService(streamId, userToken)
            val result = playListService.generateAllPlayList()
           // respondWithMediaType(MediaType.custom("application/x-mpegURL")) {
              complete(result)
           // }
          }
        } ~
        path("waid" / Segment) { reference =>
            detach() {
              println("got-here")
              var streamId = ""
              val result = RedisUserService.getAllValidStreams()
              for (r <- result) {
                for ((k, v) <- r) {
                  streamId = k
                }
              }
              val ref = UUID.randomUUID().toString+".m3u8"
              respondWithMediaType(`text/html`) {
                complete {
                  com.waid.view.html.waid(streamId, ref,"hey").toString
                }
              }
            }
        } ~
        path("invite" / Segment) { reference =>
            detach() {
              println("got-here")
              val ref = reference.split(" ")(0)+".m3u8"
              println("reference-----------------["+ref+"]")
              respondWithMediaType(`text/html`) {
                val reload = UUID.randomUUID().toString


                respondWithHeader(RawHeader("Access-Control-Allow-Origin", "*")) {
                  complete {
                    com.waid.view.html.waid(streamId, ref, reload).toString
                  }
                }
              }
            }
        }
      }
    }
}
