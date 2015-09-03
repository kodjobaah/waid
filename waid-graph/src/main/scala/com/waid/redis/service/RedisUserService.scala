package com.waid.redis.service

import java.text.{DecimalFormat, NumberFormat}

import com.waid.redis.model.{UserStreamNode, Node, UserTokenNode, UserNode}
import com.waid.redis.utils.RedisUtils
import com.waid.redis.{KeyPrefixGenerator, RedisDataStore, RedisReadOperations}

/**
 * Created by kodjobaah on 16/07/2015.
 */
object RedisUserService {



  /** ***************************************************************************************************************
    * ***************************************************************************************************************
    * ***************************************************************************************************************
                          Stream Operations
    * ****************************************************************************************************************
    * ******************************************************************************************************************/



  def getStoredStream(token: String, streamToken: String): Option[UserStreamNode] =  {


      var res: Option[UserStreamNode] = None

      val userNode = checkIfTokenIsValid(token)

      if (userNode != None) {
        val id = userNode.get.genId.split(":")(2)
        val key = KeyPrefixGenerator.StoreStreams + id.substring(id.indexOf("_") + 1)
        val streamNodeId = RedisReadOperations.getElementAttribute(key, streamToken)

        var userTokenNode = RedisReadOperations.getUserTokenNodeId(token)
        res = RedisReadOperations.populateStreamNode(streamNodeId.get,userTokenNode.get)

      }
    res
  }
  def addStreamToUsersList(user: Option[UserNode], stream: Option[UserStreamNode]): Unit = {

    val id = user.get.genId.split(":")(2)
    val userListKey = KeyPrefixGenerator.LookupAllStreams+id
    val timestamp: Long = java.lang.Double.valueOf(System.currentTimeMillis / 1000).longValue()
    val value = stream.get.genId
    RedisDataStore.addZValue(userListKey,timestamp,value)

  }

  /*
   * Used for test purposes
   */
  def getAllValidStreams(): Option[Map[String,String]] = {
      RedisReadOperations.getAllValidStreams
  }
  /*
   * This will only return the segment location if the stream is active
   */
  def getSegmentLocationOfStream(streamToken: String): Option[String] = {
    var segLocation:Option[String] = None
    val streamNodeId = getStreamNodeId(streamToken)

    for(sn <- streamNodeId)
      segLocation = RedisReadOperations.getElementAttribute(sn,KeyPrefixGenerator.SegmentLocation)

    segLocation
  }

  def addSegmentLocationToStream(streamNodeId: String, segmentLocation: String) {

        var attributes = Map.empty[String,String]
        attributes += KeyPrefixGenerator.SegmentLocation -> segmentLocation
        RedisDataStore.addEleemnt(streamNodeId,attributes);
  }

  /*
   * This will only update the playlist count if the stream is active
   */
  def updateStreamPlayListCount(streamToken: String, playListCount: Int) = {
      val streamNodeId = getStreamNodeId(streamToken)
       for(sn <- streamNodeId) {
            var attributes = Map.empty[String,String]
            attributes += KeyPrefixGenerator.PlayListCount -> playListCount.toString
            RedisDataStore.addEleemnt(sn,attributes)
       }

  }

  /*
   * Gets the active stream node id
   */
  def getStreamNodeId(streamToken: String): Option[String] = {
       RedisReadOperations.getElementAttribute(KeyPrefixGenerator.LookupValidStreams, streamToken)
  }

  def getStreamPlayListCount(streamToken: String): Option[Int] = {

      var streamPlayList: Option[Int] = None
      val streamNodeId = getStreamNodeId(streamToken)

      for(sId <- streamNodeId)  {

          val playListCount= RedisReadOperations.getElementAttribute(sId,KeyPrefixGenerator.PlayListCount)
          for(pl <- playListCount)
            streamPlayList = Option(pl.toInt)

      }

      streamPlayList
  }

  def getStreamSequenceNumber(streamId: String, sequenceReference: String): Option[Int] = {

    var sequenceNumber: Option[Int] = None
    val key = KeyPrefixGenerator.LookupLiveStreams + ":" + streamId
    var seqNumber: Option[String] = RedisReadOperations.getElementAttribute(key, sequenceReference)

    for (sn <- seqNumber)
      sequenceNumber = Some(sn.toInt)

    sequenceNumber

  }

