package com.waid.stream

import java.util.UUID

import akka.actor.Actor
import akka.event.Logging._
import com.waid.redis.service.RedisUserService
import com.waid.stream.service.PlayListService
import spray.http.HttpHeaders.{RawHeader, `Content-Type`}
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



  import spray.http.Uri.Path._
  val myRoute =

    get {
      pathPrefix("stream" / Segment) { streamId =>

        path("item" / Segment) { file =>
          get {
            var streamLocation = RedisUserService.getSegmentLocationOfStream(streamId)
            if (streamLocation != None) {
              val tsFile = streamLocation.get + "/" + file
              respondWithMediaType(MediaType.custom("video/MP2T"))
              getFromFile(tsFile)
            } else {
              complete(" ")
            }
          }
        } ~
        path("playlist" / Segment) { reference =>
          detach() {
            val playListService = PlayListService(streamId, reference)
            val result = playListService.getGenereatePlayList()
            if (!result._2) {
              RedisUserService.addStreamSequenceNumber(streamId, reference, result._3.toString)
            }
            respondWithMediaType(MediaType.custom("application/x-mpegURL")) {
              complete(result._1)
            }
          }
        }
      }
    }
}
