package controllers

import java.util.UUID
import javax.inject.Inject

import com.typesafe.config.ConfigFactory
import com.waid.redis.{RedisDataStore, RedisReadOperations, KeyPrefixGenerator}
import com.waid.redis.model.UserNode
import com.waid.redis.service.{RedisUserAdminService, RedisUserService}
import play.api.i18n.Messages.Implicits._
import play.api.Play.current

import play.api.i18n.{Messages, Lang, MessagesApi, I18nSupport}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{JsBoolean, JsObject, Json}
import play.Logger

import scala.concurrent.Future

import java.text.DecimalFormat

import org.joda.time.DateTime

import com.whatamidoing.utils.ActorUtils
import com.whatamidoing.utils.ActorUtilsReader

import play.api.data._
import play.api.data.Forms._
import play.api.mvc._

import models._
import com.whatamidoing.services.LinkedinService
import com.whatamidoing.services.TwitterService
import com.whatamidoing.services.FacebookService

import com.whatamidoing.mail.EmailSenderService

class AdminController extends Controller {

  val config = ConfigFactory.load()
  val hostStreamer = config.getString("waid.servers.streamer.host")
  val portStreamer = config.getInt("waid.servers.streamer.port")

  val emailSenderService = EmailSenderService()

  val forgottenPasswordForm = Form(
    mapping(
      "email" -> nonEmptyText()
    )(ForgottenPassword.apply)(ForgottenPassword.unapply))

  val changePasswordForm = Form(
    mapping(
      "password" -> nonEmptyText(),
      "confirmPassword" -> nonEmptyText(),
      "changePasswordId" -> nonEmptyText()
    )(ChangePassword.apply)(ChangePassword.unapply))


  val userDetailsForm = Form(
    mapping(
      "email" -> optional(text),
      "firstName" -> nonEmptyText(),
      "lastName" -> nonEmptyText()
    )(UserDetails.apply)(UserDetails.unapply))




  def logout = Action.async {
    implicit request =>

      request.session.get("whatAmIdoing-authenticationToken").map {
        token =>
          val userNode = RedisUserService.checkIfTokenIsValid(token)
          for(user <- userNode) {
            val email = user.attributes get KeyPrefixGenerator.Email
            RedisDataStore.removeFromValidLogins(token,email)
          }

      }
      Future {Ok(views.html.welcome(Index.userForm)).withNewSession}
  }

  def getLiveStreamInvites(streamId: String, ref: String) = Action.async {
    implicit request =>

      val reference = ref.split(" ")(0)+".m3u8"

      val reload = UUID.randomUUID().toString()
      Future {Ok(views.html.livestreams(streamId,reference,reload))}


  }

