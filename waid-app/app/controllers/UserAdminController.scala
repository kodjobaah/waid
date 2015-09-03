package controllers

import javax.inject.Inject

import com.waid.redis.service.RedisUserService
import com.waid.redis.{RedisDataStore, KeyPrefixGenerator}
import com.whatamidoing.mail.EmailSenderService
import com.waid.redis.service.RedisUserService
import com.whatamidoing.utils.{ActorUtils, ActorUtilsReader}
import models.{UserDetails, ForgottenPassword, PasswordChange, ChangePassword}
import com.waid.redis.model.{UserStreamNode, UserNode}
import play.api.Logger
import play.api.data.{Form, FormError}
import play.api.data.Forms._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{AnyContent, Action, Controller}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.Future

/**
 * Created by kodjobaah on 21/07/2015.
 */
class UserAdminController @Inject()(val messagesApi: MessagesApi)  extends Controller with I18nSupport {

  val userDetailsForm = Form(
    mapping(
      "email" -> optional(text),
      "firstName" -> nonEmptyText(),
      "lastName" -> nonEmptyText()
    )(UserDetails.apply)(UserDetails.unapply))

  val forgottenPasswordForm = Form(
    mapping(
      "email" -> nonEmptyText()
    )(ForgottenPassword.apply)(ForgottenPassword.unapply))

  val changePasswordForm = Form(
    mapping(
      "password" -> nonEmptyText,
      "confirmPassword" -> nonEmptyText,
      "changePasswordId" -> nonEmptyText,
      "email" -> nonEmptyText
    )(PasswordChange.apply)(PasswordChange.unapply))

  val logger:Logger = Logger("controllers.UserAdminController")

  var emailSenderService = EmailSenderService()

  def registerLoginRedis(email: Option[String], password: Option[String], firstName: Option[String], lastName: Option[String]) =
    Action.async {
      implicit request =>


        val em = email.getOrElse("no-email-address-provided")
        val p = password.getOrElse("no-password-provided")
        val fn = firstName.getOrElse("no-first-name-provided")
        val ln = lastName.getOrElse("no-last-name-provided")

        if (!em.equalsIgnoreCase("no-email-address-provided")) {

          val res = RedisUserService.findUser(em)

          Logger.info("results from searching for a user:" + p + ":")
          //Creating the user
          if (res == None) {

            val userNode = RedisUserService.createNewUser(em,fn,ln,p)
            val regToken = RedisUserService.getAttributeFromNode(userNode, KeyPrefixGenerator.RegistrationToken)
            val emailSender = EmailSenderService()

            try {
              emailSender.verifyAccount(em, regToken.get)
            } catch {
              case e: Exception =>  {
                logger.error("problems sending email for user["+userNode+"]")
                e.printStackTrace()
              }
            }
            Future { Ok("USER CREATED - ADDED AUTHENTICATION TOKEN TO SESSISON")}

          } else {

            if (!p.equalsIgnoreCase("no-password-provided")) {

              val regToken = RedisUserService.getAttributeFromNode(res.get, KeyPrefixGenerator.RegistrationToken)

              val userNode = RedisUserService.getUserFromRegistration(regToken)
              //Check to make user has completed the registration process
              if (userNode == None) {
                //Checking the users password
                import org.mindrot.jbcrypt.BCrypt
                var decrypt = true

                val hashPassword = RedisUserService.getAttributeFromNode(res.get,KeyPrefixGenerator.Password)

                try {
                  decrypt = BCrypt.checkpw(p, hashPassword.get)
                }
                catch {
                  case e: java.lang.IllegalArgumentException => decrypt = false
                }

                if (decrypt) {

                  RedisUserService.removeCurrentToken(em)
                  RedisUserService.removePreviousValidLoginDetails(em)
                  RedisUserService.removeValidStreamUsingEmail(em)
                  val userTokenNode = RedisUserService.createToken(res)
                  val token = RedisUserService.getAttributeFromNode(userTokenNode, KeyPrefixGenerator.Token)
                  Logger.info("---Token Created:" + token)
                  Future.successful(Ok("ADDED AUTHENTICATION TOKEN TO SESSISON").withSession(
                    "whatAmIdoing-authenticationToken" -> token.get))

                } else {
                  Future.successful(Ok("PASSWORD NOT VALID"))
                }

              }else {
                Future.successful(Ok("Needs to complete Registration"))
              }


            } else {
              Future.successful(Ok("Password not supplied"))
            }

          }

        } else {
          Future.successful(Ok("Email not supplied"))
        }
    }


