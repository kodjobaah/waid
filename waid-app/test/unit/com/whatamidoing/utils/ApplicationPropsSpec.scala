package unit.com.whatamidoing.utils

import org.scalatest._
import org.scalatest.matchers._


class ApplicationPropsSpec extends FlatSpec  {
  
  import com.whatamidoing.utils.ApplicationProps
  
  "Given neo4jServer name" should "get the ip addess" in {
	  assert(ApplicationProps.neo4jServer  == "localhost")
  }
}