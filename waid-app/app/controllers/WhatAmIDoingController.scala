package controllers

import java.util.UUID
import javax.inject.Inject

import com.waid.redis.utils.RedisUtils
import com.waid.redis.{RedisReadOperations, KeyPrefixGenerator, RedisDataStore}
import com.waid.redis.service.{RedisModelService, RedisUserService}
import models.ChangePassword
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Controller
import play.api.mvc.Action
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{Json, JsObject}
import play.api.mvc.Results._

import scala.concurrent.Future

import com.whatamidoing.utils.{WaidUtils, ActorUtils, ActorUtilsReader}
import com.whatamidoing.mail.EmailSenderService
import com.whatamidoing.services.FacebookService
import com.whatamidoing.services.TwitterService
import com.whatamidoing.services.LinkedinService


class WhatAmIDoingController @Inject()(val messagesApi: MessagesApi) extends Controller with I18nSupport {

  val changePasswordForm = Form(
    mapping(
      "password" -> nonEmptyText(),
      "confirmPassword" -> nonEmptyText(),
      "changePasswordId" -> nonEmptyText()
    )(ChangePassword.apply)(ChangePassword.unapply))

  val logger: Logger = Logger("controllers.WhatAmIDoingController")

  //object WhatAmIDoingController extends Controller {

  val Twitter: String = "TWITTER"
  val Facebook: String = "FACEBOOK"
  val Linkedin: String = "LINKEDIN"

  var emailSenderService = EmailSenderService()
  //NOTE: Not being used because makes the Controller stateful--will cause problems when clustering
  val openChannels = scala.collection.mutable.Map[String, play.api.libs.iteratee.Enumerator[String]]()


  def findAllInvites(tokenOption: Option[String]) = Action.async {
    implicit request =>

      val token = tokenOption.getOrElse("not-token-provided")

      if (!token.equalsIgnoreCase("not-token-provided")) {
        val res = ActorUtilsReader.findAllInvites(token)
        Future.successful(Ok(res.mkString(",")))
      } else {
        Future.successful(Ok("No token provided"))
      }

  }

  def getInviteList(tokenOption: Option[String]) = Action.async {
    implicit request =>

      val token = tokenOption.getOrElse("not-token-provided")

      if (!token.equalsIgnoreCase("not-token-provided")) {

        val acceptedUsers = ActorUtilsReader.getUsersWhoHaveAcceptedToWatchStream(token)
        val acceptedUsersResponse = acceptedUsers.asInstanceOf[List[(Option[String], Option[String], Option[String])]]

        var acceptedUsersResults: Seq[JsObject] = Seq()

        acceptedUsersResponse.foreach {
          case (email, firstName, lastName) =>
            val json = Json.obj("email" -> email, "firstName" -> firstName, "lastName" -> lastName)
            acceptedUsersResults = acceptedUsersResults :+ json
        }


        val resInstance = ActorUtilsReader.getUsersWhoHaveBeenInvitedToWatchStream(token)
        val res = resInstance.asInstanceOf[List[(Option[String], Option[String], Option[String])]]

        var response: Seq[JsObject] = Seq()

        res.foreach {
          case (email, firstName, lastName) =>
            if (!checkIfAccepted(acceptedUsersResponse, email.get)) {
              val json = Json.obj("email" -> email, "firstName" -> firstName, "lastName" -> lastName)
              response = response :+ json

            }
        }

        //Getting info about twitter
        val clause = "where s.state=\"active\""
        val twitterInvites = ActorUtilsReader.countAllTwitterInvites(token, clause).toInt
        if (twitterInvites > 0) {
          val twitterAccept = ActorUtilsReader.getTwitterAcceptanceCount(token, clause).toInt
          if (twitterAccept > 0) {
            val twitter = "number watching (" + twitterAccept + ")"
            val json = Json.obj("email" -> "Twitter", "firstName" -> twitter, "lastName" -> "")
            acceptedUsersResults = acceptedUsersResults :+ json
          } else {
            val json = Json.obj("email" -> "Twitter", "firstName" -> "", "lastName" -> "")
            response = response :+ json
          }
        }

        //Getting info about facebook
        val facebookInvites = ActorUtilsReader.countAllFacebookInvites(token, clause).toInt
        if (facebookInvites > 0) {
          val facebookAccept = ActorUtilsReader.getFacebookAcceptanceCount(token, clause).toInt
          if (facebookAccept > 0) {
            val facebook = "number watching (" + facebookAccept + ")"
            val json = Json.obj("email" -> "Facebook", "firstName" -> facebook, "lastName" -> "")
            acceptedUsersResults = acceptedUsersResults :+ json
          } else {
            val json = Json.obj("email" -> "Facebook", "firstName" -> "", "lastName" -> "")
            response = response :+ json
          }
        }

        //Getting info about linkedin

        val linkedinInvites = ActorUtilsReader.countAllLinkedinInvites(token, clause).toInt
        if (linkedinInvites > 0) {
          val linkedinAccept = ActorUtilsReader.getLinkedinAcceptanceCount(token, clause).toInt
          if (linkedinAccept > 0) {
            val linkedin = "number watching (" + linkedinAccept + ")"
            val json = Json.obj("email" -> "Linkedin", "firstName" -> linkedin, "lastName" -> "")
            acceptedUsersResults = acceptedUsersResults :+ json
          } else {
            val json = Json.obj("email" -> "Linkedin", "firstName" -> "", "lastName" -> "")
            response = response :+ json
          }
        }


        Logger.info("---accepted:" + acceptedUsersResults)
        Logger.info("---not accepted:" + response)

        val sendBack = Json.obj(
          "accepted" -> acceptedUsersResults,
          "notAccepted" -> response
        )
        Future.successful(Ok(sendBack))
      } else {
        Future.successful(Ok("No token provided"))
      }

  }


