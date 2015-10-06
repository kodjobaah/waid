package com.waid.redis

import com.redis.RedisClient
import com.waid.redis.model.{UserStreamNode, UserNode}
import com.waid.redis.utils.RedisUtils

/**
 * Created by kodjobaah on 16/07/2015.
 */
object RedisReadOperations {


  import RedisDataStore.clients


  def getValidStreamUsingEmail(em: String):Option[String]= {
    var streamId: Option[String] = None
    clients.withClient {
      client =>
        streamId = client.hget(KeyPrefixGenerator.LookupValidStreamsEmail, em)
    }
    streamId
  }

  def populateStreamNode(userStreamNodeId: String, userTokenNodeId: String): Option[UserStreamNode] = {
    var res: Option[UserStreamNode] = None
    clients.withClient {
      client => {
        val atts = client.hgetall(userStreamNodeId)
        for(mapOfAtts <- atts) {
          if (!mapOfAtts.isEmpty)
            res = Option(UserStreamNode(None, userTokenNodeId, None, atts))
        }
      }
    }
    res
  }

  def findRangeByScore(key: String, from: Long, to: Long) : Option[List[String]] = {
    var res = Option(List[String]())
    clients.withClient {
      client => {
        res = client.zrangebyscore(key,from.toDouble,true,to.toDouble,true,None)
      }
    }

    res
  }

  def getStreamNodeIdFromValidStreams(streamToken: String) :Option[String] = {

    var res: Option[String] = None
    clients.withClient {
      client =>
        res = client.hget(KeyPrefixGenerator.LookupValidStreams,streamToken)
    }
    res
  }

  def getAllValidStreams(): Option[Map[String,String]] = {

    var result: Option[Map[String,String]] = None

    clients.withClient {
      client => {
        result = client.hgetall(KeyPrefixGenerator.LookupValidStreams)
      }
    }

    result
  }
  def getElementAttribute(key: String, element: String): Option[String] = {
    var attribute:Option[String]= None
      clients.withClient{
          client => {
              attribute = client.hget(key,element)
          }
      }
     attribute

  }

  def getUserAuthTokenId(email: String): Option[String] = {
    var node: Option[String] = None
    clients.withClient {
      client =>
        node = client.hget(KeyPrefixGenerator.LookupValidLoginsEmail,email)
    }
    node
  }

  def getUserTokenNodeId(token: String): Option[String] = {
    var node: Option[String] = None
    clients.withClient {
      client =>
        node = client.hget(KeyPrefixGenerator.LookupValidLogins,token)
    }
    node
  }

  def getUserForAuthToken(token: String): Option[UserNode] = {
    var user:Option[UserNode] = None
    clients.withClient {
      client =>
          var userTokenId = client.hget(KeyPrefixGenerator.LookupValidLogins,token)
          for(tokenId <- userTokenId) {
            var userNodeId = Option(RedisUtils.getUserNodeFromUserToken(tokenId))
            user = populateUserNode(client,userNodeId)
          }
    }
    user
  }

  def getUserUsingRegistrationToken(regToken: String): Option[UserNode]= {
    var user:Option[UserNode] = None
    clients.withClient{
      client =>
        val res = client.hget(KeyPrefixGenerator.LookupRegistration,regToken)
        populateUserNode(client,res)
    }
  }

  def getForgottenPasswordToken(em: String) : Option[String] = {

    var token:Option[String] = None
    clients.withClient {
      client =>
        token = client.hget(KeyPrefixGenerator.LookupLostPassword,em)
    }
    token
  }

  def getUser(email: String): Option[UserNode] = {
    var user:Option[UserNode] = None
    clients.withClient{
      client =>
        val res= client.hget(KeyPrefixGenerator.LookupUser,email)
        user = populateUserNode(client,res)
    }
    user
  }

  def populateUserNode(userNode: String): Option[UserNode] = {
    var un: Option[UserNode] = None
    clients.withClient{
      client =>
        val userNodeMap= client.hgetall(userNode)
        val id = Some(userNode.split(":_")(1).toLong)
        un = Some(UserNode(KeyPrefixGenerator.UserNodePrefix,id,userNodeMap))
    }
    un
  }

  def getCounterValue(counter: String):Option[String] = {
    var res: Option[String] = None
    clients.withClient{
      client =>
       res = client.get(counter)
    }
    res
  }

  private def populateUserNode(client: RedisClient, userNode: Option[String]):Option[UserNode] =  {
    var user:Option[UserNode] = None
    for(un <- userNode) {
        val userNodeMap = client.hgetall(un)
        val tokens = un.split(":_")
        val id = Some(tokens(1).toLong)
        user = Some(UserNode(KeyPrefixGenerator.UserNodePrefix,id,userNodeMap))
    }
    user
  }
}
