
object TestRemoveRoom extends App {



     import org.jivesoftware.smack.Connection
     import org.jivesoftware.smack.ConnectionConfiguration
     import org.jivesoftware.smack.XMPPConnection
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
         System.out.println("---ABLE TO LOGIN TO ROOM")

	 import com.whatamidoing.services.xmpp.AddHocCommands
	 val ahc = AddHocCommands()
	 ahc.removeRoom(conn,"room20@muc.testme.my.xe","muc@my")
     } catch {
       case ioe: XMPPException => ioe.printStackTrace()
       case e: InterruptedException => e.printStackTrace()
     } finally {
        conn.disconnect()
     }



}