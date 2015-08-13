package com.whatamidoing.cypher

import models.Neo4jResult
import org.anormcypher.Cypher

object CypherReaderFunction {

  def searchForUser(em: String): () => Neo4jResult = {

    val searchForUser: Function0[Neo4jResult] = () => {
      var res = Cypher(CypherReader.searchForUser(em))
      val response = res.apply().map(row => row[String]("password")).toList
      val neo4jResult = new Neo4jResult(response)
      neo4jResult
    }
    searchForUser
  }

  val Tag: String = "CypherBuilder"

  def getUserToken(em: String): () => Neo4jResult = {

    val getUserToken: Function0[Neo4jResult] = () => {
      val tokens = Cypher(CypherReader.getTokenForUser(em)).apply().map(row => (row[String]("token"), row[String]("status"))).toList
      val neo4jResult = new Neo4jResult(tokens)
      // Logger(Tag).info("getUserToken:this is the token: " + tokens)
      neo4jResult
    }
    getUserToken
  }

  def getValidToken(token: String): () => Neo4jResult = {
    val getValidToken: Function0[Neo4jResult] = () => {
      val tokens = Cypher(CypherReader.getValidToken(token)).apply().map(row => row[String]("token")).toList
      val neo4jResult = new Neo4jResult(tokens)
      // Logger(Tag).info("getUserToken:this is a valid token: " + tokens)
      neo4jResult
    }
    getValidToken

  }

  def findActiveStreamForToken(token: String): () => Neo4jResult = {
    val findActiveStream: Function0[Neo4jResult] = () => {
      val name = Cypher(CypherReader.findActiveStreamForToken(token)).apply().map(row => row[String]("name")).toList
      val neo4jResult = new Neo4jResult(name)
      // Logger(Tag).info("findActiveStreamForToken: name of active strem:"+name)
      neo4jResult
    }
    findActiveStream
  }

  def findStreamForInvitedId(invitedId: String): () => Neo4jResult = {
    val streamForInvitedId: Function0[Neo4jResult] = () => {
      val name = Cypher(CypherReader.findStreamForInvitedId(invitedId)).apply().map(row => row[String]("name")).toList
      val neo4jResult = new Neo4jResult(name)
      //Logger(Tag).info("findStreamForInvitedId: name of active stream:"+name)
      neo4jResult
    }
    streamForInvitedId
  }


  def findStreamForInviteTwitter(invitedId: String): () => Neo4jResult = {
    val streamForInviteTwitter: Function0[Neo4jResult] = () => {
      val name = Cypher(CypherReader.findStreamForInviteTwitter(invitedId)).apply().map(row => row[String]("name")).toList
      val neo4jResult = new Neo4jResult(name)
      // Logger(Tag).info("findStreamForInviteTwitter: name of active stream:"+name)
      neo4jResult
    }
    streamForInviteTwitter
  }

  def findStreamForInviteFacebook(invitedId: String): () => Neo4jResult = {
    val streamForInviteFacebook: Function0[Neo4jResult] = () => {
      val name = Cypher(CypherReader.findStreamForInviteFacebook(invitedId)).apply().map(row => row[String]("name")).toList
      val neo4jResult = new Neo4jResult(name)
      // Logger(Tag).info("findStreamForInviteFacebook: name of active stream:"+name)
      neo4jResult
    }
    streamForInviteFacebook
  }

  def findStreamForInviteLinkedin(invitedId: String): () => Neo4jResult = {
    val streamForInviteLinkedin: Function0[Neo4jResult] = () => {
      val name = Cypher(CypherReader.findStreamForInviteLinkedin(invitedId)).apply().map(row => row[String]("name")).toList
      val neo4jResult = new Neo4jResult(name)
      // Logger(Tag).info("findStreamForInviteLinkedin: name of active stream:"+name)
      neo4jResult
    }
    streamForInviteLinkedin
  }