  def addStreamSequenceNumber(streamId: String, sequenceReference: String, sequenceNumber: String) {

    var key = KeyPrefixGenerator.LookupLiveStreams + ":" + streamId
    var attributes = Map.empty[String, String]
    attributes += sequenceReference -> sequenceNumber

    RedisDataStore.addEleemnt(key, attributes)

  }

  def removeValidStreamUsingEmail(email: String) = {
    RedisDataStore.removeValidStreams(email)
  }

  def createStream(authToken: String): Option[UserStreamNode] = {

    var userNode = RedisReadOperations.getUserForAuthToken(authToken)
    var userStreamNode: Option[UserStreamNode] = None
    var userTokenNode = RedisReadOperations.getUserTokenNodeId(authToken)
    for (un <- userNode; userTokenNodeId <- userTokenNode) {

      var email = un.attributes get KeyPrefixGenerator.Email
      userStreamNode = Some(RedisDataStore.createStreamForUser(userTokenNodeId, email))

    }
    userStreamNode
  }

  def getStreamFromStore(streamToken: String, userId: String):Option[UserStreamNode] = {
    var userStreamNode = None

    var streamStorekey = KeyPrefixGenerator.StoreStreams+userId
     val streamNode  = RedisReadOperations.getElementAttribute(streamStorekey,streamToken)
    userStreamNode
  }

  /** ***************************************************************************************************************
    * ***************************************************************************************************************
    * ***************************************************************************************************************
                           Operations For User Details
    * ****************************************************************************************************************
    * ******************************************************************************************************************/

  def removeForgottenPassword(email: Option[String]) = {

    for (e <- email) {
      RedisDataStore.removeForgottenPassword(e)
    }
  }

  def removePreviousValidLoginDetails(email: String) = {

    var authToken = RedisReadOperations.getUserAuthTokenId(email)

    for (at <- authToken) {
      RedisDataStore.removeElement(KeyPrefixGenerator.LookupValidLogins, at)
      RedisDataStore.removeElement(KeyPrefixGenerator.LookupValidLoginsEmail, email)

    }
  }

  def checkIfTokenIsValid(token: String): Option[UserNode] = {
    RedisReadOperations.getUserForAuthToken(token)
  }

  def checkForgottonPassword(em: String, changePasswordId: String): Option[UserNode] = {
    val token = RedisReadOperations.getForgottenPasswordToken(em)
    var user: Option[UserNode] = None
    for (tk <- token) {

      if (tk.equals(changePasswordId))
        user = RedisReadOperations.getUser(em)
    }
    user
  }

  def setForgottonPassword(em: String): String = {

    val token = RedisUtils.getUUID().toString
    RedisDataStore.addForgottonPassword(em, token)
    token
  }

  def removeCurrentToken(em: String) = {
    RedisDataStore.removeCurrentToken(em)

  }

  def createToken(userNode: Option[UserNode]): UserTokenNode = {

    var userToken: Option[UserTokenNode] = None
    for (user <- userNode) {
      userToken = Some(RedisDataStore.createTokenForUser(user))
      var email = user.attributes get KeyPrefixGenerator.Email
      RedisDataStore.addAuthenticationToken(userToken.get, email)
    }
    userToken.get
  }

  def removeRegistrationDetails(registrationId: Option[String]) = {
    RedisDataStore.removeRegistration(registrationId.get)

  }

  def getUserFromRegistration(regToken: Option[String]): Option[UserNode] = {
    RedisReadOperations.getUserUsingRegistrationToken(regToken.get)
  }


  def findUser(email: String): Option[UserNode] = {
    RedisReadOperations.getUser(email)
  }

  def createNewUser(em: String, fn: String, ln: String, p: String): UserNode = {
    var userNode = RedisModelService.createUser(em, Some(fn), Some(ln), Some(p))
    userNode = RedisDataStore.addUser(userNode)
    RedisDataStore.createUserLookupAfterRegistration(userNode)
    userNode
  }

  def getAttributeFromNode(node: Node, att: String): Option[String] = {
    val atts = node.attributes.get
    val value = atts.get(att)
    atts.get(att)
  }
}
