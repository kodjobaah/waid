package com.waid.redis

import com.redis.RedisClientPool
import com.redis.serialization.Parse
import com.waid.redis.model.{UserStreamNode, UserTokenNode, UserNode}
import com.waid.redis.service.RedisModelService
import com.waid.redis.utils.{RedisUtils, ApplicationProps}

import scala.collection.mutable.ListBuffer

/**
 * Created by kodjobaah on 16/07/2015.
 */
object RedisDataStore {
  val clients = new RedisClientPool(ApplicationProps.redisServer, ApplicationProps.redisPort)


  def addEleemnt(key: String, attributes: Map[String,String]) = {

      clients.withClient {
          client =>
            client.hmset(key,attributes)
        }
  }


  def removeElement(prefix: String, element: String) = {

    clients.withClient {
      client =>
        client.hdel(prefix,element)
    }

  }

  def removeForgottenPassword(email: String) = {
    clients.withClient {
      client =>
        client.hdel(KeyPrefixGenerator.LookupLostPassword,email)
    }

  }

  def addForgottonPassword(em: String, token: String) = {
    clients.withClient {
       client =>
         var attributes: ListBuffer[(String,String)] = ListBuffer()
         attributes += em -> token
         client.hmset(KeyPrefixGenerator.LookupLostPassword,attributes.toList)
    }

  }

  def removeCurrentToken(em: String) = {
    clients.withClient {
      client =>
       val cur = client.hget(KeyPrefixGenerator.LookupCurrentToken,em)
        for(token <- cur) {
           client.hdel(KeyPrefixGenerator.LookupValidTokens,token)
        }
    }
  }

  def removeValidStreams(em: String) = {
    clients.withClient {
      client =>
        val cur = client.hget(KeyPrefixGenerator.LookupValidStreamsEmail,em)
        for(token <- cur) {
          client.hdel(KeyPrefixGenerator.LookupValidStreams,token)
          client.hdel(KeyPrefixGenerator.LookupValidStreamsEmail,em)
        }
    }
  }

  import com.redis.serialization._
  import Parse.Implicits._


  def removeRegistration(registrationToken: String) = {
    clients.withClient {
      client =>
        client.hdel(KeyPrefixGenerator.LookupRegistration,registrationToken)
    }
  }

  def addUser(userNode: UserNode): UserNode = {

    /*
     * TODO: Check for success or failure
     */

    clients.withClient {
      client => {

        var count = client.incr(KeyPrefixGenerator.UserCounter)
        userNode.id = count
      }
    }

    clients.withClient {
      client => {

        val res = client.hmset(userNode.genId, userNode.attributes.getOrElse(Map()))
      }
    }
    userNode
  }


  def createStreamForUser(userTokenId: String, email: String) : UserStreamNode = {
    var userStreamNode = RedisModelService.createUserStreamNode(userTokenId)
    clients.withClient(
        client =>  {

          var userId = RedisUtils.getUserIdFromUserTokenId(userTokenId)
          var streamTokenId = incrCounter(KeyPrefixGenerator.StreamCounter+userId.toString)
          userStreamNode.id = streamTokenId
          client.hmset(userStreamNode.genId,userStreamNode.attributes.getOrElse(Map()))
          var token = userStreamNode.attributes get KeyPrefixGenerator.Token
          var lookupStream = Map(email -> token)
          var lookupStreamToken = Map(token -> userStreamNode.genId)
          client.hmset(KeyPrefixGenerator.LookupValidStreams, lookupStreamToken)
          client.hmset(KeyPrefixGenerator.LookupValidStreamsEmail, lookupStream)
        }
    )
    userStreamNode
  }


  def incrCounter(counter: String): Option[Long] = {
    var count: Option[Long] = None
    clients.withClient {
      client => {
        count = client.incr( s"""${counter}""")
      }
    }
    count
  }

  def createTokenForUser(userNode: UserNode) : UserTokenNode = {

    val userToken = RedisModelService.createUserToken(userNode)
    clients.withClient {
      client => {
        val count = client.incr( s"""${KeyPrefixGenerator.TokenCounter}${userNode.id.getOrElse(-1L)}""")
        userToken.id = count
        client.hmset(userToken.genId, userToken.attributes.getOrElse(Map()))

      }
    }
    userToken
  }

  def createUserLookupAfterRegistration(userNode: UserNode) {

    clients.withClient {
      client => {
        val attsUser = userNode.attributes.getOrElse(Map())
        val email = attsUser.get(KeyPrefixGenerator.Email)

        for (em <- email) {
          val atts = Map(em -> userNode.genId)
          client.hmset(KeyPrefixGenerator.LookupUser, atts)
        }

        val registrationToken = attsUser.get(KeyPrefixGenerator.RegistrationToken)
        for (token <- registrationToken) {
          val atts = Map(token -> userNode.genId)
          client.hmset(KeyPrefixGenerator.LookupRegistration, atts)
        }

      }
    }
  }

  def addAuthenticationToken(userToken: UserTokenNode, email:String) {

      clients.withClient { client =>
      val attsToken = userToken.attributes.getOrElse(Map())
        val token = attsToken.get(KeyPrefixGenerator.Token)
        for (tk <- token) {
          val valid = Map(token.get -> userToken.genId)
          val validEmail = Map(email -> token.get)
          client.hmset(KeyPrefixGenerator.LookupValidLogins, valid)
          client.hmset(KeyPrefixGenerator.LookupValidLoginsEmail, validEmail)
        }
      }
  }
}
