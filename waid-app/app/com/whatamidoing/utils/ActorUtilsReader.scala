package com.whatamidoing.utils

import akka.actor.ActorSystem
import com.whatamidoing.cypher.CypherReaderFunction
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.Future
import play.api.Logger

object ActorUtilsReader {

  val system = ActorSystem("whatamidoing-system")
  implicit val timeout = Timeout(500 seconds)
  var neo4jreader = ActorUtils.neo4jreader

  import models.Messages._

  def getUserToken(em: String) = {
    val getUserToken = CypherReaderFunction.getUserToken(em)
    //Used by ?(ask)

    val getUserTokenResponse: Future[Any] = ask(neo4jreader, PerformReadOperation(getUserToken)).mapTo[Any]
    val res = Await.result(getUserTokenResponse, 10 seconds) match {
      case ReadOperationResult(results) =>
        if (results.results.size > 0) {
          val tok = results.results.head.asInstanceOf[(String, String)]
          if (tok._2.equalsIgnoreCase("true")) {
            tok._1
          } else {
            "-1"
          }
        } else {
          "-1"
        }

    }
    res
  }


  def searchForUser(em: String) = {

    val searchForUser = CypherReaderFunction.searchForUser(em)
    val response: Future[Any] = ask(neo4jreader, PerformReadOperation(searchForUser)).mapTo[Any]

    val res = Await.result(response, 10 seconds) match {
      case ReadOperationResult(readResults) =>
        readResults.results.mkString
    }

    res
  }

  def streamNameForToken(token: String) = {
    val findStreamForToken = CypherReaderFunction.findActiveStreamForToken(token)
    val findStreamForTokenResponse: Future[Any] = ask(neo4jreader, PerformReadOperation(findStreamForToken)).mapTo[Any]
    val streamName = Await.result(findStreamForTokenResponse, 10 seconds) match {
      case ReadOperationResult(readResults) =>
        if (readResults.results.size > 0) {
          readResults.results.head.asInstanceOf[String]
        } else {
          ""
        }
    }

    streamName
  }

  def getValidToken(token: String) = {
    val getValidToken = CypherReaderFunction.getValidToken(token)
    val getValidTokenResponse: Future[Any] = ask(neo4jreader, PerformReadOperation(getValidToken)).mapTo[Any]
    val res = Await.result(getValidTokenResponse, 10 seconds) match {
      case ReadOperationResult(readResults) =>
        readResults.results

    }
    res
  }


  def findStreamForInvitedId(invitedId: String) = {
    val createInvite = CypherReaderFunction.findStreamForInvitedId(invitedId)
    val readerResponse: Future[Any] = ask(neo4jreader, PerformReadOperation(createInvite)).mapTo[Any]
    val streamName = Await.result(readerResponse, 10 seconds) match {
      case ReadOperationResult(results) =>
        if (results.results.size > 0) {
          results.results.head.asInstanceOf[String]
        } else {
          ""
        }
    }
    streamName

  }


  def findStreamForInviteTwitter(invitedId: String) = {
    val inviteTwitter = CypherReaderFunction.findStreamForInviteTwitter(invitedId)
    val readerResponse: Future[Any] = ask(neo4jreader, PerformReadOperation(inviteTwitter)).mapTo[Any]
    val streamName = Await.result(readerResponse, 10 seconds) match {
      case ReadOperationResult(results) =>
        if (results.results.size > 0) {
          results.results.head.asInstanceOf[String]
        } else {
          ""
        }
    }
    streamName

  }

  def findStreamForInviteFacebook(invitedId: String) = {
    val inviteFacebook = CypherReaderFunction.findStreamForInviteFacebook(invitedId)
    val readerResponse: Future[Any] = ask(neo4jreader, PerformReadOperation(inviteFacebook)).mapTo[Any]

    val streamName = Await.result(readerResponse, 10 seconds) match {
      case ReadOperationResult(results) =>
        if (results.results.size > 0) {
          results.results.head.asInstanceOf[String]
        } else {
          ""
        }
    }
    streamName

  }


  def findStreamForInviteLinkedin(invitedId: String) = {
    val inviteLinkedin = CypherReaderFunction.findStreamForInviteLinkedin(invitedId)
    val readerResponse: Future[Any] = ask(neo4jreader, PerformReadOperation(inviteLinkedin)).mapTo[Any]

    val streamName = Await.result(readerResponse, 10 seconds) match {
      case ReadOperationResult(results) =>
        if (results.results.size > 0) {
          results.results.head.asInstanceOf[String]
        } else {
          ""
        }

    }
    streamName

  }