  def checkToSeeIfFacebookInviteAcceptedAlreadyByReferer(invitedId: String, referer: String): () => Neo4jResult = {
    val checkToSeeIfFacebookInviteAcceptedAlreadyByReferer: Function0[Neo4jResult] = () => {
      val id = Cypher(CypherReader.checkToSeeIfFacebookInviteAcceptedAlreadyByReferer(invitedId, referer)).apply().map(row => row[String]("id")).toList
      val neo4jResult = new Neo4jResult(id)
      //Logger(Tag).info("streamForInviteFacebook: referer id is:"+id)
      neo4jResult
    }
    checkToSeeIfFacebookInviteAcceptedAlreadyByReferer
  }

  def checkToSeeIfLinkedinInviteAcceptedAlreadyByReferer(invitedId: String, referer: String): () => Neo4jResult = {
    val checkToSeeIfLinkedinInviteAcceptedAlreadyByReferer: Function0[Neo4jResult] = () => {
      val id = Cypher(CypherReader.checkToSeeIfLinkedinInviteAcceptedAlreadyByReferer(invitedId, referer)).apply().map(row => row[String]("id")).toList
      val neo4jResult = new Neo4jResult(id)
      //Logger(Tag).info("streamForInviteLinkedin: referer id is:"+id)
      neo4jResult
    }
    checkToSeeIfLinkedinInviteAcceptedAlreadyByReferer
  }


  def checkToSeeIfTwitterInviteAcceptedAlreadyByReferer(invitedId: String, referer: String): () => Neo4jResult = {
    val checkToSeeIfTwitterInviteAcceptedAlreadyByReferer: Function0[Neo4jResult] = () => {
      val id = Cypher(CypherReader.checkToSeeIfTwitterInviteAcceptedAlreadyByReferer(invitedId, referer)).apply().map(row => row[String]("id")).toList
      val neo4jResult = new Neo4jResult(id)
      // Logger(Tag).info("streamForInviteTwitter:referer id is:"+id)
      neo4jResult
    }
    checkToSeeIfTwitterInviteAcceptedAlreadyByReferer
  }

  def findAllInvites(email: String): () => Neo4jResult = {
    val findAllInvites: Function0[Neo4jResult] = () => {
      val allInvites = Cypher(CypherReader.findAllInvites(email)).apply().map(row => row[String]("email")).toList
      val neo4jResult = new Neo4jResult(allInvites)
      //Logger(Tag).info("findAllInvites:all invites:"+allInvites)
      neo4jResult
    }
    findAllInvites
  }

  def findAllStreamsForDay(token: String, displayStart: Int, displayLength: Int, sortColumn: Int, sortDirection: String): () => Neo4jResult = {
    val findAllStreamsForDay: Function0[Neo4jResult] = () => {
      val allStreams = Cypher(CypherReader.findAllStreamsForDay(token, displayStart, displayLength, sortColumn, sortDirection)).apply().map(row => (row[String]("stream"), row[String]("day"), row[String]("startTime"), row[Option[String]]("end"), row[Option[String]]("endTime"))).toList
      val neo4jResult = new Neo4jResult(allStreams)
      // Logger(Tag).info("findAllStreamsForDay:all streams:"+allStreams)
      neo4jResult
    }
    findAllStreamsForDay
  }


  def findAllInvitesForStream(token: String, displayStart: Int, displayLength: Int, sortColumn: Int, sortDirection: String, streamId: String): () => Neo4jResult = {
    val findAllInvitesForStream: Function0[Neo4jResult] = () => {
      val allStreams = Cypher(CypherReader.findAllInvitesForStream(token, displayStart, displayLength, sortColumn, sortDirection, streamId)).apply().map(row => (row[Option[String]]("day"), row[Option[String]]("time"), row[String]("email"), row[Option[String]]("firstName"), row[Option[String]]("lastName"))).toList
      val neo4jResult = new Neo4jResult(allStreams)
      // Logger(Tag).info("findAllStreamsForDay:all streams:"+allStreams)
      neo4jResult
    }

    findAllInvitesForStream
  }

