package com.waid.redis.service

import com.waid.redis.{KeyPrefixGenerator, RedisReadOperations}
import com.waid.redis.model.{UserStreamNode, UserNode}

/**
 * Created by kodjobaah on 31/08/2015.
 */
object RedisUserAdminService {

  def getStreamsForUser(un: UserNode, userTokenNodeId: String, from: Long, to:Long ):List[UserStreamNode]= {

    var results = List[UserStreamNode]()

    val id = un.genId.split(":")(2)
    val key = KeyPrefixGenerator.LookupAllStreams+id

    val res = RedisReadOperations.findRangeByScore(key,from,to)

    if (res != None) {

      val listOfRanges = res.get
      for((userStreamNodeId) <- listOfRanges) {
        val streamNode = RedisReadOperations.populateStreamNode(userStreamNodeId,userTokenNodeId)
        results = (streamNode.get :: results.reverse).reverse
      }
    }
    results
  }
}