  /*
   * 
   */
  def checkToSeeIfTwitterInviteAcceptedAlreadyByReferer(inviteId: String, referer: String) = {
    val checkToSeeIfTwitterInviteAcceptedAlreadyByReferer = CypherReaderFunction.checkToSeeIfTwitterInviteAcceptedAlreadyByReferer(inviteId, referer)
    val readerResponse: Future[Any] = ask(neo4jreader, PerformReadOperation(checkToSeeIfTwitterInviteAcceptedAlreadyByReferer)).mapTo[Any]

    val id = Await.result(readerResponse, 20 seconds) match {
      case ReadOperationResult(results) =>

        if (results.results.size > 0) {
          results.results.head.asInstanceOf[String]
        } else {
          ""
        }
    }
    id

  }

  /*
   * 
   */
  def checkToSeeIfFacebookInviteAcceptedAlreadyByReferer(inviteId: String, referer: String) = {
    val checkToSeeIfFacebookInviteAcceptedAlreadyByReferer = CypherReaderFunction.checkToSeeIfFacebookInviteAcceptedAlreadyByReferer(inviteId, referer)
    val readerResponse: Future[Any] = ask(neo4jreader, PerformReadOperation(checkToSeeIfFacebookInviteAcceptedAlreadyByReferer)).mapTo[Any]

    val id = Await.result(readerResponse, 20 seconds) match {
      case ReadOperationResult(results) =>
        if (results.results.size > 0) {
          results.results.head.asInstanceOf[String]
        } else {
          ""
        }
    }
    id

  }

  /*
   * 
   */
  def checkToSeeIfLinkedinInviteAcceptedAlreadyByReferer(inviteId: String, referer: String) = {
    val checkToSeeIfLinkedinInviteAcceptedAlreadyByReferer = CypherReaderFunction.checkToSeeIfLinkedinInviteAcceptedAlreadyByReferer(inviteId, referer)
    val readerResponse: Future[Any] = ask(neo4jreader, PerformReadOperation(checkToSeeIfLinkedinInviteAcceptedAlreadyByReferer)).mapTo[Any]
    val id = Await.result(readerResponse, 20 seconds) match {
      case ReadOperationResult(results) =>
        if (results.results.size > 0) {
          results.results.head.asInstanceOf[String]
        } else {
          ""
        }
    }
    id

  }


  def findActiveStreamForToken(token: String): String = {

    val findStreamForToken = CypherReaderFunction.findActiveStreamForToken(token)
    val getValidTokenResponse: Future[Any] = ask(neo4jreader, PerformReadOperation(findStreamForToken)).mapTo[Any]
    val streamName = Await.result(getValidTokenResponse, 10 seconds) match {
      case ReadOperationResult(readResults) =>
        if (readResults.results.size > 0) {
          readResults.results.head.asInstanceOf[String]
        } else {
          ""
        }
    }
    streamName
  }


  def findAllInvites(email: String): List[String] = {

    val findAllInvites = CypherReaderFunction.findAllInvites(email)
    val findAllInvitesResponse: Future[Any] = ask(neo4jreader, PerformReadOperation(findAllInvites)).mapTo[Any]
    val results = Await.result(findAllInvitesResponse, 10 seconds) match {
      case ReadOperationResult(readResults) =>
        readResults.results.asInstanceOf[List[String]]
    }
    results
  }

  def findAllStreamsForDay(token: String, displayStart: Int, displayLength: Int, sortColumn: Int, sortDirection: String): List[String] = {

    val findAllInvites = CypherReaderFunction.findAllStreamsForDay(token, displayStart, displayLength, sortColumn, sortDirection)
    val findAllInvitesResponse: Future[Any] = ask(neo4jreader, PerformReadOperation(findAllInvites)).mapTo[Any]

    val results = Await.result(findAllInvitesResponse, 30 seconds) match {
      case ReadOperationResult(readResults) =>
        readResults.results.asInstanceOf[List[String]]

    }
    results
  }

  def findAllInvitesForStream(token: String, displayStart: Int, displayLength: Int, sortColumn: Int, sortDirection: String, streamId: String): List[String] = {

    val findAllInvitesForStream = CypherReaderFunction.findAllInvitesForStream(token, displayStart, displayLength, sortColumn, sortDirection, streamId)
    val findAllInvitesForStreamResponse: Future[Any] = ask(neo4jreader, PerformReadOperation(findAllInvitesForStream)).mapTo[Any]

    val results = Await.result(findAllInvitesForStreamResponse, 30 seconds) match {
      case ReadOperationResult(readResults) =>
        readResults.results.asInstanceOf[List[String]]

    }
    results
  }

