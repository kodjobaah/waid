import org.jivesoftware.smack.Connection
import org.jivesoftware.smack.ConnectionConfiguration
import org.jivesoftware.smack.XMPPConnection
import org.jivesoftware.smack.XMPPException
import org.jivesoftware.smackx.Form
import org.jivesoftware.smackx.FormField

object TestRoomHCreate extends App {


      System.setProperty("smack.debugEnabled", "true");
      Connection.DEBUG_ENABLED = true
      // Create a connection to the jabber.org server.
      val config: ConnectionConfiguration = new ConnectionConfiguration("192.168.1.5",5222,"my")
      config.setSASLAuthenticationEnabled(false)
      val conn: XMPPConnection = new XMPPConnection(config)
      import org.jivesoftware.smack.XMPPException
      try { 
      	  conn.connect()  
         System.out.println("---ABLE TO CONNECT:"+conn.isConnected())
      	  conn.login("admin", "tigase")
	 System.out.println("success:"+conn.isAuthenticated())
        System.out.println("---ABLE TO LOGIN")

      // Create a MultiUserChat using a Connection for a room
      import org.jivesoftware.smackx.muc.MultiUserChat
      val muc: MultiUserChat = new MultiUserChat(conn, "room1@muc.testme.my");

      // Create the room
      muc.create("letMeSee");

      // Get the the room's configuration form
      val form: Form = muc.getConfigurationForm()
      // Create a new form to submit based on the original form
      val submitForm: Form = form.createAnswerForm()
      // Add default answers to the form to submit

      import scala.collection.JavaConversions._
      val fields = form.getFields()
      for (field <- fields) {
          if (!FormField.TYPE_HIDDEN.equals(field.getType()) && field.getVariable() != null) {
              // Sets the default value as the answer
              submitForm.setDefaultAnswer(field.getVariable())
          }
      }
      // Sets the new owner of the room
      submitForm.setAnswer("muc#roomconfig_persistentroom",true)
      // Send the completed form (with default values) to the server to configure the room
      muc.sendConfigurationForm(submitForm)


   } catch {
     case io: XMPPException => io.printStackTrace()
   }
}