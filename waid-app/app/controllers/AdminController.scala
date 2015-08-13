package controllers

import javax.inject.Inject

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

import models.User
import models.ForgottenPassword
import models.ChangePassword
import models.UserDetails
import com.whatamidoing.services.LinkedinService
import com.whatamidoing.services.TwitterService
import com.whatamidoing.services.FacebookService

import com.whatamidoing.mail.EmailSenderService

class AdminController extends Controller {

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

  def updateUserDetails() = Action.async {
    implicit request =>

      request.session.get("whatAmIdoing-authenticationToken").map {
        token =>

          val bindForm = userDetailsForm.bindFromRequest

          bindForm.fold(
            formWithErrors => {
              Future {BadRequest(views.html.userdetails(formWithErrors, state = false))}
            },
            userData => {
              ActorUtils.updateUserDetails(token, userData.firstName, userData.lastName)
              val res = ActorUtilsReader.fetchUserDetails(token)
              val filledForm = userDetailsForm.fill(res)
              Future {Ok(views.html.userdetails(filledForm, state = true))}
            }
          )
      }.getOrElse {
        Future {Unauthorized(views.html.welcome(Index.userForm))}
      }


  }

  def fetchUserDetails = Action.async {
    implicit request =>

      request.session.get("whatAmIdoing-authenticationToken").map {
        token =>
          val res = ActorUtilsReader.fetchUserDetails(token)
          val filledForm = userDetailsForm.fill(res)
          Future {Ok(views.html.userdetails(filledForm, state = false))}
      }.getOrElse {
        Future {Unauthorized(views.html.welcome(Index.userForm))}
      }

  }

  def performPasswordChange = Action.async {
    implicit request =>

      val body: AnyContent = request.body
      System.out.println("--:" + body.asFormUrlEncoded.get)
      var found = false
      var changePasswordId = ""
      for ((k, v) <- body.asFormUrlEncoded.get) {
        if (k == "changePasswordId") {
          if (v.size > 0) {
            changePasswordId = v.head
            found = true
          }
        }
      }


      if (!found) {
        Future {Ok(views.html.welcome(Index.userForm))}
      } else {

        val change = ActorUtilsReader.checkToSeeIfCheckPasswordIdIsValid(changePasswordId)
        if (change.size == 0) {
          Future {Ok(views.html.invalidchangepasswordid())}
        } else {
          val bindForm = changePasswordForm.bindFromRequest

          bindForm.fold(
            formWithErrors => {
              // binding failure, retrieving the form containing errors
              Future {BadRequest(views.html.changePassword(formWithErrors, changePasswordId))}
            },
            userData => {
              System.out.println("just before")
              changePasswordForUser(bindForm, userData).fold(
                formWithErrors => Future {BadRequest(views.html.changePassword(formWithErrors, changePasswordId))},
                msg => {
                  Future {Ok(views.html.passwordchangeconfirmation())}
                })
            })
        }
      }
  }

  private def changePasswordForUser(form: Form[ChangePassword], userData: ChangePassword): Either[Form[ChangePassword], String] = {
    val either = userData match {
      case ChangePassword(password, confirmPassword, changePasswordId) if password == confirmPassword => updateUserPassword(password, changePasswordId)
      case _ => Left(Seq(FormError("password", "error.passwordnotmatch")))
    }

    either.fold(
      error => {
        Logger.info("Password", "passwordm athcet erro")
        val formWithErrors = Form(form.mapping, data = form.data,
          errors = error, value = form.value)
        Left(formWithErrors)
      },
      msg => Right("")
    )

  }

  private def updateUserPassword(password: String, changePasswordId: String): Either[Seq[FormError], String] = {
    Logger.info("------", "updating passowrd")
    import org.mindrot.jbcrypt.BCrypt
    val pw_hash = BCrypt.hashpw(password, BCrypt.gensalt())
    ActorUtils.updatePassword(changePasswordId, pw_hash)
    Right("")
  }

  def changePassword(changePasswordId: String) = Action.async {
    implicit request =>


      val change = ActorUtilsReader.checkToSeeIfCheckPasswordIdIsValid(changePasswordId)
      if (change.size == 0) {
        Future {Ok(views.html.invalidchangepasswordid())}
      } else {
        Future {Ok(views.html.changePassword(changePasswordForm, changePasswordId))}
      }

  }