  def getCountOfAllUsersWhoHaveAcceptedToWatchStream(token: String): BigDecimal = {
    val getCountOfAllUsersWhoHaveAcceptedToWatchStream = CypherReaderFunction.getCountOfAllUsersWhoHaveAcceptedToWatchStream(token)
    val getCountOfAllUsersWhoHaveAcceptedToWatchStreamResponse: Future[Any] = ask(neo4jreader, PerformReadOperation(getCountOfAllUsersWhoHaveAcceptedToWatchStream)).mapTo[Any]

    val results = Await.result(getCountOfAllUsersWhoHaveAcceptedToWatchStreamResponse, 30 seconds) match {
      case ReadOperationResult(readResults) =>
        if (readResults.results.size > 0) {
          readResults.results.head.asInstanceOf[BigDecimal]
        } else {
          BigDecimal(0)
        }

    }
    results
  }

  def getUsersWhoHaveAcceptedToWatchStream(token: String): List[String] = {

    val getUsersWhoHaveAcceptedToWatchStream = CypherReaderFunction.getUsersWhoHaveAcceptedToWatchStream(token)
    val getUsersWhoHaveAcceptedToWatchStreamResponse: Future[Any] = ask(neo4jreader, PerformReadOperation(getUsersWhoHaveAcceptedToWatchStream)).mapTo[Any]

    val results = Await.result(getUsersWhoHaveAcceptedToWatchStreamResponse, 30 seconds) match {
      case ReadOperationResult(readResults) =>
        readResults.results.asInstanceOf[List[String]]

    }
    results
  }


  def getUsersWhoHaveBeenInvitedToWatchStream(token: String): List[String] = {

    val getUsersWhoHaveBeenInvitedToWatchStream = CypherReaderFunction.getUsersWhoHaveBeenInvitedToWatchStream(token)
    val getUsersWhoHaveBeenInvitedToWatchStreamResponse: Future[Any] = ask(neo4jreader, PerformReadOperation(getUsersWhoHaveBeenInvitedToWatchStream)).mapTo[Any]

    val results = Await.result(getUsersWhoHaveBeenInvitedToWatchStreamResponse, 30 seconds) match {
      case ReadOperationResult(readResults) =>
        readResults.results.asInstanceOf[List[String]]

    }
    results
  }


  def getUsersWhoHaveAcceptedToWatchStreamUsingStreamId(streamId: String): List[String] = {

    val getUsersWhoHaveAcceptedToWatchStreamUsingStreamId = CypherReaderFunction.getUsersWhoHaveAcceptedToWatchStreamUsingStreamId(streamId)
    val getUsersWhoHaveAcceptedToWatchStreamUsingStreamIdResponse: Future[Any] = ask(neo4jreader, PerformReadOperation(getUsersWhoHaveAcceptedToWatchStreamUsingStreamId)).mapTo[Any]

    val results = Await.result(getUsersWhoHaveAcceptedToWatchStreamUsingStreamIdResponse, 30 seconds) match {
      case ReadOperationResult(readResults) =>
        readResults.results.asInstanceOf[List[String]]

    }
    results
  }


  def getUsersWhoHaveBeenInvitedToWatchStreamUsingStreamId(streamId: String): List[String] = {

    val getUsersWhoHaveBeenInvitedToWatchStreamUsingStreamId = CypherReaderFunction.getUsersWhoHaveBeenInvitedToWatchStreamUsingStreamId(streamId)
    val getUsersWhoHaveBeenInvitedToWatchStreamUsingStreamIdResponse: Future[Any] = ask(neo4jreader, PerformReadOperation(getUsersWhoHaveBeenInvitedToWatchStreamUsingStreamId)).mapTo[Any]

    val results = Await.result(getUsersWhoHaveBeenInvitedToWatchStreamUsingStreamIdResponse, 30 seconds) match {
      case ReadOperationResult(readResults) =>
        readResults.results.asInstanceOf[List[String]]

    }
    results
  }


  def countNumberAllStreamsForDay(token: String): BigDecimal = {

    val countNumberAllStreamsForDay = CypherReaderFunction.countNumberAllStreamsForDay(token)
    val countNumberAllStreamsForDayResponse: Future[Any] = ask(neo4jreader, PerformReadOperation(countNumberAllStreamsForDay)).mapTo[Any]

    val results = Await.result(countNumberAllStreamsForDayResponse, 30 seconds) match {
      case ReadOperationResult(readResults) =>
        if (readResults.results.size > 0) {
          readResults.results.head.asInstanceOf[BigDecimal]
        } else {
          BigDecimal(0)
        }
    }
    results
  }


