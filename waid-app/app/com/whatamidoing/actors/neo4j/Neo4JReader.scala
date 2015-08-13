package com.whatamidoing.actors.neo4j

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.Props

import models.Messages._

class Neo4JReader  extends Actor with ActorLogging{
	
	override def receive: Receive = {  
      case PerformReadOperation(operation) => {
        import models.Neo4jResult
    	  import akka.pattern.{pipe}
    	  var res: Neo4jResult = operation()
    	  sender ! ReadOperationResult(res) 
    	  
         }
	}
  
}

object Neo4JReader {
  
  def props() = Props(new Neo4JReader())

}