  def resendPassword(email: String) =
    Action.async {
      implicit request =>

        val res = RedisUserService.findUser(email)

        if (res != None) {

          val regToken = RedisUserService.getAttributeFromNode(res.get, KeyPrefixGenerator.RegistrationToken)

          val userNode = RedisUserService.getUserFromRegistration(regToken)

          if (userNode != None) {
            Future.successful(Ok("Account needs to be verified"))
          } else {
            val token = RedisUserService.setForgottonPassword(email)
            emailSenderService.sendChangePasswordLink(email, token)
            Future.successful(Ok("Email Sent"))
          }

        }  else {
          Future.successful(ServiceUnavailable("Email Does not exist"))
        }
    }


  def completeRegistration(registrationId: Option[String]) =
    Action.async {

      val userNode = RedisUserService.getUserFromRegistration(registrationId)

      if (userNode != None) {
        RedisUserService.removeRegistrationDetails(registrationId)
        Future {
          Ok(views.html.registrationComplete())
        }
      } else {
        Future {
          Unauthorized(views.html.registrationComplete())
        }
      }
    }

  def passwordChange(changePasswordId:String,email:String) =
    Action.async {
      implicit request =>


        val res = RedisUserService.checkForgottonPassword(email,changePasswordId)

        if (res != None) {
          Future {Ok(views.html.passwordChange(changePasswordForm, changePasswordId,email))}
        } else {
          Future {Ok(views.html.invalidchangepasswordid())}
        }
    }


  def performPasswordChange = Action.async {
    implicit request =>

      val body: AnyContent = request.body
      logger.info("--:" + body.asFormUrlEncoded.get)
      var found = false
      var changePasswordId = ""
      var email = ""
      for ((k, v) <- body.asFormUrlEncoded.get) {
        if (k == "changePasswordId") {
          if (v.size > 0) {
            changePasswordId = v.head
            found = true
          } else {
            found = false
          }
        }

        if (k == "email") {
          if (v.size > 0) {
              email = v.head
              found = true
          } else {
              found = false
          }
        }
      }


      if (!found) {
        Future {Ok(views.html.welcome(Index.userForm))}
      } else {

        val res = RedisUserService.checkForgottonPassword(email,changePasswordId)
        if (res == None) {
          Future {Ok(views.html.invalidchangepasswordid())}
        } else {
          val bindForm = changePasswordForm.bindFromRequest

          bindForm.fold(
            formWithErrors => {
              // binding failure, retrieving the form containing errors
              Future {BadRequest(views.html.passwordChange(formWithErrors, changePasswordId,email))}
            },
            userData => {
              changePasswordForUser(bindForm, userData,res.get).fold(
                formWithErrors => Future {BadRequest(views.html.passwordChange(formWithErrors, changePasswordId,email))},
                msg => {
                  Future {Ok(views.html.passwordchangeconfirmation())}
                })
            })
        }
      }
  }


  private def changePasswordForUser(form: Form[PasswordChange], userData: PasswordChange, res: UserNode): Either[Form[PasswordChange], String] = {
    val either = userData match {
      case PasswordChange(password, confirmPassword, changePasswordId,email) if password == confirmPassword => updateUserPassword(password,res)
      case _ => Left(Seq(FormError("password", "error.passwordnotmatch")))
    }

    either.fold(
      error => {
        logger.info("passwordm athcet erro")
        val formWithErrors = Form(form.mapping, data = form.data,
          errors = error, value = form.value)
        Left(formWithErrors)
      },
      msg => Right("")
    )

  }

