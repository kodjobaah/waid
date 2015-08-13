package com.whatamidoing.cypher

import play.api.Logger
import org.joda.time.DateTime

object CypherWriterFunction {

  import models._
  import com.whatamidoing.actors.neo4j.Neo4JWriter._
  import org.anormcypher._

  def closeStream(stream: String): () => Neo4jResult = {
    val closeStream: Function0[Neo4jResult] = () => {
      val closeStream = Cypher(CypherWriter.closeStream(stream)).execute()

      val dt = new DateTime();
      val day = dt.getDayOfMonth();
      val time = dt.getHourOfDay() + ":" + dt.getMinuteOfHour + ":" + dt.getSecondOfMinute
      val dayDescription = "day " + day + " - month " + dt.getMonthOfYear() + "- year " + dt.getYear()
      val endStream = Cypher(CypherWriter.associateStreamCloseToDay(stream, dayDescription, time)).execute()

      val results: List[String] = List(closeStream.toString(), endStream.toString())
      val neo4jResult = new Neo4jResult(results)
     // Logger("CypherWriterFunction.closeStream").info("results from closing stream:" + results)
      neo4jResult
    }
    closeStream
  }

  def createTestStream2012(stream: String, token: String): () => Neo4jResult = {
    val createStream: Function0[Neo4jResult] = () => {
      val createStream = Cypher(CypherWriter.createStream(stream)).execute()

      val dt = new DateTime();
      val day = dt.getDayOfMonth();
      val dayDescription = "day " + day + " - month " + dt.getMonthOfYear() + "- year " + 2012
      val time = dt.getHourOfDay() + ":" + dt.getMinuteOfHour + ":" + dt.getSecondOfMinute

      val linkStreamToDay = Cypher(CypherWriter.linkStreamToDay(stream, dayDescription, time)).execute()

      val linkSteamToToken = Cypher(CypherWriter.linkStreamToToken(stream, token)).execute()

     // Logger("CypherWriterFunction.createStream").info("this is createStream: " + createStream)
     // Logger("CypherWriterFunction.createStream").info("this is linkStream: " + linkStreamToDay)
     // Logger("CypherWriterFunction.createUser").info("this is three: " + linkSteamToToken)

      val results: List[String] = List(createStream.toString(), linkStreamToDay.toString(), linkSteamToToken.toString())
      val neo4jResult = new Neo4jResult(results)
      neo4jResult

    }
    createStream

  }

  def createStream(stream: String, token: String): () => Neo4jResult = {
    val createStream: Function0[Neo4jResult] = () => {
      val createStream = Cypher(CypherWriter.createStream(stream)).execute()

      val dt = new DateTime();
      val day = dt.getDayOfMonth();
      val dayDescription = "day " + day + " - month " + dt.getMonthOfYear() + "- year " + dt.getYear()
      val time = dt.getHourOfDay + ":" + dt.getMinuteOfHour + ":" + dt.getSecondOfMinute

      val linkStreamToDay = Cypher(CypherWriter.linkStreamToDay(stream, dayDescription, time)).execute()

      val linkSteamToToken = Cypher(CypherWriter.linkStreamToToken(stream, token)).execute()

    //  Logger("CypherWriterFunction.createStream").info("this is createStream: " + createStream)
    //  Logger("CypherWriterFunction.createStream").info("this is linkStream: " + linkStreamToDay)
    //  Logger("CypherWriterFunction.createUser").info("this is three: " + linkSteamToToken)

      val results: List[String] = List(createStream.toString(), linkStreamToDay.toString(), linkSteamToToken.toString())
      val neo4jResult = new Neo4jResult(results)
      neo4jResult

    }
    createStream

  }
  def createUser(fn: String, ln: String, em: String, p: String): () => Neo4jResult = {

    import org.mindrot.jbcrypt.BCrypt

      val createUser: Function0[Neo4jResult] = () => {
      val pw_hash = BCrypt.hashpw(p, BCrypt.gensalt())
      val domJid = java.util.UUID.randomUUID.toString
      val newRes = Cypher(CypherWriter.createUser(fn, ln, em, pw_hash,domJid)).execute()

      val token = java.util.UUID.randomUUID.toString
      val valid = "true"

      val createToken = Cypher(CypherWriter.createToken(token, valid)).execute()
      val linkToken = Cypher(CypherWriter.linkUserToToken(em, token)).execute()

      val results: List[String] = List(newRes.toString(), createToken.toString(), linkToken.toString())
      val neo4jResult = new Neo4jResult(results)
      neo4jResult
    }
    createUser
  }

  def createInvite(stream: String, email: String, id: String): () => Neo4jResult = {

    val createInvite: Function0[Neo4jResult] = () => {

      val createInvite = Cypher(CypherWriter.createInvite(stream, email, id)).execute()
      val results: List[String] = List(createInvite.toString())
      val neo4jResult = new Neo4jResult(results)
      neo4jResult
    }

    createInvite
  }


