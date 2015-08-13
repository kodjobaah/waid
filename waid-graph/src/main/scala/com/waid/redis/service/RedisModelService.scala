package com.waid.redis.service

import com.waid.redis.KeyPrefixGenerator
import com.waid.redis.model.{UserStreamNode, UserTokenNode, UserNode}
import com.waid.redis.utils.RedisUtils
import org.mindrot.jbcrypt.BCrypt

import scala.collection.mutable.ListBuffer

/**
 * Created by kodjobaah on 16/07/2015.
 */
object RedisModelService {

  def createUserStreamNode(userTokenNodeId: String): UserStreamNode = {
    val attributes: scala.collection.mutable.Map[String,String] = scala.collection.mutable.Map()

    val epoch = RedisUtils.getEpochTime
    attributes += KeyPrefixGenerator.CreatedDate -> epoch.toString
    attributes += KeyPrefixGenerator.Token -> RedisUtils.getUUID().toString
    attributes += KeyPrefixGenerator.PlayListCount -> 0.toString

    UserStreamNode(None,userTokenNodeId,None,Some(attributes.toMap))

  }


  def createUser(email: String,
                 fName:Option[String],
                 lName:Option[String],
                  password:Option[String]): UserNode = {

    var attributes: ListBuffer[(String,String)] = ListBuffer()

    for(name <- fName)
        attributes += KeyPrefixGenerator.FirstName -> name

    for(name <- lName)
      attributes += KeyPrefixGenerator.LastName -> name

    for(pw <- password) {
      val pw_hash = BCrypt.hashpw(pw, BCrypt.gensalt())
      attributes += KeyPrefixGenerator.Password -> pw_hash
    }

    attributes += KeyPrefixGenerator.Email -> email

    attributes += KeyPrefixGenerator.RegistrationToken -> RedisUtils.getUUID().toString

    val epoch = RedisUtils.getEpochTime
    attributes += KeyPrefixGenerator.CreatedDate -> epoch.toString

    UserNode(KeyPrefixGenerator.UserNodePrefix,None,Some(attributes.toMap))

  }

  def createUserToken(userNode: UserNode): UserTokenNode = {

    val attributes: scala.collection.mutable.Map[String,String] = scala.collection.mutable.Map()
    attributes += KeyPrefixGenerator.Token -> RedisUtils.getUUID().toString

    val epoch = RedisUtils.getEpochTime
    attributes += KeyPrefixGenerator.CreatedDate -> epoch.toString
    attributes += KeyPrefixGenerator.CreatedDate -> epoch.toString

    UserTokenNode(userNode,KeyPrefixGenerator.UserTokenNodePrefix,None,Some(attributes.toMap))
  }
}
