

object TestCreateHost extends App {
      import org.jivesoftware.smack.Connection
      import org.jivesoftware.smack.ConnectionConfiguration
      import org.jivesoftware.smack.XMPPConnection
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

         import com.whatamidoing.services.xmpp.AddHocCommands
         val sa = new AddHocCommands()
	 val domainId = "testme.my"
	       sa.addNewVirtualHost(conn,domainId)

	} catch {
	   case io: XMPPException => io.printStackTrace()
	}

}