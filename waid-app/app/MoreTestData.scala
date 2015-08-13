import com.whatamidoing.cypher.CypherReaderFunction._
import com.whatamidoing.cypher.CypherWriterFunction._

/**
 * Created with IntelliJ IDEA.
 * User: kodjobaah
 * Date: 01/11/2013
 * Time: 12:20
 * To change this template use File | Settings | File Templates.
 */
object MoreTestData extends App {


  val stream = "stream-id-0"
  val r = closeStream(stream)
  val s = r()
  println("done")

}