  def getCountOfAllUsersWhoHaveAcceptedToWatchStream(token: String): () => Neo4jResult = {
    val getCountOfAllUsersWhoHaveAcceptedToWatchStream: Function0[Neo4jResult] = () => {
      val allCounts = Cypher(CypherReader.getCountOfAllUsersWhoHaveAcceptedToWatchStream(token)).apply().map(row => row[BigDecimal]("count")).toList
      val neo4jResult = new Neo4jResult(allCounts)
      neo4jResult
    }
    getCountOfAllUsersWhoHaveAcceptedToWatchStream

  }


  def getUsersWhoHaveAcceptedToWatchStream(token: String): () => Neo4jResult = {
    val getUsersWhoHaveAcceptedToWatchStream: Function0[Neo4jResult] = () => {
      val allStreams = Cypher(CypherReader.getUsersWhoHaveAcceptedToWatchStream(token)).apply().map(row => (row[Option[String]]("email"), row[Option[String]]("firstName"), row[Option[String]]("lastName"))).toList
      val neo4jResult = new Neo4jResult(allStreams)
      neo4jResult
    }
    getUsersWhoHaveAcceptedToWatchStream
  }

  def getUsersWhoHaveBeenInvitedToWatchStream(token: String): () => Neo4jResult = {
    val getUsersWhoHaveBeenInvitedToWatchStream: Function0[Neo4jResult] = () => {
      val allStreams = Cypher(CypherReader.getUsersWhoHaveBeenInvitedToWatchStream(token)).apply().map(row => (row[Option[String]]("email"), row[Option[String]]("firstName"), row[Option[String]]("lastName"))).toList
      val neo4jResult = new Neo4jResult(allStreams)
      neo4jResult
    }
    getUsersWhoHaveBeenInvitedToWatchStream
  }

  def getUsersWhoHaveAcceptedToWatchStreamUsingStreamId(streamId: String): () => Neo4jResult = {
    val getUsersWhoHaveAcceptedToWatchStreamUsingStreamId: Function0[Neo4jResult] = () => {
      val allStreams = Cypher(CypherReader.getUsersWhoHaveAcceptedToWatchStreamUsingStreamId(streamId)).apply().map(row => (row[Option[String]]("email"), row[Option[String]]("firstName"), row[Option[String]]("lastName"))).toList
      val neo4jResult = new Neo4jResult(allStreams)
      neo4jResult
    }
    getUsersWhoHaveAcceptedToWatchStreamUsingStreamId
  }

  def getUsersWhoHaveBeenInvitedToWatchStreamUsingStreamId(streamId: String): () => Neo4jResult = {
    val getUsersWhoHaveBeenInvitedToWatchStreamUsingStreamId: Function0[Neo4jResult] = () => {
      val allStreams = Cypher(CypherReader.getUsersWhoHaveBeenInvitedToWatchStreamUsingStreamId(streamId)).apply().map(row => (row[Option[String]]("email"), row[Option[String]]("firstName"), row[Option[String]]("lastName"))).toList
      val neo4jResult = new Neo4jResult(allStreams)
      neo4jResult
    }
    getUsersWhoHaveBeenInvitedToWatchStreamUsingStreamId
  }

  def countNumberAllStreamsForDay(token: String): () => Neo4jResult = {
    val countNumberAllStreamsForDay: Function0[Neo4jResult] = () => {
      val allStreams = Cypher(CypherReader.countNumberAllStreamsForDay(token)).apply().map(row => row[BigDecimal]("count")).toList
      val neo4jResult = new Neo4jResult(List(allStreams.head.toString()))
      // Logger(Tag).info("countNumberAllStreamsForDay:numbers streams:"+allStreams)
      neo4jResult
    }
    countNumberAllStreamsForDay
  }

  def countAllInvitesForToken(token: String): () => Neo4jResult = {
    val countAllInvitesForToken: Function0[Neo4jResult] = () => {
      val allStreams = Cypher(CypherReader.countAllInvitesForToken(token)).apply().map(row => row[BigDecimal]("count")).toList
      val neo4jResult = new Neo4jResult(List(allStreams.head.toString()))
      // Logger(Tag).info("countNumberAllStreamsForDay:numbers streams:"+allStreams)
      neo4jResult
    }
    countAllInvitesForToken
  }