  def countAllInvitesForToken(token: String): BigDecimal = {

    val countAllInvitesForToken = CypherReaderFunction.countAllInvitesForToken(token)
    val countAllInvitesForTokenResponse: Future[Any] = ask(neo4jreader, PerformReadOperation(countAllInvitesForToken)).mapTo[Any]

    val results = Await.result(countAllInvitesForTokenResponse, 30 seconds) match {
      case ReadOperationResult(readResults) =>
        if (readResults.results.size > 0) {
          readResults.results.head.asInstanceOf[BigDecimal]
        } else {
          BigDecimal(0)
        }
    }
    results
  }


  def findAllTokensForUser(email: String): List[String] = {

    val findAllTokensForUser = CypherReaderFunction.findAllTokensForUser(email)
    val findAllTokensForUserResponse: Future[Any] = ask(neo4jreader, PerformReadOperation(findAllTokensForUser)).mapTo[Any]

    val results = Await.result(findAllTokensForUserResponse, 30 seconds) match {
      case ReadOperationResult(readResults) =>
        readResults.results.asInstanceOf[List[String]]
    }
    results
  }

  def getStreamsForCalendar(email: String, startYear: Int, endYear: Int,
                            startMonth: Int, endMonth: Int,
                            startDay: Int, endDay: Int): List[String] = {

    val getStreamsForCalendar = CypherReaderFunction.getStreamsForCalendar(email, startYear, endYear, startMonth, endMonth, startDay, endDay)
    val getStreamsForCalendarResponse: Future[Any] = ask(neo4jreader, PerformReadOperation(getStreamsForCalendar)).mapTo[Any]

    val results = Await.result(getStreamsForCalendarResponse, 30 seconds) match {
      case ReadOperationResult(readResults) =>
        readResults.results.asInstanceOf[List[String]]

    }
    results
  }


  def getStreamsForCalendarThatHaveEnded(email: String, startYear: Int, endYear: Int,
                                         startMonth: Int, endMonth: Int,
                                         startDay: Int, endDay: Int): List[String] = {

    val getStreamsForCalendarThatHaveEnded = CypherReaderFunction.getStreamsForCalendarThatHaveEnded(email, startYear, endYear, startMonth, endMonth, startDay, endDay)
    val getStreamsForCalendarThatHaveEndedResponse: Future[Any] = ask(neo4jreader, PerformReadOperation(getStreamsForCalendarThatHaveEnded)).mapTo[Any]

    val results = Await.result(getStreamsForCalendarThatHaveEndedResponse, 30 seconds) match {
      case ReadOperationResult(readResults) =>
        readResults.results.asInstanceOf[List[String]]

    }
    results
  }


  def getEmailUsingToken(token: String): String = {

    val getEmailUsingToken = CypherReaderFunction.getEmailUsingToken(token)
    val getEmailUsingTokenResponse: Future[Any] = ask(neo4jreader, PerformReadOperation(getEmailUsingToken)).mapTo[Any]

    val results = Await.result(getEmailUsingTokenResponse, 30 seconds) match {
      case ReadOperationResult(readResults) =>
        var result = ""
        for (res <- readResults.results) {

          val x: String = res match {
            case Some(s: String) => s
            case None => "?"
          }

          if (!x.equals("?"))
            result = x

        }

        Logger.info("results " + result)
        result
    }

    results
  }


  def checkToSeeIfCheckPasswordIdIsValid(cpId: String): String = {

    val checkToSeeIfCheckPasswordIdIsValid = CypherReaderFunction.checkToSeeIfCheckPasswordIdIsValid(cpId)
    val checkToSeeIfCheckPasswordIdIsValidResponse: Future[Any] = ask(neo4jreader, PerformReadOperation(checkToSeeIfCheckPasswordIdIsValid)).mapTo[Any]

    val results = Await.result(checkToSeeIfCheckPasswordIdIsValidResponse, 30 seconds) match {
      case ReadOperationResult(readResults) =>

        var result = ""
        for (res <- readResults.results) {

          val x: String = res match {
            case Some(s: String) => s
            case None => "?"
          }

          if (!x.equals("?")) {
            result = x
          }
        }

        Logger.info("results " + result)
        result
    }

    results
  }


  import models.UserDetails