  def createInviteTwitter(stream: String, twitter: String, id: String): () => Neo4jResult = {

    val createInviteTwitter: Function0[Neo4jResult] = () => {

      val createInviteTwitter = Cypher(CypherWriter.createInviteTwitter(stream, twitter, id)).apply().map(row => (row[Option[String]]("inviteId"))).toList
      val neo4jResult = new Neo4jResult(createInviteTwitter)
      neo4jResult
    }

    createInviteTwitter
  }

  def createInviteFacebook(stream: String, facebook: String, id: String): () => Neo4jResult = {

    val createInviteFacebook: Function0[Neo4jResult] = () => {

      val createInviteFacebook = Cypher(CypherWriter.createInviteFacebook(stream, facebook, id)).apply().map(row => (row[Option[String]]("inviteId"))).toList

      val neo4jResult = new Neo4jResult(createInviteFacebook)
      neo4jResult
    }

    createInviteFacebook
  }

  def createInviteLinkedin(stream: String, linkedin: String, id: String): () => Neo4jResult = {

    val createInviteLinkedin: Function0[Neo4jResult] = () => {

      val createInviteLinkedin = Cypher(CypherWriter.createInviteLinkedin(stream, linkedin, id)).apply().map(row => (row[Option[String]]("inviteId"))).toList

      val neo4jResult = new Neo4jResult(createInviteLinkedin)
      neo4jResult
    }

    createInviteLinkedin
  }




  def invalidateToken(token: String): () => Neo4jResult = {

    val invalidate: Function0[Neo4jResult] = () => {
      val invalidate = Cypher(CypherWriter.invalidateToken(token)).execute()

      val results: List[String] = List(invalidate.toString())
      val neo4jResult = new Neo4jResult(results)
      neo4jResult
    }

    invalidate
  }


  def invalidateAllTokensForUser(email: String): () => Neo4jResult = {

    val invalidate: Function0[Neo4jResult] = () => {
      val invalidate = Cypher(CypherWriter.invalidateAllTokensForUser(email)).execute()
       val results: List[String] = List(invalidate.toString())
      val neo4jResult = new Neo4jResult(results)
      neo4jResult
    }

    invalidate
  }

  
  def createTokenForUser(token: String, email: String): () => Neo4jResult = {
    val createTokenForUser: Function0[Neo4jResult] = () => {
      val createTokenForUser = Cypher(CypherWriter.createTokenForUser(token, email)).execute()

      val neo4jResult = new Neo4jResult(List(createTokenForUser.toString()))
      neo4jResult
    }

    createTokenForUser
  }

  def associateDayWithInvite(inviteId: String): () => Neo4jResult = {
    
    val associatedDayWithInvite: Function0[Neo4jResult] = () => {
      
      val dt = new DateTime();
      val day = dt.getDayOfMonth();
      val dayDescription = "day " + day + " - month " + dt.getMonthOfYear() + "- year " + dt.getYear()
      val time = dt.getHourOfDay() + ":" + dt.getMinuteOfHour + ":" + dt.getSecondOfMinute

      val assocaiteDayWithInvited = Cypher(CypherWriter.associateDayWithInvite(inviteId, dayDescription, time)).execute()

      val neo4jResult = new Neo4jResult(List(assocaiteDayWithInvited.toString()))
      neo4jResult
    }

    associatedDayWithInvite
  }


  def associateInviteTwitterWithReferer(inviteId: String, referal: String, sessionId: String): () => Neo4jResult = {
    
    val associatedInviteWithTwitterReferer: Function0[Neo4jResult] = () => {
      
      val dt = new DateTime();
      val day = dt.getDayOfMonth();
      val dayDescription = "day " + day + " - month " + dt.getMonthOfYear() + "- year " + dt.getYear()
      val time = dt.getHourOfDay() + ":" + dt.getMinuteOfHour + ":" + dt.getSecondOfMinute

      val assocaiteInviteTwitterWithReferer = Cypher(CypherWriter.associateInviteTwitterWithReferer(inviteId, dayDescription, time,referal,sessionId)).execute()

      val neo4jResult = new Neo4jResult(List(assocaiteInviteTwitterWithReferer.toString()))
      neo4jResult
    }

    associatedInviteWithTwitterReferer
  }