  private def findUserForForgottenPassword(form: Form[ForgottenPassword], userData: ForgottenPassword): Either[Form[ForgottenPassword], String] = {
    val either = userData match {
      case ForgottenPassword(email) if !ActorUtilsReader.searchForUser(userData.email).isEmpty => sendForgottenPasswordEmail(email)
      case _ => Left(Seq(FormError("email", "error.notRegistered")))
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

  private def sendForgottenPasswordEmail(email: String): Either[Seq[FormError], String] = {

    //Deactivating all previous request
    ActorUtils.deactivatePreviousChangePasswordRequest(email)
    val changePasswordId = java.util.UUID.randomUUID.toString
    ActorUtils.changePasswordRequest(email, changePasswordId)
    emailSenderService.sendLinkToChangePassword(email, changePasswordId)
    Right("")
  }

  def forgottenPassword = Action.async {
    implicit request =>
      Future {Ok(views.html.forgottenPassword(forgottenPasswordForm))}


  }

  def logout = Action.async {
    implicit request =>

      request.session.get("whatAmIdoing-authenticationToken").map {
        token =>
          ActorUtils.invalidateToken(token)
      }
      Future {Ok(views.html.welcome(Index.userForm)).withNewSession}
  }

  def getStreamInvites(streamId: String) = Action.async {
    implicit request =>

      request.session.get("whatAmIdoing-authenticationToken").map {
        token =>

          val acceptedUsers = ActorUtilsReader.getUsersWhoHaveAcceptedToWatchStreamUsingStreamId(streamId)
          val acceptedUsersResponse = acceptedUsers.asInstanceOf[List[(Option[String], Option[String], Option[String])]]

          var acceptedUsersResults: Seq[(String, String, String)] = Seq()

          acceptedUsersResponse.foreach {
            case (email, firstName, lastName) =>
              val value = (email.getOrElse("noeamil@noeamil.com"), firstName.getOrElse("nofirstname"), lastName.getOrElse("nolastname"))
              acceptedUsersResults = acceptedUsersResults :+ value

          }


          val resInstance = ActorUtilsReader.getUsersWhoHaveBeenInvitedToWatchStreamUsingStreamId(streamId)
          val res = resInstance.asInstanceOf[List[(Option[String], Option[String], Option[String])]]

          var response: Seq[(String, String, String)] = Seq()

          res.foreach {
            case (email, firstName, lastName) =>
              if (!checkIfAccepted(acceptedUsersResponse, email.get)) {
                val value = (email.getOrElse("noeamil@noeamil.com"), firstName.getOrElse("nofirstname"), lastName.getOrElse("nolastname"))
                response = response :+ value
              }

          }

          //Getting info about facebook
          var socialSites: Seq[(String, String, String)] = Seq()
          val facebookService: FacebookService = FacebookService()
          val facebookCountResults = facebookService.getFacebookCount(token, streamId)
          var referersFacebook = List[(Float, Float)]()

          if (facebookCountResults._1.size > 1) {
            socialSites = socialSites :+ facebookCountResults
            if (facebookCountResults._3.size > 1) {
              referersFacebook = facebookService.getFacebookReferers(streamId)
            }
          }

          //Getting info about twitter
          val twitterService: TwitterService = TwitterService()
          val twitterCountResults = twitterService.getTwitterCount(token, streamId)
          var referersTwitter = List[(Float, Float)]()
          if (twitterCountResults._1.size > 1) {
            socialSites = socialSites :+ twitterCountResults
            if (twitterCountResults._3.size > 1) {
              referersTwitter = twitterService.getTwitterReferers(streamId)
            }
          }

          val linkedinService: LinkedinService = LinkedinService()
          val countResults = linkedinService.getLinkedInCount(token, streamId)
          var referersLinkedin = List[(Float, Float)]()
          if (countResults._1.size > 1) {
            socialSites = socialSites :+ countResults
            if (countResults._3.size > 1) {
              referersLinkedin = linkedinService.getLinkedInReferers(streamId)
            }
          }

          val streamBroadCastLocations = ActorUtilsReader.fetchLocationForStream(streamId)
          Future {Ok(views.html.streamInvites(socialSites, streamBroadCastLocations, acceptedUsersResults, response, referersLinkedin, referersTwitter, referersFacebook))}

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

          val startTime: DateTime = new org.joda.time.DateTime(java.lang.Long.valueOf(start.trim()) * 1000)
          val endTime: DateTime = new org.joda.time.DateTime(java.lang.Long.valueOf(end.trim()) * 1000)
          Logger.info("start[" + startTime + "] end [" + endTime + "]")


          val y = startTime.getYear
          val m = startTime.getMonthOfYear
          val d = startTime.getDayOfMonth

          val yend = endTime.getYear
          val mend = endTime.getMonthOfYear
          val dend = endTime.getDayOfMonth

          val email = ActorUtilsReader.getEmailUsingToken(token)
          Logger.info("THIS IS THE EMAIL:" + email)

          val resInstanceEnded = ActorUtilsReader.getStreamsForCalendarThatHaveEnded(email, y, yend, m, mend, d, dend)
          val resEnded = resInstanceEnded.asInstanceOf[List[(Option[BigDecimal], Option[BigDecimal], Option[BigDecimal], Option[String], Option[String], Option[String])]]

          val resInstance = ActorUtilsReader.getStreamsForCalendar(email, y, yend, m, mend, d, dend)
          val res = resInstance.asInstanceOf[List[(Option[BigDecimal], Option[BigDecimal], Option[BigDecimal], Option[String], Option[String], Option[String])]]

          var response: Seq[JsObject] = Seq()

          res.foreach {
            case (year, month, day, time, streamId, streamName) =>

              val dateString = formatDate(year.getOrElse(0), month.getOrElse(0), day.getOrElse(0), time.getOrElse("00:00:00"))
              var json = Json.obj("id" -> streamId, "title" -> streamName, "start" -> dateString, "allDay" -> JsBoolean(value = false))

              checkIfStreamHasEnded(resEnded, streamId.get) match {
                case (Some(y), Some(m), Some(d), Some(t), Some(sId)) =>
                  val endDateString = formatDate(y, m, d, t)
                  json = Json.obj("id" -> streamId, "title" -> streamName, "start" -> dateString, "end" -> endDateString, "allDay" -> JsBoolean(value = false))
                case _ =>


              }
              response = response :+ json


          }
          Logger.info("events:" + response)
          Future {Ok(Json.toJson(response))}
      }.getOrElse {
        Future {Unauthorized(views.html.welcome(Index.userForm))}
      }


  }


  def formatDate(year: BigDecimal, month: BigDecimal, day: BigDecimal, time: String): String = {

    val yearFormatter: DecimalFormat = new DecimalFormat("0000")
    val monthFormatter: DecimalFormat = new DecimalFormat("00")

    val y = yearFormatter.format(year)
    val m = monthFormatter.format(month)
    val d = monthFormatter.format(day)

    val timeElements = time split ":"

    var newValue = ""

    var count = 1
    for (v <- timeElements) {
      if (v.length() < 2) {
        newValue = newValue + "0" + v
      } else {
        newValue = newValue + v
      }

      if (count != timeElements.length) {
        newValue = newValue + ":"
      }
      count = count + 1
    }
    val endDateString = y + "-" + m + "-" + d + "T" + newValue
    endDateString
  }

  def checkIfStreamHasEnded(all: List[(Option[BigDecimal], Option[BigDecimal], Option[BigDecimal], Option[String], Option[String], Option[String])], streamIdToCheck: String):
  (Option[BigDecimal], Option[BigDecimal], Option[BigDecimal], Option[String], Option[String]) = {

    all.foreach {
      case (year, month, day, time, streamId, streamName) =>
        if (streamIdToCheck.equalsIgnoreCase(streamId.getOrElse(""))) {
          return (year, month, day, time, streamId)
        }

    }
    (None, None, None, None, None)
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

  def listInvites(sEcho: Int, iDisplayLength: Int, iDisplayStart: Int, iSortCol_0: Int, sSortDir_0: String, streamId: String, token: String) = Action.async {
    implicit request =>

      request.session.get("whatAmIdoing-authenticationToken").map {
        tokenAuth =>

          val resInstance = ActorUtilsReader.findAllInvitesForStream(token, iDisplayStart, iDisplayLength, iSortCol_0, sSortDir_0, streamId)

          val res = resInstance.asInstanceOf[List[(Option[String], Option[String], String, Option[String], Option[String])]]

          var response: Seq[JsObject] = Seq()

          var totalDisplay = 0
          res.foreach {
            case (day, time, email, firstName, lastName) =>
              val json = Json.obj("0" -> day, "1" -> time, "2" -> email, "3" -> firstName, "4" -> lastName)
              response = response :+ json
              totalDisplay = totalDisplay + 1

          }


          val numberOfRecords = ActorUtilsReader.countAllInvitesForToken(token)
          val sendBack = Json.obj(
            "sEcho" -> sEcho,
            "iTotalRecords" -> numberOfRecords,
            "iTotalDisplayRecords" -> numberOfRecords,
            "aaData" -> response)

          Future {Ok(sendBack)}
      }.getOrElse {
        Future {Unauthorized(views.html.welcome(Index.userForm))}
      }

  }


  def list = Action.async {
    implicit request =>

      request.session.get("whatAmIdoing-authenticationToken").map {
        tokenAuth =>

          val sEcho = request.queryString.get("sEcho").get.head.toInt
          val numberOfItems = request.queryString.get("iDisplayLength").get.head.toInt
          val displayStart = request.queryString.get("iDisplayStart").get.head.toInt
          val sortColumn = request.queryString.get("iSortCol_0").get.head.toInt
          val sortDirection = request.queryString.get("sSortDir_0").get.head
          val token = request.queryString.get("token").get.head


          val resInstance = ActorUtilsReader.findAllStreamsForDay(token, displayStart, numberOfItems, sortColumn, sortDirection)

          val res = resInstance.asInstanceOf[List[(String, String, String, Option[String], Option[String])]]

          var response: Seq[JsObject] = Seq()

          var totalDisplay = 0
          res.foreach {
            case (stream, day, startTime, end, endTime) =>
              val json = Json.obj("stream" -> stream, "day" -> day, "startTime" -> startTime, "end" -> end, "endTime" -> endTime)
              response = response :+ json
              totalDisplay = totalDisplay + 1

          }


          val numberOfRecords = ActorUtilsReader.countNumberAllStreamsForDay(token)
          val sendBack = Json.obj(
            "sEcho" -> sEcho,
            "iTotalRecords" -> numberOfRecords,
            "iTotalDisplayRecords" -> numberOfRecords,
            "aaData" -> response)

          Future {Ok(sendBack)}
      }.getOrElse {
        Future {Unauthorized(views.html.welcome(Index.userForm))}
      }

  }

  def findAllStreams(email: String) = Action.async {
    implicit request =>

      request.session.get("whatAmIdoing-authenticationToken").map {
        user =>
          val valid = ActorUtilsReader.getValidToken(user)
          if (valid.asInstanceOf[List[String]].size > 0) {
            val toks = ActorUtilsReader.findAllTokensForUser(email).asInstanceOf[List[Option[String]]]

            var tokens: List[String] = List()
            for (x <- toks) {
              x match {
                case Some(tok) => tokens = tokens :+ tok.asInstanceOf[String]
                case None => tokens = tokens :+ "Nothing"
              }

            }
            Future {Ok(views.html.activity(tokens))}
          } else {
            Future {Unauthorized(views.html.welcome(Index.userForm))}
          }
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


              var token = ActorUtilsReader.getUserToken(userData.userName)
              Logger.info("active token associated with user[" + token + "]")

              if (token.isEmpty || token.equalsIgnoreCase("-1")) {
                token = java.util.UUID.randomUUID.toString
                ActorUtils.createTokenForUser(token, userData.userName)
              }

              Logger.info("active token associated with user[" + token + "]")
              Future {Redirect(routes.AdminController.getInvites).withSession(
                "whatAmIdoing-authenticationToken" -> token)}

            })

        })

  }

  private def authenticateUser(form: Form[User], userData: User): Either[Form[User], String] = {
    val res = ActorUtilsReader.searchForUser(userData.userName)
    val either = userData match {
      case User(username, password) if !res.isEmpty => validateUser(res, password)
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