  def checkIfAccepted(all: List[(Option[String], Option[String], Option[String])], checkEmail: String): Boolean = {

    var found = false
    all.foreach {
      case (email, firstName, lastName) =>
        if (checkEmail.equalsIgnoreCase(email.get)) {
          found = true
        }

    }
    found
  }

  /**
   * Used to return the page for the user to view the stream
   */
  def invalidateToken(tokenOption: Option[String]) = Action.async {
    implicit request =>

      val token = tokenOption.getOrElse("no-token-provided")

      if (!token.equalsIgnoreCase("no-token-provided")) {
        val streamId = ActorUtilsReader.findActiveStreamForToken(token)
        if (!streamId.isEmpty) {
          ActorUtils.closeStream(streamId)
        }
        val valid = ActorUtils.invalidateToken(token)
        Future.successful(Ok(valid).withNewSession)
      } else {
        Future.successful(Ok("No token provided").withNewSession)
      }
  }

  /**
   * Used to return the  locations for the inviteId
   */
  def whatAreTheLocations(inviteId: String) = Action.async {
    implicit request =>

      var streamId = ""
      import models.Location
      var locations = List(Location())

      if (inviteId.endsWith(Linkedin)) {
        streamId = ActorUtilsReader.findStreamForInviteLinkedin(inviteId)
        locations = ActorUtilsReader.fetchLocationForActiveStreamLinkedin(inviteId)

      } else if (inviteId.endsWith(Twitter)) {
        streamId = ActorUtilsReader.findStreamForInviteTwitter(inviteId)
        locations = ActorUtilsReader.fetchLocationForActiveStreamTwitter(inviteId)

      } else if (inviteId.endsWith(Facebook)) {
        streamId = ActorUtilsReader.findStreamForInviteFacebook(inviteId)
        locations = ActorUtilsReader.fetchLocationForActiveStreamFacebook(inviteId)

      } else {
        streamId = ActorUtilsReader.findStreamForInvitedId(inviteId)
        if (!streamId.isEmpty) {
          locations = ActorUtilsReader.fetchLocationForActiveStream(inviteId)
        }
      }

      var listOfLocations = Seq[JsObject]()
      var result = Json.arr(listOfLocations)
      if (!streamId.isEmpty) {
        for (loc <- locations) {
          val l = Json.obj("lat" -> loc.latitude, "long" -> loc.longitude)
          listOfLocations = listOfLocations :+ l
        }
        result = Json.arr(listOfLocations)
      }
      System.out.println(result)
      Future.successful(Ok(result))
  }