  def associateInviteFacebookWithReferer(inviteId: String, referal: String, sessionId: String): () => Neo4jResult = {
    
    val associatedInviteWithFacebookReferer: Function0[Neo4jResult] = () => {
      
      val dt = new DateTime();
      val day = dt.getDayOfMonth();
      val dayDescription = "day " + day + " - month " + dt.getMonthOfYear() + "- year " + dt.getYear()
      val time = dt.getHourOfDay() + ":" + dt.getMinuteOfHour + ":" + dt.getSecondOfMinute

      val assocaiteInviteFacebookWithReferer = Cypher(CypherWriter.associateInviteFacebookWithReferer(inviteId, dayDescription, time,referal,sessionId)).execute()

      val neo4jResult = new Neo4jResult(List(assocaiteInviteFacebookWithReferer.toString()))
      neo4jResult
    }

    associatedInviteWithFacebookReferer
  }

  def associateInviteLinkedinWithReferer(inviteId: String, referal: String, sessionId: String): () => Neo4jResult = {
    
    val associatedInviteWithLinkedinReferer: Function0[Neo4jResult] = () => {
      val dt = new DateTime();
      val day = dt.getDayOfMonth();
      val dayDescription = "day " + day + " - month " + dt.getMonthOfYear() + "- year " + dt.getYear()
      val time = dt.getHourOfDay() + ":" + dt.getMinuteOfHour + ":" + dt.getSecondOfMinute

      val assocaiteInviteLinkedinWithReferer = Cypher(CypherWriter.associateInviteLinkedinWithReferer(inviteId, dayDescription, time,referal,sessionId)).execute()

      val neo4jResult = new Neo4jResult(List(assocaiteInviteLinkedinWithReferer.toString()))
      neo4jResult
    }

    associatedInviteWithLinkedinReferer
  }


  def changePasswordRequest(email:String,changePasswordId: String): () => Neo4jResult = {
    
    val changePasswordRequest: Function0[Neo4jResult] = () => {
      
      val dt = new DateTime();
      val day = dt.getDayOfMonth();
      val dayDescription = "day " + day + " - month " + dt.getMonthOfYear() + "- year " + dt.getYear()
      val time = dt.getHourOfDay() + ":" + dt.getMinuteOfHour + ":" + dt.getSecondOfMinute

      val changePasswordRequest = Cypher(CypherWriter.changePasswordRequest(email, dayDescription, time,changePasswordId)).execute()

      val neo4jResult = new Neo4jResult(List(changePasswordRequest.toString()))
      neo4jResult
    }
    changePasswordRequest
  }


  def updatePassword(cpId: String,newPassword: String): () => Neo4jResult = {
    
    val updatePassword: Function0[Neo4jResult] = () => {
      
      val dt = new DateTime();
      val day = dt.getDayOfMonth();
      val dayDescription = "day " + day + " - month " + dt.getMonthOfYear() + "- year " + dt.getYear()
      val time = dt.getHourOfDay() + ":" + dt.getMinuteOfHour + ":" + dt.getSecondOfMinute

      val updatePassword = Cypher(CypherWriter.updatePassword(cpId, dayDescription, newPassword,time)).execute()

      val neo4jResult = new Neo4jResult(List(updatePassword.toString()))
      neo4jResult
    }
    updatePassword
  }


  def deactivatePreviousChangePasswordRequest(email: String) : () => Neo4jResult = {
    
    val deactivatePreviousChangePasswordRequest: Function0[Neo4jResult] = () => {
      
      val deactivatePreviousChangePasswordRequest = Cypher(CypherWriter.deactivatePreviousChangePasswordRequest(email)).execute()
       val neo4jResult = new Neo4jResult(List(deactivatePreviousChangePasswordRequest.toString()))
      neo4jResult
    }
    deactivatePreviousChangePasswordRequest
  }

  def updateUserDetails(token:String, firstName: String, lastName: String) : () => Neo4jResult = {

    val updateUserDetails: Function0[Neo4jResult] = () => {
      val updateUserDetails = Cypher(CypherWriter.updateUserDetails(token,firstName,lastName)).execute()
      val neo4jResult = new Neo4jResult(List(updateUserDetails.toString()))
      neo4jResult
    }
    updateUserDetails
  }

  def createLocationForStream(token: String, latitude: Double, longitude: Double): () => Neo4jResult = {

    val createLocationForStream: Function0[Neo4jResult] = () => {
      val createLocationForStream = Cypher(CypherWriter.createLocationForStream(token,latitude,longitude)).execute()
      Logger("CypherWriterFunction.createLocationForStream").info("this is createLocationForStream: " + createLocationForStream)
      val neo4jResult = new Neo4jResult(List(createLocationForStream.toString()))
      neo4jResult
    }
    createLocationForStream
  }

  def associateRoomWithStream(token: String, roomId: String): () => Neo4jResult = {
   val associateRoomWithStream : Function0[Neo4jResult] = () => {
      val associateRoomWithStream = Cypher(CypherWriter.associateRoomWithStream(token,roomId)).execute()
      val neo4jResult = new Neo4jResult(List(associateRoomWithStream.toString()))
      neo4jResult
    }
    associateRoomWithStream
  }


