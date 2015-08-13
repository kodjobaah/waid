package integration.controller

import org.mockito.Matchers.any
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.scalatest.FlatSpec
import org.scalatest.Matchers
import org.scalatest.mock.MockitoSugar
import com.whatamidoing.mail.EmailSenderService
import play.api.test.FakeRequest
import play.api.test.Helpers
import play.api.test.Helpers.contentAsString
import play.api.test.Helpers.defaultAwaitTimeout
import play.api.test.Helpers.running
import play.api.test.TestServer
import com.whatamidoing.utils.ApplicationProps

//import org.java_websocket.WebSocket

//import com.ning.http.client.websocket.WebSocket

class WhatAmIdoingControllerSpec extends FlatSpec with MockitoSugar with SetupNeo4jActorsStub with Matchers {

  "when getting all invited " should "return list of all invited" in {

    running(TestServer(3333)) {
      currentTest = "findAllInvited"
      val fakeRequest = FakeRequest()
      val someResult = controllers.WhatAmIDoingController.findAllInvites(Option("token"))(fakeRequest)

      println("---------------------")
      println(someResult)
      contentAsString(someResult) should include("authenticationid")

    }
  }

  "when getting all invited if no token is provided" should "not return a list of invited" in {
    running(TestServer(3333)) {
      currentTest = "findAllInvitedNoToken"
      val fakeRequest = FakeRequest()
      val someResult = controllers.WhatAmIDoingController.findAllInvites(None)(fakeRequest)

      contentAsString(someResult) should include("No token provided")

    }
  }

  "when invited id is not supplied" should "send them to page with appropriate message" in {
    running(TestServer(3333)) {
      currentTest = "whatAmIdoingViewPageInvitedIdIsNotSupplied"
      val fakeRequest = FakeRequest()
      val someResult = controllers.WhatAmIDoingController.whatAmIdoing(None)(fakeRequest)

      contentAsString(someResult) should not include ("rtmp://www.whatamidoing.info:1935/oflaDemo/testPageToView.flv")
      contentAsString(someResult) should include("No Invitation Found")

    }
  }

  "when invited id does not exist" should "send them to page with appropriate message" in {
    running(TestServer(3333)) {
      currentTest = "whatAmIdoingViewPageInvitedIdDoesNotExist"
      val fakeRequest = FakeRequest()
      val someResult = controllers.WhatAmIDoingController.whatAmIdoing(Option("testInvitedId"))(fakeRequest)

      contentAsString(someResult) should not include ("rtmp://www.whatamidoing.info:1935/oflaDemo/testPageToView.flv")
      contentAsString(someResult) should include("No Invitation Found")

    }
  }
  "when given the invited id" should "get the page with the right url" in {
    running(TestServer(3333)) {
      currentTest = "whatAmIdoingViewPage"
      val fakeRequest = FakeRequest()
      val someResult = controllers.WhatAmIDoingController.whatAmIdoing(Option("testInvitedId"))(fakeRequest)

      contentAsString(someResult) should include("rtmp://www.whatamidoing.info:1935/oflaDemo/testPageToView.flv")
    }
  }