  def findAllTokensForUser(email: String): () => Neo4jResult = {
    val findAllTokensForUser: Function0[Neo4jResult] = () => {
      val allStreams = Cypher(CypherReader.findAllTokensForUser(email)).apply().map(row => row[Option[String]]("token")).toList
      val neo4jResult = new Neo4jResult(allStreams)
      // Logger(Tag").info("findAllStreamsForDay:all streams:"+allStreams)
      neo4jResult
    }
    findAllTokensForUser
  }


  def getStreamsForCalendar(email: String,
                            startYear: Int, endYear: Int,
                            startMonth: Int, endMonth: Int,
                            startDay: Int, endDay: Int): () => Neo4jResult = {

    val getStreamsForCalendar: Function0[Neo4jResult] = () => {
      val allStreams = Cypher(CypherReader.getStreamsForCalendar(email, startYear, endYear, startMonth, endMonth, startDay, endDay)).apply().map(row => (row[Option[BigDecimal]]("year"), row[Option[BigDecimal]]("month"), row[Option[BigDecimal]]("day"), row[Option[String]]("time"), row[Option[String]]("streamId"), row[Option[String]]("streamName"))).toList
      val neo4jResult = new Neo4jResult(allStreams)
      neo4jResult
    }
    getStreamsForCalendar
  }

  def getStreamsForCalendarThatHaveEnded(email: String,
                                         startYear: Int, endYear: Int,
                                         startMonth: Int, endMonth: Int,
                                         startDay: Int, endDay: Int): () => Neo4jResult = {

    val getStreamsForCalendarThatHaveEnded: Function0[Neo4jResult] = () => {
      val allStreams = Cypher(CypherReader.getStreamsForCalendarThatHaveEnded(email, startYear, endYear, startMonth, endMonth, startDay, endDay)).apply().map(row => (row[Option[BigDecimal]]("year"), row[Option[BigDecimal]]("month"), row[Option[BigDecimal]]("day"), row[Option[String]]("time"), row[Option[String]]("streamId"), row[Option[String]]("streamName"))).toList
      val neo4jResult = new Neo4jResult(allStreams)
      neo4jResult
    }
    getStreamsForCalendarThatHaveEnded
  }

  def getEmailUsingToken(token: String): () => Neo4jResult = {

    val getEmailUsingToken: Function0[Neo4jResult] = () => {
      val allStreams = Cypher(CypherReader.getEmailUsingToken(token)).apply().map(row => row[Option[String]]("email")).toList
      val neo4jResult = new Neo4jResult(allStreams)
      neo4jResult
    }
    getEmailUsingToken
  }

  def checkToSeeIfCheckPasswordIdIsValid(cpId: String): () => Neo4jResult = {

    val checkToSeeIfCheckPasswordIdIsValid: Function0[Neo4jResult] = () => {
      val checkToSeeIfCheckPasswordIdIsValid = Cypher(CypherReader.checkToSeeIfCheckPasswordIdIsValid(cpId)).apply().map(row => row[Option[String]]("state")).toList
      val neo4jResult = new Neo4jResult(checkToSeeIfCheckPasswordIdIsValid)
      neo4jResult
    }
    checkToSeeIfCheckPasswordIdIsValid
  }


  def fetchUserDetails(cpId: String): () => Neo4jResult = {

    val fetchUserDetails: Function0[Neo4jResult] = () => {
      val fetchUserDetails = Cypher(CypherReader.fetchUserDetails(cpId)).apply().map(row => (row[Option[String]]("email"), row[Option[String]]("firstName"), row[Option[String]]("lastName"))).toList
      val neo4jResult = new Neo4jResult(fetchUserDetails)
      neo4jResult
    }
    fetchUserDetails
  }


  def fetchUserInformation(token: String): () => Neo4jResult = {

    val fetchUserInformation: Function0[Neo4jResult] = () => {
      System.out.println("user information")
      val fetchUserInformation = Cypher(CypherReader.fetchUserInformation(token)).apply().map(row => (row[Option[String]]("email"), row[Option[String]]("firstName"), row[Option[String]]("lastName"), row[Option[String]]("domId"))).toList
      System.out.println("results:" + fetchUserInformation)
      val neo4jResult = new Neo4jResult(fetchUserInformation)
      neo4jResult
    }
    fetchUserInformation
  }