  def fetchUserDetails(token: String): UserDetails = {

    val fetchUserDetails = CypherReaderFunction.fetchUserDetails(token)
    val fetchUserDetailsResponse: Future[Any] = ask(neo4jreader, PerformReadOperation(fetchUserDetails)).mapTo[Any]

    val results = Await.result(fetchUserDetailsResponse, 30 seconds) match {
      case ReadOperationResult(readResults) =>
        var result = UserDetails()
        for (res <- readResults.results) {
          val x: UserDetails = res match {
            case (Some(email), Some(firstName), Some(lastName)) => UserDetails(Option(email.asInstanceOf[String]), firstName.asInstanceOf[String], lastName.asInstanceOf[String])
            case _ => null
          }
          if (x != null) {
            result = x
          }

        }

        Logger.info("results " + result)
        result
    }

    results
  }

  import models.UserInformation

  def fetchUserInformation(token: String): UserInformation = {

    val fetchUserInformation = CypherReaderFunction.fetchUserInformation(token)
    val fetchUserInformationResponse: Future[Any] = ask(neo4jreader, PerformReadOperation(fetchUserInformation)).mapTo[Any]

    val results = Await.result(fetchUserInformationResponse, 30 seconds) match {
      case ReadOperationResult(readResults) =>

        var result = UserInformation()
        for (res <- readResults.results) {
          val x: UserInformation = res match {
            case (Some(email), Some(firstName), Some(lastName), Some(domId)) =>
              UserInformation(email.asInstanceOf[String], firstName.asInstanceOf[String], lastName.asInstanceOf[String], Option(domId.asInstanceOf[String]))

            case (Some(email), Some(firstName), Some(lastName), None) =>
              UserInformation(email.asInstanceOf[String], firstName.asInstanceOf[String], lastName.asInstanceOf[String], None)

            case _ => null
          }
          if (x != null) {
            result = x
          }

        }

        Logger.info("results " + result)
        result
    }

    results
  }


  import models.Location

  def fetchLocationForStream(stream: String): List[Location] = {

    val fetchLocationForStream = CypherReaderFunction.fetchLocationForStream(stream)
    val fetchLocationForStreamResponse: Future[Any] = ask(neo4jreader, PerformReadOperation(fetchLocationForStream)).mapTo[Any]

    val results = Await.result(fetchLocationForStreamResponse, 30 seconds) match {
      case ReadOperationResult(readResults) =>

        var result = List[Location]()
        for (res <- readResults.results) {
          val x: Location = res match {
            case (Some(latitude), Some(longitude)) =>
              Location(latitude.asInstanceOf[Double], longitude.asInstanceOf[Double])
            case _ => null
          }
          if (result != null) {
            result = result :+ x
          }

        }

        Logger.info("results " + result)
        result
    }

    results
  }

  def fetchLocationForActiveStream(inviteId: String): List[Location] = {

    val fetchLocationForActiveStream = CypherReaderFunction.fetchLocationForActiveStream(inviteId)
    val fetchLocationForActiveStreamResponse: Future[Any] = ask(neo4jreader, PerformReadOperation(fetchLocationForActiveStream)).mapTo[Any]

    val results = Await.result(fetchLocationForActiveStreamResponse, 30 seconds) match {
      case ReadOperationResult(readResults) =>

        var result = List[Location]()
        for (res <- readResults.results) {
          System.out.println("----fetch-location--resuls" + res)
          val x: Location = res match {
            case (Some(latitude), Some(longitude)) =>
              Location(latitude.asInstanceOf[Double], longitude.asInstanceOf[Double])
            case _ => null
          }
          if (result != null) {
            result = result :+ x
          }

        }

        //Logger.info("results "+result)
        result
    }

    results
  }


  import models.Location

  def fetchLocationForActiveStreamTwitter(inviteId: String): List[Location] = {

    val fetchLocationForActiveStreamTwitter = CypherReaderFunction.fetchLocationForActiveStreamTwitter(inviteId)
    val fetchLocationForActiveStreamResponseTwitter: Future[Any] = ask(neo4jreader, PerformReadOperation(fetchLocationForActiveStreamTwitter)).mapTo[Any]

    val results = Await.result(fetchLocationForActiveStreamResponseTwitter, 30 seconds) match {
      case ReadOperationResult(readResults) =>

        var result = List[Location]()
        for (res <- readResults.results) {
          val x: Location = res match {
            case (Some(latitude), Some(longitude)) =>
              Location(latitude.asInstanceOf[Double], longitude.asInstanceOf[Double])
            case _ => null
          }
          if (result != null) {
            result = result :+ x
          }

        }

        // Logger.info("results "+result)
        result
    }

    results
  }