  def videoStarted(sessionId: String, accessType: String) = Action.async {

    if (accessType.equalsIgnoreCase("SOCIALMEDIA")) {
      ActorUtils.deactivateAllRefererStreamActions(sessionId)
      ActorUtils.videoStreamStartedSocialMedia(sessionId)
    } else {
      ActorUtils.deactivateAllStreamActions(sessionId)
      ActorUtils.videoStreamStarted(sessionId)
    }
    Future.successful(Ok(Json.obj("" -> "").toString()))
  }

  def videoStopped(sessionId: String, accessType: String) = Action.async {

    if (accessType.equalsIgnoreCase("SOCIALMEDIA")) {
      ActorUtils.deactivateAllRefererStreamActions(sessionId)
      ActorUtils.videoStreamStoppedSocialMedia(sessionId)
    } else {
      ActorUtils.deactivateAllStreamActions(sessionId)
      ActorUtils.videoStreamStopped(sessionId)
    }

    Future.successful(Ok(Json.obj("" -> "").toString()))
  }


  /**
   * Used to return the page for the user to view the stream

   */
  def whatAmIdoing(invitedIdOption: Option[String]) = Action.async {
    implicit request =>

      import models.Location
      val invitedId = invitedIdOption.getOrElse("no-invited-id")
      var locations = List(Location())

      if (!invitedId.equalsIgnoreCase("no-invited-id")) {
        val sessionId = java.util.UUID.randomUUID.toString
        var streamId = ""
        var roomJid = ""
        var nickName = ""
        var accessType = "SOCIALMEDIA"
        if (invitedId.endsWith(Linkedin)) {
          val referer = request.headers.get("X-Forwarded-For").orElse(Option("127.0.0.1"))

          val res = ActorUtilsReader.checkToSeeIfFacebookInviteAcceptedAlreadyByReferer(invitedId, referer.get)
          if (res.size < 1) {
            ActorUtils.associatedInviteLinkedinWithReferer(invitedId, referer.get, sessionId)
          }
          streamId = ActorUtilsReader.findStreamForInviteLinkedin(invitedId)

          locations = ActorUtilsReader.fetchLocationForActiveStreamLinkedin(invitedId)
          nickName = "LinkedIn"

        } else if (invitedId.endsWith(Twitter)) {
          nickName = "Twitter"


          val referer = request.headers.get("X-Forwarded-For").orElse(Option("127.0.0.1"))

          val res = ActorUtilsReader.checkToSeeIfTwitterInviteAcceptedAlreadyByReferer(invitedId, referer.get)
          if (res.size < 1) {
            ActorUtils.associatedInviteTwitterWithReferer(invitedId, referer.get, sessionId)
          }
          streamId = ActorUtilsReader.findStreamForInviteTwitter(invitedId)
          locations = ActorUtilsReader.fetchLocationForActiveStreamTwitter(invitedId)

        } else if (invitedId.endsWith(Facebook)) {
          nickName = "Facebook"
          val referer = request.headers.get("X-Forwarded-For").orElse(Option("127.0.0.1"))
          val res = ActorUtilsReader.checkToSeeIfFacebookInviteAcceptedAlreadyByReferer(invitedId, referer.get)
          if (res.size < 1) {
            ActorUtils.associatedInviteFacebookWithReferer(invitedId, referer.get, sessionId)
          }
          streamId = ActorUtilsReader.findStreamForInviteFacebook(invitedId)
          locations = ActorUtilsReader.fetchLocationForActiveStreamFacebook(invitedId)

        } else {

          accessType = "EMAIL"
          val userInformation = ActorUtilsReader.getUserInformationUsingInviteId(invitedId)
          if (userInformation.firstName.length < 1) {
            nickName = "Friend"
          } else {
            nickName = userInformation.firstName
          }
          streamId = ActorUtilsReader.findStreamForInvitedId(invitedId)
          Logger.info("----------STREAM ID:" + streamId)
          if (!streamId.isEmpty) {
            ActorUtils.associatedInviteWithDayOfAcceptance(invitedId)
            locations = ActorUtilsReader.fetchLocationForActiveStream(invitedId)

          }
        }

        if (streamId.isEmpty) {
          Future.successful(Ok(views.html.whatamidoingnoinviteId()))
        } else {


          roomJid = ActorUtilsReader.getRoomJidForStream(streamId)
          //streamId = streamId.dropRight(3) + "m3u8"
          streamId = streamId + ".m3u8"

          nickName = sessionId + "-DIDLY-SQUAT-" + nickName
          Future.successful(Ok(views.html.whatamidoing(streamId, locations, invitedId, roomJid, nickName, sessionId, accessType)).withSession("whatAmIdoing-xmpp" -> sessionId))
        }
      } else {
        Future.successful(Ok(views.html.whatamidoingnoinviteId()))
      }
  }

