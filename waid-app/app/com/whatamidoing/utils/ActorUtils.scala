package com.whatamidoing.utils

import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.ask
import akka.util.Timeout
import play.api._
import play.api.mvc._
import scala.concurrent.Await

import scala.concurrent.duration._
import scala.concurrent.Future
import scala.concurrent.future
import com.whatamidoing.cypher.CypherWriterFunction
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import play.api.{Play, Logger}
import com.whatamidoing.actors.red5.FrameSupervisor
import com.whatamidoing.actors.xmpp.XmppSupervisor
import com.whatamidoing.actors.neo4j.Neo4JWriter
import com.whatamidoing.actors.neo4j.Neo4JReader
import com.typesafe.config.ConfigFactory
import models.Neo4jResult


object ActorUtils {

  val system = ActorSystem("whatamidoing-system")
  val cl = ActorUtils.getClass.getClassLoader
  val priority = ActorSystem("priority", ConfigFactory.load(), Play.classloader(Play.current))
  implicit val timeout = Timeout(10 seconds)
  var xmppSupervisor = system.actorOf(XmppSupervisor.props(), "xmppSupervisor")
  var neo4jwriter = system.actorOf(Neo4JWriter.props(), "neo-4j-writer-supervisor")
  var neo4jreader = system.actorOf(Neo4JReader.props(), "neo-4j-reader-supervisor")

  val frameSupervisors = scala.collection.mutable.Map[String, (ActorRef, String)]()

  val Tag: String = "ActorUtils"

  import models.Messages._

  import play.api.mvc.Results._

  def createUser(fn: String, ln: String, em: String, p: String) = {

    val createUser = CypherWriterFunction.createUser(fn, ln, em, p)

    val writeResponse: Future[Any] = ask(neo4jwriter, PerformOperation(createUser)).mapTo[Any]

    val writeResult: scala.concurrent.Future[Result] = writeResponse.flatMap(
    {
      case WriteOperationResult(results) =>

        val res = ActorUtilsReader.getUserToken(em)
        if (res != "-1")
          Future { Ok("USER CREATED - ADDED AUTHENTICATION TOKEN TO SESSISON").withSession(
            "whatAmIdoing-authenticationToken" -> res) }
        else
            Future { Ok("USER CREATE - AUTHENTICATION TOKEN NOT ADDED") }




    })
    writeResult
  }


  def getStringValueFronResult(results: Neo4jResult): String = {
    if (results.results.size > 0) {
      results.results.head.asInstanceOf[String]
    } else {
      ""
    }

  }

  def createInvite(stream: String, email: String, id: String) = {
    val createInvite = CypherWriterFunction.createInvite(stream, email, id)
    val writeResponse: Future[WriteOperationResult] = ask(neo4jwriter, PerformOperation(createInvite)).mapTo[WriteOperationResult]
    val streamName = writeResponse map {
      r => r match {
        case WriteOperationResult(results) =>
          getStringValueFronResult(results)
      }

    }
    streamName

  }

  def createInviteTwitter(stream: String, twitter: String, id: String) = {
    val createInviteTwitter = CypherWriterFunction.createInviteTwitter(stream, twitter, id)
    val writeResponse: Future[WriteOperationResult] = ask(neo4jwriter, PerformOperation(createInviteTwitter)).mapTo[WriteOperationResult]
    val inviteId = writeResponse map {
      r => r match {
        case WriteOperationResult(results) =>
          getStringValueFronResult(results)

      }
    }
    inviteId
  }

  def createInviteFacebook(stream: String, facebook: String, id: String) = {
    val createInviteFacebook = CypherWriterFunction.createInviteFacebook(stream, facebook, id)
    val writeResponse: Future[WriteOperationResult] = ask(neo4jwriter, PerformOperation(createInviteFacebook)).mapTo[WriteOperationResult]
    val inviteId = writeResponse map {
      r => r match {
        case WriteOperationResult(results) =>
          getStringValueFronResult(results)
      }

    }
    inviteId
  }

  def createInviteLinkedin(stream: String, linkedin: String, id: String) = {
    val createInviteLinkedin = CypherWriterFunction.createInviteLinkedin(stream, linkedin, id)
    val writeResponse: Future[WriteOperationResult] = ask(neo4jwriter, PerformOperation(createInviteLinkedin)).mapTo[WriteOperationResult]
    val inviteId = writeResponse map {
      r => r match {
        case WriteOperationResult(results) =>
          getStringValueFronResult(results)

      }
    }
    inviteId
  }