  /*

  "when a user tries to invite some one with an invalid email" should "not send the email" in {
    running(TestServer(3333)) {
      currentTest = "invitedToViewInvalidEmail"
      val fakeRequest = FakeRequest()
      val mockEmailService = mock[EmailSenderService]
      controllers.WhatAmIDoingController.emailSenderService = mockEmailService
      val testToken = "testToken"
      //val someResult = play.api.test.Helpers.await(controllers.WhatAmIDoingController.invite(email.get,testToken)(fakeRequest))
      val someResult = controllers.WhatAmIDoingController.invite(Option(testToken), Option("invalid email address"))(fakeRequest)

      import org.mockito.Mockito._
      import org.mockito.Matchers._

      verify(mockEmailService, never()).sendRegistrationEmail(org.mockito.Matchers.eq(email.get), any())
      verify(mockEmailService, never()).sendInviteEmail(org.mockito.Matchers.eq(email.get), any())

      contentAsString(someResult) should include("Invalid Email")

    }

  }
  */
  "when a user is invited to join and token is not valid" should "not be sent an email" in {
    running(TestServer(3333)) {
      currentTest = "invitedToViewTokenNotValid"
      val fakeRequest = FakeRequest()
      val mockEmailService = mock[EmailSenderService]
      controllers.WhatAmIDoingController.emailSenderService = mockEmailService
      val testToken = "testToken"
      //val someResult = play.api.test.Helpers.await(controllers.WhatAmIDoingController.invite(email.get,testToken)(fakeRequest))
      val someResult = controllers.WhatAmIDoingController.invite(Option(testToken), email)(fakeRequest)

      import org.mockito.Mockito._
      import org.mockito.Matchers._

      verify(mockEmailService, never()).sendRegistrationEmail(org.mockito.Matchers.eq(email.get), any())
      verify(mockEmailService, never()).sendInviteEmail(org.mockito.Matchers.eq(email.get), any())

      contentAsString(someResult) should include("Unable To Invite")

    }
  }
  "when a user is invited to join and stream does not exist" should "not be sent an email" in {
    running(TestServer(3333)) {
      currentTest = "invitedToViewStreamButDoesNotExist"
      val fakeRequest = FakeRequest()
      val mockEmailService = mock[EmailSenderService]
      controllers.WhatAmIDoingController.emailSenderService = mockEmailService
      val testToken = "testToken"
      //val someResult = play.api.test.Helpers.await(controllers.WhatAmIDoingController.invite(email.get,testToken)(fakeRequest))
      val someResult = controllers.WhatAmIDoingController.invite(Option(testToken), email)(fakeRequest)

      import org.mockito.Mockito._
      import org.mockito.Matchers._

      verify(mockEmailService, never()).sendRegistrationEmail(org.mockito.Matchers.eq(email.get), any())
      verify(mockEmailService, never()).sendInviteEmail(org.mockito.Matchers.eq(email.get), any())

      contentAsString(someResult) should include("Unable to Invite No Stream")

    }

  }
  "when a user is invited to join and is not regitered" should "registered user and create invited" in {
    running(TestServer(3333)) {
      val fakeRequest = FakeRequest()
      currentTest = "invitedToJoinAndRegistered"
      val mockEmailService = mock[EmailSenderService]
      controllers.WhatAmIDoingController.emailSenderService = mockEmailService
      val testToken = "testToken"
      //val someResult = play.api.test.Helpers.await(controllers.WhatAmIDoingController.invite(email.get,testToken)(fakeRequest))
      val someResult = controllers.WhatAmIDoingController.invite(Option(testToken), email)(fakeRequest)

      import org.mockito.Mockito._
      import org.mockito.Matchers._

      verify(mockEmailService).sendRegistrationEmail(org.mockito.Matchers.eq(email.get), any())
      verify(mockEmailService).sendInviteEmail(org.mockito.Matchers.eq(email.get), any())

      contentAsString(someResult) should include("Done")
      println(someResult)
    }
  }

  "when a user is invited to join and but not email is supplied" should "not be invited" in {
    running(TestServer(3333)) {
      val fakeRequest = FakeRequest()
      currentTest = "invitedToJoinButNoEmailProvided"
      val mockEmailService = mock[EmailSenderService]
      controllers.WhatAmIDoingController.emailSenderService = mockEmailService
      val testToken = "testToken"
      //val someResult = play.api.test.Helpers.await(controllers.WhatAmIDoingController.invite(email.get,testToken)(fakeRequest))
      val someResult = controllers.WhatAmIDoingController.invite(Option(testToken), None)(fakeRequest)

      import org.mockito.Mockito._
      import org.mockito.Matchers._

      verify(mockEmailService, never()).sendRegistrationEmail(org.mockito.Matchers.eq(email.get), any())
      verify(mockEmailService, never()).sendInviteEmail(org.mockito.Matchers.eq(email.get), any())

      contentAsString(someResult) should include("No email provided")

    }
  }

  "when a user is invited to join and but no token is supplied" should "not be invited" in {
    running(TestServer(3333)) {
      val fakeRequest = FakeRequest()
      currentTest = "invitedToJoinButNoTokenProvided"
      val mockEmailService = mock[EmailSenderService]
      controllers.WhatAmIDoingController.emailSenderService = mockEmailService
      val testToken = "testToken"
      //val someResult = play.api.test.Helpers.await(controllers.WhatAmIDoingController.invite(email.get,testToken)(fakeRequest))
      val someResult = controllers.WhatAmIDoingController.invite(None, email)(fakeRequest)

      import org.mockito.Mockito._
      import org.mockito.Matchers._

      verify(mockEmailService, never()).sendRegistrationEmail(org.mockito.Matchers.eq(email.get), any())
      verify(mockEmailService, never()).sendInviteEmail(org.mockito.Matchers.eq(email.get), any())

      contentAsString(someResult) should include("No token provided")

    }
  }