  def getStreamInvites(streamId: String) = Action.async {
    implicit request =>

      request.session.get("whatAmIdoing-authenticationToken").map {
        token =>
          var acceptedUsersResults: Seq[(String, String, String)] = Seq()

          var response: Seq[(String, String, String)] = Seq()

          //Getting info about facebook
          var socialSites: Seq[(String, String, String)] = Seq()
          var referersFacebook = List[(Float, Float)]()

          //Getting info about twitter
          var referersTwitter = List[(Float, Float)]()

          var referersLinkedin = List[(Float, Float)]()

          val streamBroadCastLocations: List[Location] = List()
          val reference = "playlist.m3u8"
          Future {Ok(views.html.streamInvites(hostStreamer,portStreamer,streamId,token,socialSites, streamBroadCastLocations, acceptedUsersResults, response, referersLinkedin, referersTwitter, referersFacebook))}

      }.getOrElse {
        Future {Unauthorized(views.html.welcome(Index.userForm))}
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

  case class StreamDetails(result: (Option[BigDecimal], Option[BigDecimal], Option[BigDecimal], Option[String], Option[String]))

  def getStreams(start: String, end: String) = Action.async {
    implicit request =>

      request.session.get("whatAmIdoing-authenticationToken").map {
        token =>
          Logger.info("start[" + start + "] end [" + end + "]")


          val user = RedisUserService.checkIfTokenIsValid(token)

          if (user != None) {
            val email = RedisUserService.getAttributeFromNode(user.get, KeyPrefixGenerator.Email)

            var userTokenNode = RedisReadOperations.getUserTokenNodeId(token)

            val userStreams = RedisUserAdminService.getStreamsForUser(user.get, userTokenNode.get, start.trim().toLong, end.trim().toLong)

            var response: Seq[JsObject] = Seq()

            var streamCounter = 0
            userStreams.foreach {
              case (userStreamNode) =>

                val time = userStreamNode.attributes get KeyPrefixGenerator.CreatedDate
                val startTime: DateTime = new org.joda.time.DateTime(time.toLong * 1000)
                val streamId = userStreamNode.attributes get KeyPrefixGenerator.Token
                var title = "stream-"+streamCounter
                streamCounter = streamCounter + 1
                val json = Json.obj("id" -> streamId, "title" -> title, "start" -> startTime.toString, "end" -> startTime.toString, "allDay" -> JsBoolean(value = false))
                response = response :+ json

            }
            Logger.debug("events:" + response)
            Future {Ok(Json.toJson(response))}
          } else {
            Future {Unauthorized(views.html.welcome(Index.userForm))}
          }


      }.getOrElse {
        Future {Unauthorized(views.html.welcome(Index.userForm))}
      }


  }


  def getInvites = Action.async {
    implicit request =>

      request.session.get("whatAmIdoing-authenticationToken").map {
        token =>
          Future {Ok(views.html.invite())}
      }.getOrElse {
        Future {Unauthorized(views.html.welcome(Index.userForm))}
      }

  }


  def login = Action.async {
    implicit request =>

      Logger.info("login")
      val userForm = controllers.Index.userForm

      val bindForm = userForm.bindFromRequest
      bindForm.fold(
        formWithErrors => {
          Logger.info("login--errors")
          // binding failure, you retrieve the form containing errors:
          Future {BadRequest(views.html.welcome(formWithErrors))}
        },
        userData => {
          authenticateUser(bindForm, userData).fold(
            formWithErrors => Future {BadRequest(views.html.welcome(formWithErrors))},
            msg => {



              val user: Option[UserNode] = RedisUserService.findUser(userData.userName)
              RedisUserService.removeCurrentToken(userData.userName)
              RedisUserService.removePreviousValidLoginDetails(userData.userName)
              RedisUserService.removeValidStreamUsingEmail(userData.userName)
              val userTokenNode = RedisUserService.createToken(user)
              val token = RedisUserService.getAttributeFromNode(userTokenNode, KeyPrefixGenerator.Token)

              Logger.info("active token associated with user[" + token + "]")
              Future {Redirect(routes.AdminController.getInvites).withSession(
                "whatAmIdoing-authenticationToken" -> token.get)}

            })

        })

  }

  private def authenticateUser(form: Form[User], userData: User): Either[Form[User], String] = {
    val res:Option[UserNode] = RedisUserService.findUser(userData.userName)

    val either = userData match {
      case User(username, password) if (res != None) => {


        val regToken = RedisUserService.getAttributeFromNode(res.get, KeyPrefixGenerator.RegistrationToken)
        val userNode = RedisUserService.getUserFromRegistration(regToken)
        //Check to make user has completed the registration process
        if (userNode == None) {
          val hashPassword = RedisUserService.getAttributeFromNode(res.get, KeyPrefixGenerator.Password)
          validateUser(hashPassword.get, password)
        } else {
          Left(Seq(FormError("Email", "error.completeRegistration")))
        }
      }
      case _ => Left(Seq(FormError("Email", "error.notRegistered")))
    }

    either.fold(
      error => {
        val formWithErrors = Form(form.mapping, data = form.data,
          errors = error, value = form.value)
        Left(formWithErrors)
      },
      msg => Right("")
    )
  }

  def validateUser(passwordHash: String, suppliedPassword: String): Either[Seq[FormError], String] = {

    val res = validate(passwordHash, suppliedPassword)

    val answer = res match {
      case true => Right("")
      case false => Left(Seq(FormError("Email", "error.authenticationFailure")))
    }

    answer
  }

  def validate(passwordHash: String, suppliedPassword: String): Boolean = {
    import org.mindrot.jbcrypt.BCrypt
    try {
      if (BCrypt.checkpw(suppliedPassword, passwordHash)) {
        return true
      }
    } catch {
      case e: java.lang.IllegalArgumentException => return false
    }

    false

  }

}