import java.util.UUID
import scala.util.Random

/**
 * Created with IntelliJ IDEA.
 * User: kodjobaah
 * Date: 31/10/2013
 * Time: 10:23
 * To change this template use File | Settings | File Templates.
 */
object TestData2012 extends App {


  import com.whatamidoing.cypher.CypherWriterFunction._
  import com.whatamidoing.cypher.CypherReaderFunction._

  val email = "kodjo_baah@hotmail.com"
  val firstName = "first-name"
  val lastname = "last-name"
  val password = "password"


  val emailInvite = "kodjo_invite2012@hotmail.com"
  val firstNameInvite = "first-name"
  val lastnameInvite = "last-name"
  val passwordInvite = "password"
  val inviteId = "inviteId"


  val emailInvite1 = "kodjo_invite20121@hotmail.com"
  val inviteId1 = "inviteId1"

  val emailInvite2 = "kodjo_invite20122@hotmail.com"
  val inviteId2 = "inviteId2"

  val emailInvite3 = "kodjo_invite20123@hotmail.com"
  val inviteId3 = "inviteId3"

  val emailInvite4 = "kodjo_invite20124@hotmail.com"
  val inviteId4 = "inviteId4"


  /*
  val invalid = invalidateToken("236096a5-1395-4ffc-a57a-77101f526360")

  val step = invalid()
  println(step)*/
 // val res = createUser(firstName, lastname, email, password)
 // val result = res()
  val token = getUserToken(email)
  val tokenResult = token()

  val userToken = tokenResult.results.head.asInstanceOf[(String, String)]._1

  val stream = "stream-id-2012-0"

  val userStream1 = createTestStream2012(stream, userToken)
  val userStreamResult = userStream1()
  println(userStreamResult)

  for (x <- 1 until 200) {
    val userStream1 = createTestStream2012("stream-id-2012-" + x, userToken)
    val userStreamResult = userStream1()
    println(userStreamResult.results)

  }


  var r = createUser(firstName, lastname, emailInvite, password)
  var s = r()
  r = createInvite(stream, emailInvite, inviteId)
  s = r()
  r = associateDayWithInvite(inviteId)
  s = r()


  r = createUser(firstName, lastname, emailInvite1, password)
  s = r()
  r = createInvite(stream, emailInvite1, inviteId1)
  s = r()
  r = associateDayWithInvite(inviteId1)
  s = r()


  r = createUser(firstName, lastname, emailInvite2, password)
  s = r()
  r = createInvite(stream, emailInvite2, inviteId2)
  s = r()
  r = associateDayWithInvite(inviteId2)
  s = r()


  r = createUser(firstName, lastname, emailInvite3, password)
  s = r()
  r = createInvite(stream, emailInvite3, inviteId3)
  s = r()
  r = associateDayWithInvite(inviteId3)
  s = r()


  r = createUser(firstName, lastname, emailInvite4, password)
  s = r()
  r = createInvite(stream, emailInvite4, inviteId4)
  s = r()


  r = closeStream(stream)
  s = r()
  println("ended")



 val hmm = createTokenForUser(userToken, email)
  val j = hmm()

  for (i <- 0 until 3) {
    val userToken = java.util.UUID.randomUUID().toString()
    val res = createTokenForUser(userToken, email)
    val j = res()

    val stream = "stream-id-2012-other-"+i


    val userStream1 = createTestStream2012(stream, userToken)
    val userStreamResult = userStream1()
    println(userStreamResult)

    for (x <- 1 until 4) {
      val userStream1 = createTestStream2012("stream-id-2012-other"+i+"-"+ x, userToken)
      val userStreamResult = userStream1()
      println(userStreamResult.results)

    }


    var r = createUser(firstName, lastname, emailInvite, password)
    var k = r()
    r = createInvite(stream, emailInvite, inviteId)
    var s = r()
    r = associateDayWithInvite(inviteId)
    s = r()


    r = createUser(firstName, lastname, emailInvite1, password)
    s = r()
    r = createInvite(stream, emailInvite1, inviteId1)
    s = r()
    r = associateDayWithInvite(inviteId1)
    s = r()


    r = createUser(firstName, lastname, emailInvite2, password)
    s = r()
    r = createInvite(stream, emailInvite2, inviteId2)
    s = r()
    r = associateDayWithInvite(inviteId2)
    s = r()


    r = createUser(firstName, lastname, emailInvite3, password)
    s = r()
    r = createInvite(stream, emailInvite3, inviteId3)
    s = r()
    r = associateDayWithInvite(inviteId3)
    s = r()


    r = createUser(firstName, lastname, emailInvite4, password)
    s = r()
    r = createInvite(stream, emailInvite4, inviteId4)
    s = r()

    val rus = invalidateToken(userToken)
    val results = rus()
  }

  println("done")

}
