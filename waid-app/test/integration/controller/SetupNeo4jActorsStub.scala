package integration.controller

import akka.actor.Actor
import org.scalatest.Suite
import org.scalatest.BeforeAndAfterEach
import org.mindrot.jbcrypt.BCrypt
import models.Neo4jResult
import play.api.Logger
import models.Messages._
import akka.actor.actorRef2Scala
import akka.testkit.TestActorRef
import com.whatamidoing.utils.ActorUtils

trait SetupNeo4jActorsStub extends BeforeAndAfterEach  { this: Suite =>

  val email = Option("testEmail@hotmail.com")
  val password = Option("testpassword")
  val firstName = Option("firstName")
  val lastName = Option("lastName")
  val newToken = "newToken"

  var currentTest = "NOTHING"

  override def beforeEach() {

    import akka.testkit.TestActorRef
    var numberOfTimesCalled = 0
    import com.whatamidoing.actors.neo4j.Neo4JReader._
    import com.whatamidoing.actors.neo4j.Neo4JWriter._
    
    implicit var actorSystem = akka.actor.ActorSystem("WhatAmIdoingControllerSpec")
    
    
    val neo4jWriter = TestActorRef(new Actor {
    	def receive = {
    	   case PerformOperation(operation) => {
    	  
    	    var result = List(("true"))
            var res = Neo4jResult(result)
    	    sender ! WriteOperationResult(res) 
    	   }
    	       
    	}
    })
    
    
    val neo4jReader = TestActorRef(new Actor {
      def receive = {
        case PerformReadOperation(operation) => {

          var res: Neo4jResult = Neo4jResult(List(""))
          Logger("SetupNeo4jActorsStub").info("------------operation received:"+operation)

          if (currentTest.equalsIgnoreCase("invitedToViewInvalidEmail")) {
            if (numberOfTimesCalled == 0) {
              var result = List()
              res = Neo4jResult(result)
              numberOfTimesCalled = numberOfTimesCalled + 1
            }
          } else if (currentTest.equalsIgnoreCase("findAllInvited")) {
             if (numberOfTimesCalled == 0) {
              var result = List("authenticationid")
              res = Neo4jResult(result)
              numberOfTimesCalled = numberOfTimesCalled + 1
            }
          } else if (currentTest.equalsIgnoreCase("registeredLoginWithOutSupplyingPassword")) {
             if (numberOfTimesCalled == 0) {
              var result = List("invite,invite2")
              res = Neo4jResult(result)
              numberOfTimesCalled = numberOfTimesCalled + 1
            }
          } else if (currentTest.equalsIgnoreCase("whatAmIdoingViewPageInvitedIdDoesNotExist")) {
             if (numberOfTimesCalled == 0) {
              var result = List()
              res = Neo4jResult(result)
              numberOfTimesCalled = numberOfTimesCalled + 1
            }
          } else if (currentTest.equalsIgnoreCase("whatAmIdoingViewPage")) {
             if (numberOfTimesCalled == 0) {
              var result = List("testPageToView.flv")
              res = Neo4jResult(result)
              numberOfTimesCalled = numberOfTimesCalled + 1
            }
          } else if (currentTest.equalsIgnoreCase("invitedToViewTokenNotValid")) {
        	  if (numberOfTimesCalled == 0) {
              var result = List()
              res = Neo4jResult(result)
              numberOfTimesCalled = numberOfTimesCalled + 1
            } 
          }else if (currentTest.equalsIgnoreCase("invitedToViewStreamButDoesNotExist")) {
        	  if (numberOfTimesCalled == 0) {
              var result = List("ValidToken")
              res = Neo4jResult(result)
              numberOfTimesCalled = numberOfTimesCalled + 1
            } else if (numberOfTimesCalled == 1) {
              var result = List((""))
              res = Neo4jResult(result)
              numberOfTimesCalled = numberOfTimesCalled + 1
            } 
          }else if (currentTest.equalsIgnoreCase("invitedToJoinAndRegistered")) {
        	  if (numberOfTimesCalled == 0) {
              var result = List("ValidToken")
              res = Neo4jResult(result)
              numberOfTimesCalled = numberOfTimesCalled + 1
            } else if (numberOfTimesCalled == 1) {
              var result = List(("stream name"))
              res = Neo4jResult(result)
              numberOfTimesCalled = numberOfTimesCalled + 1
            } else if (numberOfTimesCalled == 2) {
              var result = List((""))
              res = Neo4jResult(result)
              numberOfTimesCalled = numberOfTimesCalled + 1
            }
          } else if (currentTest.equalsIgnoreCase("registeredLoginWithValidPasswordInvalidToken")) {
             if (numberOfTimesCalled == 0) {
              val pw_hash = BCrypt.hashpw(password.get, BCrypt.gensalt())
              var result = List(pw_hash)
              res = Neo4jResult(result)
              numberOfTimesCalled = numberOfTimesCalled + 1
            } else if (numberOfTimesCalled == 1) {
              var result = List(("test-token", "false"))
              res = Neo4jResult(result)
              numberOfTimesCalled = numberOfTimesCalled + 1
            } 
            
          } else if (currentTest.equalsIgnoreCase("registeredLoginWithInvalidPassword")) {
            if (numberOfTimesCalled == 0) {
              var result = List("invalid hash")
              res = Neo4jResult(result)
              numberOfTimesCalled = numberOfTimesCalled + 1
            } 
            
          } else if (currentTest.equalsIgnoreCase("registeredWithValidPasswordAndValidToken")) {
          
            if (numberOfTimesCalled == 0) {
              val pw_hash = BCrypt.hashpw(password.get, BCrypt.gensalt())
              var result = List(pw_hash)
              res = Neo4jResult(result)
              numberOfTimesCalled = numberOfTimesCalled + 1
            } else if (numberOfTimesCalled == 1) {
              var result = List(("test-token", "true"))
              res = Neo4jResult(result)
              numberOfTimesCalled = numberOfTimesCalled + 1
            }
            
          } else if (currentTest.equalsIgnoreCase("registerLoginNotRegisteredButInvalidToken")) {
          
            if (numberOfTimesCalled == 0) {
            	var result = List()
            	res = Neo4jResult(result)
            	numberOfTimesCalled = numberOfTimesCalled + 1
            } else if (numberOfTimesCalled == 1) {
              var result = List(("test-token", "false"))
              res = Neo4jResult(result)
              numberOfTimesCalled = numberOfTimesCalled + 1
            }
            
          } else if (currentTest.equalsIgnoreCase("registerLoginNotRegistered")) {
            if (numberOfTimesCalled == 0) {
            	var result = List()
            	res = Neo4jResult(result)
            	numberOfTimesCalled = numberOfTimesCalled + 1
            } else if (numberOfTimesCalled == 1) {
              var result = List(("test-token", "true"))
              res = Neo4jResult(result)
              numberOfTimesCalled = numberOfTimesCalled + 1
            }
            
          } else {
            if (numberOfTimesCalled == 0) {
              val pw_hash = BCrypt.hashpw(password.get, BCrypt.gensalt())
              var result = List(pw_hash)
              res = Neo4jResult(result)
              numberOfTimesCalled = numberOfTimesCalled + 1

            } else if (numberOfTimesCalled == 1) {
              var result = List(("test-token", "true"))
              res = Neo4jResult(result)
              numberOfTimesCalled = numberOfTimesCalled + 1

            } else {
              var result = List("test-token")
              res = Neo4jResult(result)
              numberOfTimesCalled = numberOfTimesCalled + 1
            }
          }
          sender ! ReadOperationResult(res)

        }
      }
    })

    import com.whatamidoing.actors.red5.FrameSupervisor._

    val frameSupervisor = TestActorRef(new Actor {
      def receive = {
        case RTMPMessage(message, token) => {
          println(message)
        }
      }
    })

    ActorUtils.neo4jreader = neo4jReader
    ActorUtils.frameSupervisor = frameSupervisor
    ActorUtils.neo4jwriter = neo4jWriter

    super.beforeEach() // To be stackable, must call super.beforeEach
  }

  override def afterEach() {
    try super.afterEach() // To be stackable, must call super.afterEach
  }
}