  import models.Location

  def fetchLocationForActiveStreamFacebook(inviteId: String): List[Location] = {

    val fetchLocationForActiveStreamFacebook = CypherReaderFunction.fetchLocationForActiveStreamFacebook(inviteId)
    val fetchLocationForActiveStreamResponseFacebook: Future[Any] = ask(neo4jreader, PerformReadOperation(fetchLocationForActiveStreamFacebook)).mapTo[Any]

    val results = Await.result(fetchLocationForActiveStreamResponseFacebook, 30 seconds) match {
      case ReadOperationResult(readResults) =>

        var result = List[Location]()
        for (res <- readResults.results) {
          val x: Location = res match {
            case (Some(latitude), Some(longitude)) =>
              Location(latitude.asInstanceOf[Double], longitude.asInstanceOf[Double])
            case _ => null
          }
          if (result != null)
            result = result :+ x

        }

        // Logger.info("results "+result)
        result
    }

    results
  }

  def fetchLocationForActiveStreamLinkedin(inviteId: String): List[Location] = {

    val fetchLocationForActiveStreamLinkedin = CypherReaderFunction.fetchLocationForActiveStreamLinkedin(inviteId)
    val fetchLocationForActiveStreamResponseLinkedin: Future[Any] = ask(neo4jreader, PerformReadOperation(fetchLocationForActiveStreamLinkedin)).mapTo[Any]

    val results = Await.result(fetchLocationForActiveStreamResponseLinkedin, 30 seconds) match {
      case ReadOperationResult(readResults) =>

        var result = List[Location]()
        for (res <- readResults.results) {
          val x: Location = res match {
            case (Some(latitude), Some(longitude)) =>
              Location(latitude.asInstanceOf[Double], longitude.asInstanceOf[Double])
            case _ => null

          }
          if (result != null) {
            result = result :+ x
          }

        }

        // Logger.info("results "+result)
        result

    }
    results
  }


  def countAllTwitterInvites(token: String, clause: String): BigDecimal = {

    val countAllTwitterInvites = CypherReaderFunction.countAllTwitterInvites(token, clause)
    val countAllTwitterInvitesResponse: Future[Any] = ask(neo4jreader, PerformReadOperation(countAllTwitterInvites)).mapTo[Any]

    val results = Await.result(countAllTwitterInvitesResponse, 30 seconds) match {
      case ReadOperationResult(readResults) =>
        if (readResults.results.size > 0) {
          readResults.results.head.asInstanceOf[BigDecimal]
        } else {
          BigDecimal(0)
        }
    }

    results
  }


  def countAllFacebookInvites(token: String, clause: String): BigDecimal = {

    val countAllFacebookInvites = CypherReaderFunction.countAllFacebookInvites(token, clause)
    val countAllFacebookInvitesResponse: Future[Any] = ask(neo4jreader, PerformReadOperation(countAllFacebookInvites)).mapTo[Any]
    val results = Await.result(countAllFacebookInvitesResponse, 30 seconds) match {
      case ReadOperationResult(readResults) =>
        if (readResults.results.size > 0) {
          readResults.results.head.asInstanceOf[BigDecimal]
        } else {
          BigDecimal(0)
        }


    }
    results
  }

  def countAllLinkedinInvites(token: String, clause: String): BigDecimal = {

    val countAllLinkedinInvites = CypherReaderFunction.countAllLinkedinInvites(token, clause)
    val countAllLinkedinInvitesResponse: Future[Any] = ask(neo4jreader, PerformReadOperation(countAllLinkedinInvites)).mapTo[Any]

    val results = Await.result(countAllLinkedinInvitesResponse, 30 seconds) match {
      case ReadOperationResult(readResults) =>
        if (readResults.results.size > 0) {
          readResults.results.head.asInstanceOf[BigDecimal]
        } else {
          BigDecimal(0)
        }
    }

    results
  }

  def getFacebookViewers(token: String, streamClause: String = ""): BigDecimal = {

    val getFacebookViewers = CypherReaderFunction.getFacebookViewers(token, streamClause)
    val getFacebookViewersResponse: Future[Any] = ask(neo4jreader, PerformReadOperation(getFacebookViewers)).mapTo[Any]

    val results = Await.result(getFacebookViewersResponse, 30 seconds) match {
      case ReadOperationResult(readResults) =>
        if (readResults.results.size > 0) {
          readResults.results.head.asInstanceOf[BigDecimal]
        } else {
          BigDecimal(0)
        }

    }

    results
  }