  "when no email is supplied user" should "not be allowed to regsiter" in {
    running(TestServer(3333)) {
      val fakeRequest = FakeRequest()
      currentTest = "registeredLoginWithOutSupplyingEmail"
      val someResult = controllers.WhatAmIDoingController.registerLogin(None, password, firstName, lastName)(fakeRequest)

      contentAsString(someResult) should include("Email not supplied")

      val cookies = Helpers.cookies(someResult)
      val play_session = cookies.get("PLAY_SESSION") match {
        case Some(cookie) => cookie.value
        case None => ""
      }

      play_session should not include ("whatAmIdoing-authenticationToken")

    }

  }

  "when a user tries to registerd without supplying a password" should "not be allowed to register" in {
    running(TestServer(3333)) {
      val fakeRequest = FakeRequest()
      currentTest = "registeredLoginWithOutSupplyingPassword"
      val someResult = controllers.WhatAmIDoingController.registerLogin(email, None, firstName, lastName)(fakeRequest)

      contentAsString(someResult) should include("Password not supplied")

      val cookies = Helpers.cookies(someResult)
      val play_session = cookies.get("PLAY_SESSION") match {
        case Some(cookie) => cookie.value
        case None => ""
      }

      play_session should not include ("whatAmIdoing-authenticationToken")

    }
  }

  /*
  "when a user tries to registerd with an invalid email" should "not be allowed to register" in {
    running(TestServer(3333)) {
      val fakeRequest = FakeRequest()
      currentTest = "registeredLoginWithValidPasswordInvalidToken"
      val someResult = controllers.WhatAmIDoingController.registerLogin(Option("invalid email address"), password, firstName, lastName)(fakeRequest)

      contentAsString(someResult) should include("Email Not Valid")

      val cookies = Helpers.cookies(someResult)
      val play_session = cookies.get("PLAY_SESSION") match {
        case Some(cookie) => cookie.value
        case None => ""
      }

      play_session should not include ("whatAmIdoing-authenticationToken")

    }
  }
  */
  "when a user is registered" should "not return authentication token if passoword is valid but token is invalid" in {
    running(TestServer(3333)) {
      val fakeRequest = FakeRequest()
      currentTest = "registeredLoginWithValidPasswordInvalidToken"
      val someResult = controllers.WhatAmIDoingController.registerLogin(email, password, firstName, lastName)(fakeRequest)

      val cookies = Helpers.cookies(someResult)
      val play_session = cookies.get("PLAY_SESSION") match {
        case Some(cookie) => cookie.value
        case None => ""
      }

      play_session should include("whatAmIdoing-authenticationToken")

    }

  }
  "when a user is registered" should "not return authentication token if password is not valid" in {
    running(TestServer(3333)) {
      currentTest = "registeredLoginWithInvalidPassword"
      val fakeRequest = FakeRequest()
      val someResult = controllers.WhatAmIDoingController.registerLogin(email, password, firstName, lastName)(fakeRequest)

      val cookies = Helpers.cookies(someResult)
      val play_session = cookies.get("PLAY_SESSION") match {
        case Some(cookie) => cookie.value
        case None => ""
      }

      play_session should be(empty)
      play_session should not include ("whatAmIdoing-authenticationToken")

    }

  }

  "when a user is registered" should "return authentication token if password valid and token is valid" in {
    running(TestServer(3333)) {
      currentTest = "registeredWithValidPasswordAndValidToken"
      val fakeRequest = FakeRequest()
      val someResult = controllers.WhatAmIDoingController.registerLogin(email, password, firstName, lastName)(fakeRequest)

      val cookies = Helpers.cookies(someResult)
      val play_session = cookies.get("PLAY_SESSION").get.value

      play_session should not be empty
      play_session should include("whatAmIdoing-authenticationToken")

    }
  }

