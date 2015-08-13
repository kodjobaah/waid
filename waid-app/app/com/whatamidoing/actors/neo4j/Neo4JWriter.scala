package com.whatamidoing.actors.neo4j

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.Props

import models.Messages._

class Neo4JWriter  extends Actor with ActorLogging{
	
	override def receive: Receive = {  
      case PerformOperation(operation) => {
          import models.Neo4jResult
    	  import akka.pattern.{pipe}
    	  var res: Neo4jResult = operation()
    	  sender ! WriteOperationResult(res) 
    	  
         }
	}
  
}
object Neo4JWriter {
  
  def props() = Props(new Neo4JWriter())


}