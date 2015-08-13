package integration.com.whatamidoing.cypher

import org.scalatest.FlatSpec
import com.whatamidoing.cypher.CypherReader
import org.scalatest.Matchers
import org.scalatest.BeforeAndAfter
import org.neo4j.cypher.javacompat.ExecutionEngine
import org.neo4j.graphdb.Transaction
import org.neo4j.tooling.GlobalGraphOperations
import com.whatamidoing.cypher.CypherWriter
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner


@RunWith(classOf[JUnitRunner])
class CypherReaderSpec extends FlatSpec with Neo4jTestDb with Matchers with BeforeAndAfter {
  
  "Given a valid user with no active token" should "return nothing" in {
      var result = getEngine.execute(CypherReader.getTokenForUser(testUserWithInactiveToken))
	  result.columns().size should equal(2)
	  var token = ""
	  var status = ""
	  val it = result.iterator()
	  while(it.hasNext()) {
	    val res = it.next()
	     token = res.get("token").asInstanceOf[String]
	     status = res.get("status").asInstanceOf[String]
	  }
	  
	  token.length should equal(0) 
	  status.length should equal(0)
  }
  "Given a valid user" should "return the token" in {
	  var result = getEngine.execute(CypherReader.getTokenForUser(testUser))
	  result.columns().size should equal(2)
	  var token = ""
	  var status = ""
	  val it = result.iterator()
	  while(it.hasNext()) {
	    val res = it.next()
	     token = res.get("token").asInstanceOf[String]
	     status = res.get("status").asInstanceOf[String]
	  }
	  
	  token should equal (testToken)
	  status should equal ("true")
  }
  
  "Given a valid token" should "return the token since its valid" in {
    var result = getEngine.execute(CypherReader.getValidToken(testToken))
	  result.columns().size should equal(1)
	  var token = ""
	  val it = result.iterator()
	  while(it.hasNext()) {
	    val res = it.next()
	     token = res.get("token").asInstanceOf[String]
	  }
	  
	  token should equal (testToken)
  }
  
   "Given an valid token" should "return nothing" in {
    var result = getEngine.execute(CypherReader.getValidToken("test-invalid-token"))
    result.iterator().hasNext() should equal (false)
  }
   
   "Given a active stream" should "turn inactive when the stream is close" in {
     var result = getEngine.execute(CypherWriter.closeStream(testMakeInactiveStream))
     var resp = ""
     val it = result.iterator()
     while(it.hasNext()) {
        val res = it.next()
	     resp = res.get("state").asInstanceOf[String]
     }     
     resp should equal ("inactive")
   
   }
   
   "given the token" should "return the name of the active stream " in {
     var result = getEngine.execute(CypherReader.findActiveStreamForToken(testToken))
     var res = ""
     val it = result.iterator()
     while(it.hasNext()) {
       val resp = it.next()
       res = resp.get("name").asInstanceOf[String]
       
     }
     res should equal(testStream)
        
   }
   
   "given the invited id for a non active stream" should "not return the stream name" in {
     
       var result = getEngine.execute(CypherReader.findStreamForInvitedId(testNonActiveStreamInvitedId))
	   var res = ""
       val it = result.iterator()
       while(it.hasNext()) {
        val resp = it.next()
        res = resp.get("name").asInstanceOf[String]
       
      }
	 res should not equal(testStreamNonActive)
     
   }
   
   "given the invited id" should "return the name of the stream" in {
	   var result = getEngine.execute(CypherReader.findStreamForInvitedId(testInvitedId))
	   var res = ""
       val it = result.iterator()
       while(it.hasNext()) {
        val resp = it.next()
        res = resp.get("name").asInstanceOf[String]
       
      }
	 res should equal(testStream)    
     
   }
   
   "given the token" should "log the user out by setting the token to false" in {
     
       var result = getEngine.execute(CypherWriter.invalidateToken(testTokenToInvalidate))
	   var res = ""
       val it = result.iterator()
       while(it.hasNext()) {
        val resp = it.next()
        res = resp.get("valid").asInstanceOf[String]
       
      }
	 res should equal("false")  
   }
   
   "given the user" should "return all the invited uses" in {
       var result = getEngine.execute(CypherReader.findAllInvites(testUserFindAllInvitesToken))

       var res = new scala.collection.mutable.ListBuffer[String]
       val it = result.iterator()
       while(it.hasNext()) {
        val resp = it.next()
        res += resp.get("email").asInstanceOf[String]
       
      }
       res.length should equal(3)
       res should contain (testUserFindAllInvitesInvited1)
       res should contain (testUserFindAllInvitesInvited2)
       res should contain (testUserFindAllInvitesInvited3)
        
   }

  "given the token " should "return all the streams" in {

    var result = getEngine.execute(CypherReader.findAllStreamsForDay(testTokenForCollectingUserInfo,0,5,1,"asc"))

    println(result.dumpToString())

  }

  "given a token that is associated with an active stream" should "return all users that accepted the invitation" in {

  }

  "given a token that is associated with an active stream" should "return all users that have been inivted but have not acc" in {

  }

}