  "when a user is not registered" should
    "register the user and add authentication token to the session" in {
      running(TestServer(3333)) {
        currentTest = "registerLoginNotRegistered"

        val fakeRequest = FakeRequest()
        val someResult = controllers.WhatAmIDoingController.registerLogin(email, password, firstName, lastName)(fakeRequest)

        val cookies = Helpers.cookies(someResult)
        val play_session = cookies.get("PLAY_SESSION").get.value

        play_session should not be empty
        play_session should include("whatAmIdoing-authenticationToken")

      }

    }

  "when a user is not registered" should
    "register the user and return should not store no authenticaion token if the token was not found" in {
      running(TestServer(3333)) {
        currentTest = "registerLoginNotRegisteredButInvalidToken"

        val fakeRequest = FakeRequest()
        val someResult = controllers.WhatAmIDoingController.registerLogin(email, password, firstName, lastName)(fakeRequest)

        val cookies = Helpers.cookies(someResult)
        val play_session: String = cookies.get("PLAY_SESSION") match {
          case Some(cookie) => cookie.value
          case None => ""
        }

        play_session should be(empty)
      }

    }

  "when user tries to logout without providind a token" should "invalidate the session" in {
    running(TestServer(3333)) {
      currentTest = "invalidteTheSessionWhenNoToken"

      var fakeRequest = FakeRequest().withSession("whatAmIdoing-authenticationToken" -> "token")
      val result = controllers.WhatAmIDoingController.invalidateToken(None)(fakeRequest)

      val cookies = Helpers.cookies(result)
      val play_session: String = cookies.get("PLAY_SESSION") match {
        case Some(cookie) => cookie.value
        case None => ""
      }

      play_session should be(empty)

      contentAsString(result) should include("No token provided")

    }
  }

  "when a user logs out" should "invalidte the session" in {
    running(TestServer(3333)) {
      currentTest = "invalidteTheSessionWhenNoActiveStream"

      var fakeRequest = FakeRequest().withSession("whatAmIdoing-authenticationToken" -> "token")
      val result = controllers.WhatAmIDoingController.invalidateToken(Option("token"))(fakeRequest)

      val cookies = Helpers.cookies(result)
      val play_session: String = cookies.get("PLAY_SESSION") match {
        case Some(cookie) => cookie.value
        case None => ""
      }

      play_session should be(empty)
    }
  }

  "when user token is not valid" should "not be allowed to encoded video" in {

    running(TestServer(3333)) {

      var fakeRequest = FakeRequest()
      //  implicit val timeout = Timeout(1 seconds)
      // var som = play.api.test.Helpers.await(controllers.WhatAmIDoingController.registerLogin(email, password, firstName, lastName)(fakeRequest))
      // println(som)
      /* var result = controllers.WhatAmIDoingController.registerLogin(email, password, firstName, lastName)(fakeRequest)

      println(result)
      val cookies = Helpers.cookies(result)
      println(cookies.get("PLAY_SESSION").get.value)
     
      val value = cookies.get("PLAY_SESSION").get.value
      
      var c: AsyncHttpClient = new AsyncHttpClient()
      val r: Request = c.prepareGet("ws://localhost:9000/publishVideo?token=don").build()
      var h: WebSocketUpgradeHandler = new WebSocketUpgradeHandler.Builder().
        addWebSocketListener(
          new WebSocketTextListener() {
            @Override def onMessage(message: String) {}
            @Override def onOpen(websocket: WebSocket) {}
            @Override def onClose(websocket: WebSocket) {}
            @Override def onError(t: Throwable) {}
            @Override def onFragment(fragment: String, last: Boolean) {}
          }).build();
      var websocket: WebSocket = c.executeRequest(r, h).get()
      websocket.sendTextMessage("Beer")
      println("----ended")

       
      val json: JsValue = Json.parse("""
        		{ 
        		"response": {
        		"value" : "TOKEN NOT VALID"
        		}
        		} 
        	""")
     
       import integration.util.WebSocketClient.Messages._
       integration.util.WebSocketClient(new URI("ws://localhost:3333/publishVideo?token=don")) {
     	case Connected(client) => println("Connection has been established to: " + client.url.toASCIIString)
      case Disconnected(client, _) => println("The websocket to " + client.url.toASCIIString + " disconnected.")
      case TextMessage(client, message) => {
        println("RECV: " + message)
        client send ("ECHO: " + message)
      } 
      * 
     
    }
    *      
    */

    }

  }
}