  def invalidateToken(token: String) = {
    val invalidateToken = CypherWriterFunction.invalidateToken(token)
    val writerResponse: Future[Any] = ask(neo4jwriter, PerformOperation(invalidateToken)).mapTo[Any]


    val res = Await.result(writerResponse, 10 seconds) match {
      case WriteOperationResult(results) =>
        if (results.results.size > 0) {
          results.results.head.asInstanceOf[String]
        } else {
          ""
        }

    }

    res
  }

  def invalidateAllTokensForUser(email: String) = {

    val invalidateToken = CypherWriterFunction.invalidateAllTokensForUser(email)
    val writerResponse: Future[Any] = ask(neo4jwriter, PerformOperation(invalidateToken)).mapTo[Any]

    val res = Await.result(writerResponse, 10 seconds) match {
      case WriteOperationResult(results) =>
        if (results.results.size > 0) {
          results.results.head.asInstanceOf[String]
        } else {
          ""
        }
    }

    res
  }

  def createTokenForUser(token: String, email: String) = {

    Logger("ActorUtils.createTokenForUser").info("token[" + token + "] email[" + email + "]")
    val createTokenForUser = CypherWriterFunction.createTokenForUser(token, email)
    val writerResponse: Future[Any] = ask(neo4jwriter, PerformOperation(createTokenForUser)).mapTo[Any]

    val res = Await.result(writerResponse, 10 seconds) match {
      case WriteOperationResult(results) =>
        if (results.results.size > 0) {
          results.results.head.asInstanceOf[String]
        } else {
          ""
        }
    }

    res
  }

  def associatedInviteWithDayOfAcceptance(inviteId: String) = {
    val associateInviteWithAcceptanceDay = CypherWriterFunction.associateDayWithInvite(inviteId)
    val writerResponse: Future[Any] = ask(neo4jwriter, PerformOperation(associateInviteWithAcceptanceDay)).mapTo[Any]

    val res = Await.result(writerResponse, 10 seconds) match {
      case WriteOperationResult(results) =>
        if (results.results.size > 0) {
          results.results.head.asInstanceOf[String]
        } else {
          ""
        }
    }

    res

  }

  def associatedInviteTwitterWithReferer(inviteId: String, referal: String, sessionId: String) = {
    val associateInviteTwitterWithReferer = CypherWriterFunction.associateInviteTwitterWithReferer(inviteId, referal, sessionId)
    val writerResponse: Future[Any] = ask(neo4jwriter, PerformOperation(associateInviteTwitterWithReferer)).mapTo[Any]

    val res = Await.result(writerResponse, 10 seconds) match {
      case WriteOperationResult(results) =>
        if (results.results.size > 0) {
          results.results.head.asInstanceOf[String]
        } else {
          ""
        }
    }

    res

  }

  def associatedInviteFacebookWithReferer(inviteId: String, referal: String, sessionId: String) = {
    val associateInviteFacebookWithReferer = CypherWriterFunction.associateInviteFacebookWithReferer(inviteId, referal, sessionId)
    val writerResponse: Future[Any] = ask(neo4jwriter, PerformOperation(associateInviteFacebookWithReferer)).mapTo[Any]

    val res = Await.result(writerResponse, 10 seconds) match {
      case WriteOperationResult(results) =>
        if (results.results.size > 0) {
          results.results.head.asInstanceOf[String]
        } else {
          ""
        }

    }
    res

  }

  def associatedInviteLinkedinWithReferer(inviteId: String, referal: String, sessionId: String) = {
    val associateInviteLinkedinWithReferer = CypherWriterFunction.associateInviteLinkedinWithReferer(inviteId, referal, sessionId)
    val writerResponse: Future[Any] = ask(neo4jwriter, PerformOperation(associateInviteLinkedinWithReferer)).mapTo[Any]

    val res = Await.result(writerResponse, 10 seconds) match {
      case WriteOperationResult(results) =>
        if (results.results.size > 0) {
          results.results.head.asInstanceOf[String]
        } else {
          ""
        }
    }

    res

  }


  def createStream(token: String, streamName: String): String = {
    // Logger("FrameSupervisor-receive").info("creating actor for token:"+streamName)

    val stream = CypherWriterFunction.createStream(streamName, token)
    val writeResponse: Future[Any] = ask(neo4jwriter, PerformOperation(stream)).mapTo[Any]

    val res = Await.result(writeResponse, 10 seconds) match {
      case WriteOperationResult(results) =>
        results.results.mkString

    }
    res
  }


  def closeStream(streamName: String) = {
    val closeStream = CypherWriterFunction.closeStream(streamName)
    val writeResponse: Future[Any] = ask(neo4jwriter, PerformOperation(closeStream)).mapTo[Any]

    Await.result(writeResponse, 10 seconds) match {
      case WriteOperationResult(results) =>
        results.results.mkString

    }
  }