  def invalidateAllStreams(token: String): () => Neo4jResult = {

    val invalidate: Function0[Neo4jResult] = () => {
      val invalidate = Cypher(CypherWriter.invalidateAllStreams(token)).execute()

      val results: List[String] = List(invalidate.toString())
      val neo4jResult = new Neo4jResult(results)
      neo4jResult
    }

    invalidate
  }

  def updateUserInformation(token: String, domId: String): () => Neo4jResult = {

    val updateUserInformation: Function0[Neo4jResult] = () => {
      val userInformation = Cypher(CypherWriter.updateUserInformation(token,domId)).execute()

      val results: List[String] = List(userInformation.toString())
      val neo4jResult = new Neo4jResult(results)
      neo4jResult
    }

    updateUserInformation
  }



  def videoStreamStartedSocialMedia(sessionId: String): () => Neo4jResult = {

    val videoStreamStartedSocialMedia: Function0[Neo4jResult] = () => {
      
      val dt = new DateTime();
      val day = dt.getDayOfMonth();
      val dayDescription = "day " + day + " - month " + dt.getMonthOfYear() + "- year " + dt.getYear()
      val time = dt.getHourOfDay() + ":" + dt.getMinuteOfHour + ":" + dt.getSecondOfMinute

      val videoStreamStartedSocialMedia = Cypher(CypherWriter.videoStreamStartedSocialMedia(sessionId, dayDescription, time)).execute()

      val neo4jResult = new Neo4jResult(List(videoStreamStartedSocialMedia.toString()))
      neo4jResult
    }

    videoStreamStartedSocialMedia
  }

  def videoStreamStoppedSocialMedia(sessionId: String): () => Neo4jResult = {

    val videoStreamStoppedSocialMedia: Function0[Neo4jResult] = () => {
      
      val dt = new DateTime();
      val day = dt.getDayOfMonth();
      val dayDescription = "day " + day + " - month " + dt.getMonthOfYear() + "- year " + dt.getYear()
      val time = dt.getHourOfDay() + ":" + dt.getMinuteOfHour + ":" + dt.getSecondOfMinute

      val videoStreamStoppedSocialMedia = Cypher(CypherWriter.videoStreamStoppedSocialMedia(sessionId, dayDescription, time)).execute()

      val neo4jResult = new Neo4jResult(List(videoStreamStoppedSocialMedia.toString()))
      neo4jResult
    }
    videoStreamStoppedSocialMedia
  }

  def deactivateAllRefererStreamActions(sessionId: String): () => Neo4jResult = {

    val deactivateAllRefererStreamActions: Function0[Neo4jResult] = () => {
      
      val deactivateAllRefererStreamActions = Cypher(CypherWriter.deactivateAllRefererStreamActions(sessionId)).execute()

      val neo4jResult = new Neo4jResult(List(deactivateAllRefererStreamActions.toString()))
      neo4jResult
    }
    deactivateAllRefererStreamActions
  }



  def videoStreamStarted(inviteId: String): () => Neo4jResult = {

    val videoStreamStarted: Function0[Neo4jResult] = () => {
      
      val dt = new DateTime();
      val day = dt.getDayOfMonth();
      val dayDescription = "day " + day + " - month " + dt.getMonthOfYear() + "- year " + dt.getYear()
      val time = dt.getHourOfDay() + ":" + dt.getMinuteOfHour + ":" + dt.getSecondOfMinute

      val videoStreamStarted = Cypher(CypherWriter.videoStreamStarted(inviteId, dayDescription, time)).execute()

      val neo4jResult = new Neo4jResult(List(videoStreamStarted.toString()))
      neo4jResult
    }

    videoStreamStarted
  }

  def videoStreamStopped(inviteId: String): () => Neo4jResult = {

    val videoStreamStopped: Function0[Neo4jResult] = () => {
      
      val dt = new DateTime();
      val day = dt.getDayOfMonth();
      val dayDescription = "day " + day + " - month " + dt.getMonthOfYear() + "- year " + dt.getYear()
      val time = dt.getHourOfDay() + ":" + dt.getMinuteOfHour + ":" + dt.getSecondOfMinute

      val videoStreamStopped = Cypher(CypherWriter.videoStreamStopped(inviteId, dayDescription, time)).execute()

      val neo4jResult = new Neo4jResult(List(videoStreamStopped.toString()))
      neo4jResult
    }
    videoStreamStopped
  }

  def deactivateAllStreamActions(inviteId: String): () => Neo4jResult = {

    val deactivateAllStreamActions: Function0[Neo4jResult] = () => {
      
      val deactivateAllStreamActions = Cypher(CypherWriter.deactivateAllStreamActions(inviteId)).execute()
      val neo4jResult = new Neo4jResult(List(deactivateAllStreamActions.toString()))
      neo4jResult
    }
    deactivateAllStreamActions
  }




}