  def fetchLocationForStream(stream: String): () => Neo4jResult = {
    val fetchLocationForStream: Function0[Neo4jResult] = () => {
      val fetchLocationForStream = Cypher(CypherReader.fetchLocationForStream(stream)).apply().map(row => (row[Option[Double]]("latitude"), row[Option[Double]]("longitude"))).toList
      val neo4jResult = new Neo4jResult(fetchLocationForStream)
      neo4jResult
    }
    fetchLocationForStream
  }

  def fetchLocationForActiveStream(inviteId: String): () => Neo4jResult = {
    val fetchLocationForActiveStream: Function0[Neo4jResult] = () => {
      val fetchLocationForActiveStream = Cypher(CypherReader.fetchLocationForActiveStream(inviteId)).apply().map(row => (row[Option[Double]]("latitude"), row[Option[Double]]("longitude"))).toList
      val neo4jResult = new Neo4jResult(fetchLocationForActiveStream)
      neo4jResult
    }
    fetchLocationForActiveStream
  }

  def fetchLocationForActiveStreamTwitter(inviteId: String): () => Neo4jResult = {
    val fetchLocationForActiveStreamTwitter: Function0[Neo4jResult] = () => {
      val fetchLocationForActiveStreamTwitter = Cypher(CypherReader.fetchLocationForActiveStreamTwitter(inviteId)).apply().map(row => (row[Option[Double]]("latitude"), row[Option[Double]]("longitude"))).toList
      val neo4jResult = new Neo4jResult(fetchLocationForActiveStreamTwitter)
      neo4jResult
    }
    fetchLocationForActiveStreamTwitter
  }

  def fetchLocationForActiveStreamFacebook(inviteId: String): () => Neo4jResult = {
    val fetchLocationForActiveStreamFacebook: Function0[Neo4jResult] = () => {
      val fetchLocationForActiveStreamFacebook = Cypher(CypherReader.fetchLocationForActiveStreamFacebook(inviteId)).apply().map(row => (row[Option[Double]]("latitude"), row[Option[Double]]("longitude"))).toList
      val neo4jResult = new Neo4jResult(fetchLocationForActiveStreamFacebook)
      neo4jResult
    }
    fetchLocationForActiveStreamFacebook
  }

  def fetchLocationForActiveStreamLinkedin(inviteId: String): () => Neo4jResult = {
    val fetchLocationForActiveStreamLinkedin: Function0[Neo4jResult] = () => {
      val fetchLocationForActiveStreamLinkedin = Cypher(CypherReader.fetchLocationForActiveStreamLinkedin(inviteId)).apply().map(row => (row[Option[Double]]("latitude"), row[Option[Double]]("longitude"))).toList
      val neo4jResult = new Neo4jResult(fetchLocationForActiveStreamLinkedin)
      neo4jResult
    }
    fetchLocationForActiveStreamLinkedin
  }

  def countAllTwitterInvites(token: String, clause: String): () => Neo4jResult = {
    val countAllTwitterInvites: Function0[Neo4jResult] = () => {
      val allInvites = Cypher(CypherReader.countAllTwitterInvites(token, clause)).apply().map(row => row[BigDecimal]("count")).toList
      val count = if (allInvites.size > 0) allInvites.head else BigDecimal(0)
      val neo4jResult = new Neo4jResult(List(count))
      // Logger(Tag).info("countAllTwitterInvites:number twitter invites:"+allInvites)
      neo4jResult
    }
    countAllTwitterInvites
  }

  def countAllFacebookInvites(token: String, clause: String): () => Neo4jResult = {
    val countAllFacebookInvites: Function0[Neo4jResult] = () => {
      val allInvites = Cypher(CypherReader.countAllFacebookInvites(token, clause)).apply().map(row => row[BigDecimal]("count")).toList
      val count = if (allInvites.size > 0) allInvites.head else BigDecimal(0)
      val neo4jResult = new Neo4jResult(List(count))
      // Logger(Tag).info("countAllFacebookInvites:number facebook invites:"+allInvites)
      neo4jResult
    }
    countAllFacebookInvites
  }

