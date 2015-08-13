package util.helper

import views.html.helper.FieldConstructor

/**
 * Created with IntelliJ IDEA.
 * User: valtechuk
 * Date: 27/10/2013
 * Time: 23:25
 * To change this template use File | Settings | File Templates.
 */
object MyHelper {

  implicit val fieldConstructor = FieldConstructor(views.html.helper.templates.userNameFieldConstructor.f)
}