  def getFacebookAcceptanceCount(token: String, streamClause: String = ""): BigDecimal = {

    val getFacebookAcceptanceCount = CypherReaderFunction.getFacebookAcceptanceCount(token, streamClause)
    val getFacebookAcceptanceCountResponse: Future[Any] = ask(neo4jreader, PerformReadOperation(getFacebookAcceptanceCount)).mapTo[Any]

    val results = Await.result(getFacebookAcceptanceCountResponse, 30 seconds) match {
      case ReadOperationResult(readResults) =>
        if (readResults.results.size > 0) {
          readResults.results.head.asInstanceOf[BigDecimal]
        } else {
          BigDecimal(0)
        }

    }

    //Logger.info("---facebookaccept:"+results)
    results
  }


  def getTwitterViewers(token: String, streamClause: String = ""): BigDecimal = {

    val getTwitterViewers = CypherReaderFunction.getTwitterViewers(token, streamClause)
    val getTwitterViewersResponse: Future[Any] = ask(neo4jreader, PerformReadOperation(getTwitterViewers)).mapTo[Any]

    val results = Await.result(getTwitterViewersResponse, 30 seconds) match {
      case ReadOperationResult(readResults) =>
        if (readResults.results.size > 0) {
          readResults.results.head.asInstanceOf[BigDecimal]
        } else {
          BigDecimal(0)
        }

    }

    results
  }

  def getTwitterAcceptanceCount(token: String, streamClause: String = ""): BigDecimal = {

    val getTwitterAcceptanceCount = CypherReaderFunction.getTwitterAcceptanceCount(token, streamClause)
    val getTwitterAcceptanceCountResponse: Future[Any] = ask(neo4jreader, PerformReadOperation(getTwitterAcceptanceCount)).mapTo[Any]

    val results = Await.result(getTwitterAcceptanceCountResponse, 30 seconds) match {
      case ReadOperationResult(readResults) =>
        if (readResults.results.size > 0) {
          readResults.results.head.asInstanceOf[BigDecimal]
        } else {
          BigDecimal(0)
        }

    }

    results
  }


  def getLinkedinViewers(token: String, streamClause: String = ""): BigDecimal = {

    val getLinkedinViewers = CypherReaderFunction.getLinkedinViewers(token, streamClause)
    val getLinkedinViewersResponse: Future[Any] = ask(neo4jreader, PerformReadOperation(getLinkedinViewers)).mapTo[Any]

    val results = Await.result(getLinkedinViewersResponse, 30 seconds) match {
      case ReadOperationResult(readResults) =>
        if (readResults.results.size > 0) {
          readResults.results.head.asInstanceOf[BigDecimal]
        } else {
          BigDecimal(0)
        }

    }

    results
  }

  def getEmailViewers(token: String, streamId: String = ""): BigDecimal = {

    val streamClause = "where s.name=\"" + streamId + "\""
    val getEmailViewers = CypherReaderFunction.getEmailViewers(token, streamClause)
    val getEmailViewersResponse: Future[Any] = ask(neo4jreader, PerformReadOperation(getEmailViewers)).mapTo[Any]

    val results = Await.result(getEmailViewersResponse, 30 seconds) match {
      case ReadOperationResult(readResults) =>
        if (readResults.results.size > 0) {
          readResults.results.head.asInstanceOf[BigDecimal]
        } else {
          BigDecimal(0)
        }

    }

    results
  }


  def getLinkedinAcceptanceCount(token: String, streamClause: String = ""): BigDecimal = {


    val getLinkedinAcceptanceCount = CypherReaderFunction.getLinkedinAcceptanceCount(token, streamClause)
    val getLinkedinAcceptanceCountResponse: Future[Any] = ask(neo4jreader, PerformReadOperation(getLinkedinAcceptanceCount)).mapTo[Any]

    val results = Await.result(getLinkedinAcceptanceCountResponse, 30 seconds) match {
      case ReadOperationResult(readResults) =>
        if (readResults.results.size > 0) {
          readResults.results.head.asInstanceOf[BigDecimal]
        } else {
          BigDecimal(0)
        }

    }
    results
  }