  def countAllLinkedinInvites(token: String, clause: String): () => Neo4jResult = {
    val countAllLinkedinInvites: Function0[Neo4jResult] = () => {
      val allInvites = Cypher(CypherReader.countAllLinkedinInvites(token, clause)).apply().map(row => row[BigDecimal]("count")).toList
      val count = if (allInvites.size > 0) allInvites.head else BigDecimal(0)
      val neo4jResult = new Neo4jResult(List(count))
      // Logger(Tag).info("countAllLinkedinInvites:number linkedin invites:"+allInvites)
      neo4jResult
    }
    countAllLinkedinInvites
  }


  def getFacebookViewers(token: String, streamClause: String = ""): () => Neo4jResult = {
    val getFacebookViewersCount: Function0[Neo4jResult] = () => {
      val allInvites = Cypher(CypherReader.getFacebookViewers(token, streamClause)).apply().map(row => row[BigDecimal]("count")).toList
      val count = if (allInvites.size > 0) allInvites.head else BigDecimal(0)
      val neo4jResult = new Neo4jResult(List(count))
      //   Logger(Tag).info("getFacebookViewers:number facebook referers:"+allInvites)
      neo4jResult
    }
    getFacebookViewersCount
  }

  def getEmailViewers(token: String, streamClause: String = ""): () => Neo4jResult = {
    val getEmailViewersCount: Function0[Neo4jResult] = () => {
      val allInvites = Cypher(CypherReader.getEmailViewers(token, streamClause)).apply().map(row => row[BigDecimal]("count")).toList
      val count = if (allInvites.size > 0) allInvites.head else BigDecimal(0)
      val neo4jResult = new Neo4jResult(List(count))
      // Logger(Tag).info("getEmailViewers:number email viewers:"+allInvites)
      neo4jResult
    }
    getEmailViewersCount
  }


  def getFacebookAcceptanceCount(token: String, streamClause: String): () => Neo4jResult = {
    val getFacebookAcceptanceCount: Function0[Neo4jResult] = () => {
      val allInvites = Cypher(CypherReader.getFacebookAcceptanceCount(token, streamClause)).apply().map(row => row[BigDecimal]("count")).toList
      val count = if (allInvites.size > 0) allInvites.head else BigDecimal(0)
      val neo4jResult = new Neo4jResult(List(count))
      //    Logger(Tag).info("getFacebookAcceptanceCount:number facebook referers:"+allInvites)
      neo4jResult
    }
    getFacebookAcceptanceCount
  }


  def getTwitterViewers(token: String, streamClause: String = ""): () => Neo4jResult = {
    val getTwitterViewersCount: Function0[Neo4jResult] = () => {
      val allInvites = Cypher(CypherReader.getTwitterViewers(token, streamClause)).apply().map(row => row[BigDecimal]("count")).toList
      val count = if (allInvites.size > 0) allInvites.head else BigDecimal(0)
      val neo4jResult = new Neo4jResult(List(count))
      //      Logger(Tag).info("getTwitterViewers:number twitter referers:"+allInvites)
      neo4jResult
    }
    getTwitterViewersCount
  }

  def getTwitterAcceptanceCount(token: String, streamClause: String = ""): () => Neo4jResult = {
    val getTwitterAcceptanceCount: Function0[Neo4jResult] = () => {
      val allInvites = Cypher(CypherReader.getTwitterAcceptanceCount(token, streamClause)).apply().map(row => row[BigDecimal]("count")).toList
      val count = if (allInvites.size > 0) allInvites.head else BigDecimal(0)
      val neo4jResult = new Neo4jResult(List(count))
      // Logger("CypherBuilder.getTwitterAcceptanceCount").info("number twitter referers:"+allInvites)
      neo4jResult
    }
    getTwitterAcceptanceCount
  }


