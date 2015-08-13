package com.waid.redis.model

import com.waid.redis.KeyPrefixGenerator
import com.waid.redis.utils.RedisUtils

/**
 * Created by kodjobaah on 16/07/2015.
 */


abstract class Node {
  val prefix: String
  def id: Option[Long]
  var attributes: Option[Map[String,String]]
}

case class UserNode(prefix:String = KeyPrefixGenerator.UserNodePrefix,var id: Option[Long], var attributes: Option[Map[String,String]]) extends Node {
  println( s"""${prefix}${id.getOrElse(-1L)}""")
  def genId = s"""${prefix}${id.getOrElse(-1L)}"""
}
case class UserTokenNode(userNode: UserNode, prefix:String,var id: Option[Long], var attributes: Option[Map[String,String]]) extends Node {
  def genId = s"""${prefix}${userNode.id.getOrElse(-1)}_:${id.getOrElse(-1)}"""
}

case class UserStreamNode(userTokenNode: Option[UserTokenNode], prefix:String, var id: Option[Long], var attributes: Option[Map[String,String]]) extends Node {
  def genId: String = {

    if (userTokenNode != None) {
      var toks = userTokenNode.get.genId.split(":")
      var userId = toks(2)
      var tokenId = toks(3)

      KeyPrefixGenerator.UserStreamNodePrefix + userId + ":_" + tokenId + "_"
      s"""${prefix}${userId}:_${tokenId}_:${id.getOrElse(-1l)}"""
    } else {
        RedisUtils.getStreamNodeFromUserToken(prefix)+":"+id.getOrElse(-1L)
    }
  }
}
case class UserInviteNode(userStreamNode: UserStreamNode, prefix:String, var id: Option[Long], var attributes: Option[Map[String,String]]) extends Node {
  def genId: Unit = s"""${prefix}"""
}
case class UserInviteeNode(userInviteNode: UserInviteNode,prefix:String, var id: Option[Long], var attributes: Option[Map[String,String]]) extends Node {
  def genId: Unit = s"""${prefix}"""
}

