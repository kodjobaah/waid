
object TestJoin extends App {


      import org.jivesoftware.smack.Connection
      import org.jivesoftware.smack.ConnectionConfiguration
      import org.jivesoftware.smack.XMPPConnection
      Connection.DEBUG_ENABLED = true
      // Create a connection to the jabber.org server.
      val config: ConnectionConfiguration = new ConnectionConfiguration("192.168.1.2",5222,"my")
      config.setSASLAuthenticationEnabled(false)
      val conn: XMPPConnection = new XMPPConnection(config)
      import org.jivesoftware.smack.XMPPException
      try { 
      	  conn.connect()  
         System.out.println("---ABLE TO CONNECT:"+conn.isConnected())
      	  conn.login("admin", "tigase")
	 System.out.println("success:"+conn.isAuthenticated())
        System.out.println("---ABLE TO LOGIN")
	 Thread.sleep(3500)
      import org.jivesoftware.smackx.muc.MultiUserChat
      val muc: MultiUserChat = new MultiUserChat(conn, "room8@muc.testme.my");
     muc.join("cisco")
      muc.sendMessage("hello")


     } catch {
       case ioe: XMPPException => ioe.printStackTrace()
       case e: InterruptedException => e.printStackTrace()
     } finally {

       }
      

}