  def getCountOfAllUsersWatchingStream(token: String) = Action.async {
    implicit request =>
      val valid = ActorUtilsReader.getValidToken(token)
      if (valid.asInstanceOf[List[String]].size > 0) {

        val streamName = ActorUtilsReader.streamNameForToken(token)
        if ((streamName != null) && (!streamName.isEmpty)) {
          val facebookService: FacebookService = FacebookService()
          val facebookCount: BigDecimal = facebookService.getCountOfAllViewers(token, streamName)

          val twitterService: TwitterService = TwitterService()
          val twitterCount: BigDecimal = twitterService.getCountOfAllViewers(token, streamName)


          val linkedinService: LinkedinService = LinkedinService()
          val linkedinCount: Int = linkedinService.getCountOfAllViewers(token, streamName)


          val totalUsersInvite = ActorUtilsReader.getEmailViewers(token, streamName).toInt

          val total = facebookCount + twitterCount + linkedinCount + totalUsersInvite


          Future.successful(Ok(total.toString()))
        } else {
          Future.successful(Ok("No active Stream"))
        }
      } else {
        Future.successful(Ok("TOKEN NOT VALID"))
      }


  }

  def createLocationForStream(token: String, latitude: Double, longitude: Double) = Action.async {
    implicit request =>
      val valid = ActorUtilsReader.getValidToken(token)
      if (valid.asInstanceOf[List[String]].size > 0) {
        ActorUtils.createLocationForStream(token, latitude, longitude)
        Future.successful(Ok("Location added"))
      } else {
        Future.successful(Ok("Unable to add Location"))
      }
  }

  def getRoomJid(token: String) = Action.async {

    implicit request =>
      val valid = ActorUtilsReader.getValidToken(token)
      if (valid.asInstanceOf[List[String]].size > 0) {
        val jid = ActorUtilsReader.getRoomJid(token)
        Logger.info("roomjid:" + jid)
        import models.UserDetails
        if (jid.size > 0) {
          val res: UserDetails = ActorUtilsReader.fetchUserDetails(token)
          val json = Json.obj("jid" -> jid, "nickname" -> res.firstName)
          Future.successful(Ok(json.toString()))
        } else {
          val json = Json.obj()
          Future.successful(Ok(json.toString()))
        }
      } else {
        val json = Json.obj()
        Future.successful(Ok(json.toString()))
      }
  }


  /**
   * *
   * Used to send an invite to some one to come and view the stream
   */
  def inviteTwitter(token: String) = Action.async {
    implicit request =>

      val node: Option[String] = RedisUserService.getStreamNodeId(token)

      if (node != None) {
        val reference = "twitter-" + UUID.randomUUID().toString() + ".m3u8"
        val reload = UUID.randomUUID().toString()
        Future {
          Ok(views.html.livestreams(token, reference, reload))
        }
      } else {
        Future {
          Ok(views.html.streamNotAvailable())
        }
      }
  }