  def getLinkedinViewers(token: String, streamClause: String = ""): () => Neo4jResult = {
    val getLinkedinViewersCount: Function0[Neo4jResult] = () => {
      val allInvites = Cypher(CypherReader.getLinkedinViewers(token, streamClause)).apply().map(row => row[BigDecimal]("count")).toList
      val count = if (allInvites.size > 0) allInvites.head else BigDecimal(0)
      val neo4jResult = new Neo4jResult(List(count))
      // Logger(Tag).info("getLinkedinViewers:number linkedin referers:"+allInvites)
      neo4jResult
    }
    getLinkedinViewersCount
  }

  def getLinkedinAcceptanceCount(token: String, streamClause: String = ""): () => Neo4jResult = {
    val getLinkedinAcceptanceCount: Function0[Neo4jResult] = () => {
      val allInvites = Cypher(CypherReader.getLinkedinAcceptanceCount(token, streamClause)).apply().map(row => row[BigDecimal]("count")).toList
      val count = if (allInvites.size > 0) allInvites.head else BigDecimal(0)
      val neo4jResult = new Neo4jResult(List(count))
      // Logger(Tag).info("getLinkedinAcceptanceCount:number facebook referers:"+allInvites)
      neo4jResult
    }
    getLinkedinAcceptanceCount
  }

  def getReferersForLinkedin(stream: String): () => Neo4jResult = {

    val getReferersForLinkedin: Function0[Neo4jResult] = () => {
      val getReferersForLinkedin = Cypher(CypherReader.getReferersForLinkedin(stream)).apply().map(row => row[Option[String]]("ip")).toList
      val neo4jResult = new Neo4jResult(getReferersForLinkedin)
      neo4jResult
    }
    getReferersForLinkedin
  }

  def getReferersForTwitter(stream: String): () => Neo4jResult = {

    val getReferersForTwitter: Function0[Neo4jResult] = () => {
      val getReferersForTwitter = Cypher(CypherReader.getReferersForTwitter(stream)).apply().map(row => row[Option[String]]("ip")).toList
      val neo4jResult = new Neo4jResult(getReferersForTwitter)
      neo4jResult
    }
    getReferersForTwitter
  }

  def getReferersForFacebook(stream: String): () => Neo4jResult = {

    val getReferersForFacebook: Function0[Neo4jResult] = () => {
      val getReferersForFacebook = Cypher(CypherReader.getReferersForFacebook(stream)).apply().map(row => row[Option[String]]("ip")).toList
      val neo4jResult = new Neo4jResult(getReferersForFacebook)
      neo4jResult
    }
    getReferersForFacebook
  }


  def getRoomJid(token: String): () => Neo4jResult = {

    val getRoomJid: Function0[Neo4jResult] = () => {
      val getRoomJid = Cypher(CypherReader.getRoomJid(token)).apply().map(row => row[Option[String]]("jid")).toList
      val neo4jResult = new Neo4jResult(getRoomJid)
      neo4jResult
    }
    getRoomJid
  }

  def getRoomJidForStream(stream: String): () => Neo4jResult = {

    val getRoomJidForStream: Function0[Neo4jResult] = () => {
      val getRoomJidForStream = Cypher(CypherReader.getRoomJidForStream(stream)).apply().map(row => row[Option[String]]("jid")).toList
      val neo4jResult = new Neo4jResult(getRoomJidForStream)
      neo4jResult
    }
    getRoomJidForStream
  }

  def getUserInformationUsingInviteId(inviteId: String): () => Neo4jResult = {

    val getUserInformationUsingInviteId: Function0[Neo4jResult] = () => {
      val getUserInformationUsingInviteId = Cypher(CypherReader.getUserInformationUsingInviteId(inviteId)).apply().map(row => (row[Option[String]]("email"), row[Option[String]]("firstName"), row[Option[String]]("lastName"), row[Option[String]]("domId"))).toList
      val neo4jResult = new Neo4jResult(getUserInformationUsingInviteId)
      neo4jResult
    }
    getUserInformationUsingInviteId

  }


}