  def getReferersForLinkedin(stream: String): List[String] = {

    val getReferersForLinkedin = CypherReaderFunction.getReferersForLinkedin(stream)
    val getReferersForLinkedinResponse: Future[Any] = ask(neo4jreader, PerformReadOperation(getReferersForLinkedin)).mapTo[Any]

    val results = Await.result(getReferersForLinkedinResponse, 30 seconds) match {
      case ReadOperationResult(readResults) =>

        var result = List[String]()
        for (res <- readResults.results) {
          val x: String = res match {
            case Some(ip) => ip.asInstanceOf[String]
            case _ => null
          }
          if (x != null)
            result = result :+ x

        }
        Logger.info("results " + result)
        result
    }

    results
  }

  def getReferersForTwitter(stream: String): List[String] = {

    val getReferersForTwitter = CypherReaderFunction.getReferersForTwitter(stream)
    val getReferersForTwitterResponse: Future[Any] = ask(neo4jreader, PerformReadOperation(getReferersForTwitter)).mapTo[Any]

    val results = Await.result(getReferersForTwitterResponse, 30 seconds) match {
      case ReadOperationResult(readResults) =>

        var result = List[String]()
        for (res <- readResults.results) {
          val x: String = res match {
            case Some(ip) => ip.asInstanceOf[String]
            case _ => null
          }
          if (x != null)
            result = result :+ x

        }
        Logger.info("results " + result)
        result
    }

    results
  }

  def getReferersForFacebook(stream: String): List[String] = {

    val getReferersForFacebook = CypherReaderFunction.getReferersForFacebook(stream)
    val getReferersForFacebookResponse: Future[Any] = ask(neo4jreader, PerformReadOperation(getReferersForFacebook)).mapTo[Any]

    val results = Await.result(getReferersForFacebookResponse, 30 seconds) match {
      case ReadOperationResult(readResults) =>

        var result = List[String]()
        for (res <- readResults.results) {
          val x: String = res match {
            case Some(ip) => ip.asInstanceOf[String]
            case _ => null
          }
          if (x != null)
            result = result :+ x

        }
        Logger.info("results " + result)
        result
    }

    results
  }


  def getRoomJid(token: String): String = {

    val getRoomJid = CypherReaderFunction.getRoomJid(token)
    val getRoomJidResponse: Future[Any] = ask(neo4jreader, PerformReadOperation(getRoomJid)).mapTo[Any]

    val results = Await.result(getRoomJidResponse, 30 seconds) match {
      case ReadOperationResult(readResults) =>

        var result = List[String]()
        for (res <- readResults.results) {
          val x: String = res match {
            case Some(ip) => ip.asInstanceOf[String]
            case _ => null
          }
          if (x != null)
            result = result :+ x

        }
        Logger.info("results " + result)
        if (result.size > 0) {
          result.head
        } else {
          ""
        }
    }


    results
  }

  def getRoomJidForStream(stream: String): String = {

    val getRoomJidForStream = CypherReaderFunction.getRoomJidForStream(stream)
    val getRoomJidForStreamResponse: Future[Any] = ask(neo4jreader, PerformReadOperation(getRoomJidForStream)).mapTo[Any]

    val results = Await.result(getRoomJidForStreamResponse, 30 seconds) match {
      case ReadOperationResult(readResults) =>

        var result = List[String]()
        for (res <- readResults.results) {
          val x: String = res match {
            case Some(ip) => ip.asInstanceOf[String]
            case _ => null
          }

          if (x != null)
            result = result :+ x


        }
        Logger.info("results " + result)
        if (result.size > 0) {
          result.head
        } else {
          ""
        }
    }
    results
  }

  import models.UserInformation

  def getUserInformationUsingInviteId(inviteId: String): UserInformation = {

    val getUserInformationUsingInviteId = CypherReaderFunction.getUserInformationUsingInviteId(inviteId)
    val getUserInformationUsingInviteIdResponse: Future[Any] = ask(neo4jreader, PerformReadOperation(getUserInformationUsingInviteId)).mapTo[Any]

    val results = Await.result(getUserInformationUsingInviteIdResponse, 30 seconds) match {
      case ReadOperationResult(readResults) =>

        var result = UserInformation()
        for (res <- readResults.results) {
          val x: UserInformation = res match {
            case (Some(email), Some(firstName), Some(lastName), Some(domId)) =>
              UserInformation(email.asInstanceOf[String], firstName.asInstanceOf[String], lastName.asInstanceOf[String], Option(domId.asInstanceOf[String]))

            case (Some(email), Some(firstName), Some(lastName), None) =>
              UserInformation(email.asInstanceOf[String], firstName.asInstanceOf[String], lastName.asInstanceOf[String], None)

            case _ => null
          }
          if (x != null)
            result = x

        }


        result
    }
    results
  }


}