  def changePasswordRequest(email: String, changePasswordId: String): String = {

    val stream = CypherWriterFunction.changePasswordRequest(email, changePasswordId)
    val writeResponse: Future[Any] = ask(neo4jwriter, PerformOperation(stream)).mapTo[Any]

    val res = Await.result(writeResponse, 10 seconds) match {
      case WriteOperationResult(results) =>
        results.results.mkString

    }
    res
  }

  def updatePassword(cpId: String, newPassword: String): String = {

    val stream = CypherWriterFunction.updatePassword(cpId, newPassword)
    val writeResponse: Future[Any] = ask(neo4jwriter, PerformOperation(stream)).mapTo[Any]

    val res = Await.result(writeResponse, 10 seconds) match {
      case WriteOperationResult(results) =>
        results.results.mkString
    }

    res
  }

  def deactivatePreviousChangePasswordRequest(email: String): String = {

    val stream = CypherWriterFunction.deactivatePreviousChangePasswordRequest(email)
    val writeResponse: Future[Any] = ask(neo4jwriter, PerformOperation(stream)).mapTo[Any]

    val res = Await.result(writeResponse, 10 seconds) match {
      case WriteOperationResult(results) =>
        results.results.mkString

    }
    res
  }

  def updateUserDetails(token: String, firstName: String, lastName: String): String = {

    val updateUserDetails = CypherWriterFunction.updateUserDetails(token, firstName, lastName)
    val updateUserDetailsResponse: Future[Any] = ask(neo4jwriter, PerformOperation(updateUserDetails)).mapTo[Any]

    val res = Await.result(updateUserDetailsResponse, 10 seconds) match {
      case WriteOperationResult(results) =>
        results.results.mkString

    }
    res
  }

  def createLocationForStream(token: String, latitude: Double, longitude: Double): String = {
    val createLocationForStream = CypherWriterFunction.createLocationForStream(token, latitude, longitude)
    val createLocationForStreamResponse: Future[Any] = ask(neo4jwriter, PerformOperation(createLocationForStream)).mapTo[Any]

    val res = Await.result(createLocationForStreamResponse, 10 seconds) match {
      case WriteOperationResult(results) =>
        results.results.mkString

    }
    res
  }


  def associateRoomWithStream(token: String, roomId: String): String = {
    val associateRoomWithStream = CypherWriterFunction.associateRoomWithStream(token, roomId)
    val associateRoomWithStreamResponse: Future[Any] = ask(neo4jwriter, PerformOperation(associateRoomWithStream)).mapTo[Any]

    val res = Await.result(associateRoomWithStreamResponse, 10 seconds) match {
      case WriteOperationResult(results) =>
        results.results.mkString

    }
    res
  }

  def invalidateAllStreams(token: String) = {

    val invalidateAllStreams = CypherWriterFunction.invalidateAllStreams(token)
    val writerResponse: Future[Any] = ask(neo4jwriter, PerformOperation(invalidateAllStreams)).mapTo[Any]

    val res = Await.result(writerResponse, 10 seconds) match {
      case WriteOperationResult(results) =>
        if (results.results.size > 0) {
          results.results.head.asInstanceOf[String]
        } else {
          ""
        }

    }
    res
  }


  def updateUserInformation(token: String, domId: String) = {

    val updateUserInformation = CypherWriterFunction.updateUserInformation(token, domId)
    val writerResponse: Future[Any] = ask(neo4jwriter, PerformOperation(updateUserInformation)).mapTo[Any]

    val res = Await.result(writerResponse, 10 seconds) match {
      case WriteOperationResult(results) =>
        if (results.results.size > 0) {
          results.results.head.asInstanceOf[String]
        } else {
          ""
        }

    }
    res
  }

  def createXmppDomain(domain: String) {
    import models.Messages.CreateXMPPDomainMessage
    val mess = CreateXMPPDomainMessage(domain)

    val response: Future[Any] = ask(xmppSupervisor, mess).mapTo[Any]
    import models.Messages.Done
    Await.result(response, 10 seconds) match {
      case Done(results) =>
        Logger.info("--------results from creating---:" + results)

    }

  }


  def createXmppGroup(roomJid: String, token: String) = {
    import models.Messages.CreateXMPPGroupMessage
    val message = CreateXMPPGroupMessage(roomJid, token)
    xmppSupervisor ! message
  }


  def createXmppRoom(roomJid: String) = {
    import models.Messages.CreateXMPPRoomMessage
    val message = CreateXMPPRoomMessage(roomJid)
    xmppSupervisor ! message
  }

