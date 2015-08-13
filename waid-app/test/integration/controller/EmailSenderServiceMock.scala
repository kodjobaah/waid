package integration.controller
import org.scalatest.BeforeAndAfterEach
import org.scalatest.Suite
import org.scalamock.MockFactoryBase
import org.scalatest.exceptions.TestFailedException

trait EmailSenderServiceMock extends BeforeAndAfterEach with MockFactoryBase { this: Suite =>
  
  
  var emailServiceMock = mock[com.whatamidoing.mail.EmailSenderService]
  
  override def beforeEach() {
     
     controllers.WhatAmIDoingController.emailSenderService = emailServiceMock
     super.beforeEach() // To be stackable, must call super.beforeEach
  }

  override def afterEach() {
    try super.afterEach() // To be stackable, must call super.afterEach
  }
   
  
   type ExpectationException = TestFailedException
    
  protected def newExpectationException(message: String, methodName: Option[Symbol]) = 
    new TestFailedException(_ => Some(message), None, {e =>
        e.getStackTrace indexWhere { s =>
          !s.getClassName.startsWith("org.scalamock") && !s.getClassName.startsWith("org.scalatest") &&
          !(s.getMethodName == "newExpectationException") && !(s.getMethodName == "reportUnexpectedCall") &&
          !(methodName.isDefined && s.getMethodName == methodName.get.name)
        }
      })
}