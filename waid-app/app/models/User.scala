package models

case class User(val userName : String = "", val password: String = "")
case class UserDetails(val email: Option[String] = Option(""), val firstName: String="", val lastName: String = "")
case class Location(val latitude: Double = 0.0 , val longitude: Double = 0.0)
case class UserInformation(val email: String ="",val firstName: String = "" , val lastName: String = "",  val domId: Option[String]=Option(""))