package controllers

import javax.inject.Inject

import play.api.data._
import play.api.data.Forms._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._

import scala.concurrent.future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.data.validation._
import play.api.data.validation.Valid

class Index @Inject()(val messagesApi: MessagesApi)  extends Controller with I18nSupport {

//object Index extends Controller {

  import Index._
/*
  import models.User
  val userForm = Form(
    mapping(
     "Email" -> email,
      "Password" -> nonEmptyText()
     )(User.apply)(User.unapply))

*/
  def index = Action.async { implicit request =>
    future(Ok(views.html.welcome(userForm)))
  }

  
}

object Index extends Controller {

  //import Index._

  import models.User

  val userForm = Form(
    mapping(
      "Email" -> email,
      "Password" -> nonEmptyText()
    )(User.apply)(User.unapply))

}