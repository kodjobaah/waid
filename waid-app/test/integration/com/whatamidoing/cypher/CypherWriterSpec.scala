package integration.com.whatamidoing.cypher

import org.scalatest.FlatSpec
import com.whatamidoing.cypher.CypherReader
import org.scalatest.Matchers
import org.scalatest.BeforeAndAfter
import org.neo4j.cypher.javacompat.ExecutionEngine
import org.neo4j.graphdb.Transaction
import org.neo4j.tooling.GlobalGraphOperations
import com.whatamidoing.cypher.CypherWriter
import org.neo4j.graphdb.Node

class CypherWriterSpec extends FlatSpec with Neo4jTestDb with Matchers with BeforeAndAfter {

  "given the token" should "log the user out by setting the token to false" in {

    var result = getEngine.execute(CypherWriter.invalidateToken(testTokenToInvalidate))
    var res = ""
    val it = result.iterator()
    while (it.hasNext()) {
      val resp = it.next()
      res = resp.get("valid").asInstanceOf[String]

    }
    res should equal("false")
  }

  "given the token" should "set all tokens for the user to false" in {
    var result = getEngine.execute(CypherWriter.invalidateAuthenticationTokenForUser(testTokenToInvalidateOne))
    var res = List()
    val it = result.iterator()

    while (it.hasNext()) {
      {
        val resp = it.next()
        val keySet = resp.keySet()
        val keySetIterator = keySet.iterator()
        val tx = db.beginTx()
        while (keySetIterator.hasNext()) {
          val key = keySetIterator.next()
          val value = resp.get(key).asInstanceOf[Node]
          val valueKeys = value.getPropertyKeys()
          val valueKeysIterator = valueKeys.iterator()
          while (valueKeysIterator.hasNext()) {
            val valueKey = valueKeysIterator.next()
            val actualValue = value.getProperty(valueKey)

            if (valueKey.equalsIgnoreCase("valid")) {
              actualValue should equal("false")
            }
          }

        }
        tx.close()

      }

    }

  }

}