  /**
   * *
   * Used to send an invite to some one to come and view the stream
   */
  def inviteLinkedin(token: String) = Action.async {
    implicit request =>

      val node: Option[String] = RedisUserService.getStreamNodeId(token)

      if (node != None) {
        val reference = "linkedin-" + UUID.randomUUID().toString() + ".m3u8"
        val reload = UUID.randomUUID().toString()
        Future {
          Ok(views.html.livestreams(token, reference, reload))
        }
      } else {
        Future {
          Ok(views.html.streamNotAvailable())
        }
      }
  }

  /**
   * *
   * Used to send an invite to some one to come and view the stream
   */
  def inviteFacebook(token: String) = Action.async {
    implicit request =>

      val node: Option[String] = RedisUserService.getStreamNodeId(token)

      if (node != None) {
        val reference = "facebook-" + UUID.randomUUID().toString() + ".m3u8"
        val reload = UUID.randomUUID().toString()
        Future {
          Ok(views.html.livestreams(token, reference, reload))
        }
      } else {
        Future {
          Ok(views.html.streamNotAvailable())
        }
      }
  }

  /**
   * *
   * Used to send an invite to some one to come and view the stream
   */
  def invite(tokenOption: Option[String], emailOption: Option[String]) = Action.async {
    implicit request =>

      val emails = emailOption.getOrElse("no-email-provided")
      val token = tokenOption.getOrElse("no-token-provided")

      if (!token.equalsIgnoreCase("no-token-provided")) {
        if (!emails.equalsIgnoreCase("no-email-provided")) {

          val userNode = RedisUserService.checkIfTokenIsValid(token)

          if (userNode != None) {
            val un = userNode.get
            val tokenNodeId = RedisReadOperations.getUserTokenNodeId(token).get

            val userId = RedisUtils.getUserIdFromUserTokenId(tokenNodeId)

            val streamCount = RedisReadOperations.getCounterValue(KeyPrefixGenerator.StreamCounter + userId)
            val streamNodePrefix = RedisUtils.getStreamNodeFromUserToken(tokenNodeId)
            val streamNodeId = streamNodePrefix + ":" + streamCount.get
            val streamNode = RedisReadOperations.populateStreamNode(streamNodeId, tokenNodeId)


            if (streamNode != None) {
              val streamToken = streamNode.get.attributes get KeyPrefixGenerator.Token
              val validStreamNode = RedisReadOperations.getStreamNodeIdFromValidStreams(streamToken)

              if (validStreamNode != None) {
                /*
               * Checking to see if invite is already in the system
              */

                Logger.info("emails[" + emails + "]")
                val listOfEmails = emails.split(",")

                Logger.info("LIST OF EMAILS [" + listOfEmails + "] size = [" + listOfEmails.size + "]")
                for (email <- listOfEmails) {

                  /*
                 * TODO: For those users that are not already registered with waid send them a registration email
                 */
                  /*
                val res = ActorUtilsReader.searchForUser(email)

                if (res.isEmpty) {
                  val password = "test"
                  ActorUtils.createUser("", "", email, password)
                  emailSenderService.sendRegistrationEmail(email, password)
                }
                */
                  RedisDataStore.addInviteEmail(streamNodeId, email)

                  val invitedId = java.util.UUID.randomUUID.toString
                  println("--------------------INVITED[" + invitedId + "]")
                  emailSenderService.sendInviteEmail(email, invitedId, streamNode.get, userNode.get)

                }

                Future.successful(Ok("Done"))

              } else {
                Future.successful(Ok("Unable to Invite No Stream"))
              }
            } else {
              Future.successful(Unauthorized("Stream does not exits"))
            }
          } else {
            Future.successful(Unauthorized("Unable To Invite"))
          }
        } else {
          Future.successful(BadRequest("No email provided"))
        }
      } else {
        Future.successful(BadRequest("No token provided"))
      }
  }


  val Tag: String = "WhatAmIDoingController"
}