  private def updateUserPassword(password: String, user: UserNode): Either[Seq[FormError], String] = {
    logger.info("updating passowrd")
    import org.mindrot.jbcrypt.BCrypt
    val pw_hash = BCrypt.hashpw(password, BCrypt.gensalt())

    var atts = user.attributes.get
    atts += KeyPrefixGenerator.Password -> pw_hash
    user.attributes = Option(atts)
    RedisDataStore.addUser(user)
    RedisUserService.removeForgottenPassword(atts.get(KeyPrefixGenerator.Email))
    Right("")
  }


  def changePasswordRequest = Action.async {
    implicit request =>

      val bindForm = forgottenPasswordForm.bindFromRequest

      bindForm.fold(
        formWithErrors => {
          // binding failure, retrieving the form containing errors
          Future {BadRequest(views.html.forgottenPassword(formWithErrors))}

        },
        userData => {
          findUserForForgottenPassword(bindForm, userData).fold(
            formWithErrors => Future {BadRequest(views.html.forgottenPassword(formWithErrors))},
            msg => {
              Future {Ok(views.html.passwordsent())}
            })
        })
  }

  private def findUserForForgottenPassword(form: Form[ForgottenPassword], userData: ForgottenPassword): Either[Form[ForgottenPassword], String] = {


    val res = RedisUserService.findUser(userData.email)
    var userNode:Option[UserNode] = None
    for(r <- res) {
      val regToken = RedisUserService.getAttributeFromNode(res.get, KeyPrefixGenerator.RegistrationToken)
      userNode = RedisUserService.getUserFromRegistration(regToken)

    }
    val either = userData match {
      case ForgottenPassword(email) if (res != None) && (userNode == None) => sendForgottenPasswordEmail(email)
      case ForgottenPassword(email) if (res != None) && (userNode != None) => Left(Seq(FormError("email", "error.validate")))
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

    val token = RedisUserService.setForgottonPassword(email)
    emailSenderService.sendChangePasswordLink(email, token)
    Right("")
  }


  def fetchUserDetails = Action.async {
    implicit request =>

      request.session.get("whatAmIdoing-authenticationToken").map {
        token =>
          val res = RedisUserService.checkIfTokenIsValid(token)

          if (res != None) {
            val userDetails = populateUserDetails(res.get)
            val filledForm = userDetailsForm.fill(userDetails)
            Future {
              Ok(views.html.userdetails(filledForm, state = false))
            }
          } else {
            Future {Unauthorized(views.html.welcome(Index.userForm))}
          }
      }.getOrElse {
        Future {Unauthorized(views.html.welcome(Index.userForm))}
      }

  }

  private def populateUserDetails(userNode: UserNode ): UserDetails =  {
    val email = userNode.attributes get KeyPrefixGenerator.Email
    val firstName = userNode.attributes get KeyPrefixGenerator.FirstName
    val lastName = userNode.attributes get KeyPrefixGenerator.LastName
    val userDetails = UserDetails(Option(email),firstName,lastName)
    userDetails
  }
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

              var user = RedisUserService.checkIfTokenIsValid(token)

              if(user != None) {
                val firstName = Map(KeyPrefixGenerator.FirstName -> userData.firstName)
                val lastName = Map(KeyPrefixGenerator.LastName -> userData.lastName)
                RedisDataStore.addEleemnt(user.get.genId,firstName)
                RedisDataStore.addEleemnt(user.get.genId,lastName)

                user = RedisUserService.checkIfTokenIsValid(token)
                val userDetails = populateUserDetails(user.get)
                val filledForm = userDetailsForm.fill(userDetails)
                Future {
                  Ok(views.html.userdetails(filledForm, state = true))
                }
              } else {
                Future {Unauthorized(views.html.welcome(Index.userForm))}
              }
              }
          )
      }.getOrElse {
        Future {Unauthorized(views.html.welcome(Index.userForm))}
      }


  }

  def forgottenPassword = Action.async {
    implicit request =>
      Future {Ok(views.html.forgottenPassword(forgottenPasswordForm))}

  }


}