  def removeRoom(roomJid: String) = {
    import models.Messages.RemoveXMPPRoomMessage
    val message = RemoveXMPPRoomMessage(roomJid)
    xmppSupervisor ! message
  }

  def videoStreamStartedSocialMedia(sessionId: String) = {
    val videoStreamStartedSocialMedia = CypherWriterFunction.videoStreamStartedSocialMedia(sessionId)
    val writerResponse: Future[Any] = ask(neo4jwriter, PerformOperation(videoStreamStartedSocialMedia)).mapTo[Any]

    val res = Await.result(writerResponse, 10 seconds) match {
      case WriteOperationResult(results) =>
        if (results.results.size > 0) {
          results.results.head.asInstanceOf[String]
        } else {
          ""
        }

    }
    res

  }

  def videoStreamStoppedSocialMedia(sessionId: String) = {
    val videoStreamStoppedSocialMedia = CypherWriterFunction.videoStreamStoppedSocialMedia(sessionId)
    val writerResponse: Future[Any] = ask(neo4jwriter, PerformOperation(videoStreamStoppedSocialMedia)).mapTo[Any]

    val res = Await.result(writerResponse, 10 seconds) match {
      case WriteOperationResult(results) =>
        if (results.results.size > 0) {
          results.results.head.asInstanceOf[String]
        } else {
          ""
        }
    }

    res

  }

  def deactivateAllRefererStreamActions(sessionId: String) = {
    val deactivateAllRefererStreamActions = CypherWriterFunction.deactivateAllRefererStreamActions(sessionId)
    val writerResponse: Future[Any] = ask(neo4jwriter, PerformOperation(deactivateAllRefererStreamActions)).mapTo[Any]

    val res = Await.result(writerResponse, 10 seconds) match {
      case WriteOperationResult(results) =>
        if (results.results.size > 0) {
          results.results.head.asInstanceOf[String]
        } else {
          ""
        }
    }

    res

  }


  def videoStreamStarted(inviteId: String) = {
    val videoStreamStarted = CypherWriterFunction.videoStreamStarted(inviteId)
    val writerResponse: Future[Any] = ask(neo4jwriter, PerformOperation(videoStreamStarted)).mapTo[Any]

    val res = Await.result(writerResponse, 10 seconds) match {
      case WriteOperationResult(results) =>
        if (results.results.size > 0) {
          results.results.head.asInstanceOf[String]
        } else {
          ""
        }
    }

    res

  }

  def videoStreamStopped(inviteId: String) = {
    val videoStreamStopped = CypherWriterFunction.videoStreamStopped(inviteId)
    val writerResponse: Future[Any] = ask(neo4jwriter, PerformOperation(videoStreamStopped)).mapTo[Any]

    val res = Await.result(writerResponse, 10 seconds) match {
      case WriteOperationResult(results) =>
        if (results.results.size > 0) {
          results.results.head.asInstanceOf[String]
        } else {
          ""
        }
    }

    res

  }

  def deactivateAllStreamActions(inviteId: String) = {
    val deactivateAllStreamActions = CypherWriterFunction.deactivateAllStreamActions(inviteId)
    val writerResponse: Future[Any] = ask(neo4jwriter, PerformOperation(deactivateAllStreamActions)).mapTo[Any]

    val res = Await.result(writerResponse, 10 seconds) match {
      case WriteOperationResult(results) =>
        if (results.results.size > 0) {
          results.results.head.asInstanceOf[String]
        } else {
          ""
        }
    }

    res

  }

  def stopRtmpMessage(message: StopVideo) {

    val token = message.token

    Logger(Tag).info("STOPING SELF")
    frameSupervisors get token match {

      case value: Option[_] =>
        value get match {
          case (actor, streamId) =>
            actor ! message
            frameSupervisors -= token

        }

    }

  }


  def sendRtmpMessage(message: RTMPMessage) {

    val token = message.token

    frameSupervisors get token match {

      case None =>

        //NOTE: If system goes down all active stream information will be lost
        val streamName = token + "--" + java.util.UUID.randomUUID.toString
        val createMessage = RTMPCreateStream(message.message, message.token, streamName)
        val frameSupervisor = priority.actorOf(FrameSupervisor.props.withMailbox("priority-dispatch"), "frameSupervisor-" + streamName)
        frameSupervisor ! createMessage
        frameSupervisors += token ->(frameSupervisor, streamName)

      case value: Option[_] =>
        value get match {
          case (actor, streamId) =>
            val newMessage = RTMPMessage(message.message, message.token, streamId.asInstanceOf[String])
            actor ! newMessage


        }


    }

  }


}