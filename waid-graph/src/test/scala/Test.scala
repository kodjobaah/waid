import com.waid.redis.RedisReadOperations
import com.waid.redis.model.UserStreamNode
import com.waid.redis.utils.RedisUtils

/**
 * Created by kodjobaah on 04/08/2015.
 */
object Test extends App{

  //var result = RedisUtils.getUserNodeFromUserToken("node:token:_1_:1")
  //var result = RedisUtils.getStreamNodeFromUserToken("node:token:_1_:4")
  //var userStreamnNode = UserStreamNode(None,"node:token:_1_:4", Option(20L),Option(Map()))
  //println(userStreamnNode.genId)

  println(RedisUtils.getUserIdFromUserTokenId("node:token:_1_:4"))
}
