package com.whatamidoing.actors.xmpp

import akka.actor.Actor
import akka.actor.Props


import models.Messages.CreateXMPPRoomMessage

class CreateRoom()  extends Actor {

	   import play.api.Play
      	   implicit var currentPlay = Play.current
      	   val xmppDomain = Play.current.configuration.getString("xmpp.domain").get
      	   val xmppIp = Play.current.configuration.getString("xmpp.ip").get
      	   val xmppPort = Play.current.configuration.getString("xmpp.port").get

      override def receive: Receive = {

        case CreateXMPPRoomMessage(roomJid) => {

      	   import org.jivesoftware.smack.Connection
      	   import org.jivesoftware.smack.ConnectionConfiguration
      	   import org.jivesoftware.smack.XMPPConnection

//      	   Connection.DEBUG_ENABLED = true
          import org.jivesoftware.smack.SmackConfiguration
           SmackConfiguration.setLocalSocks5ProxyPort(-1)



      	   // Create a connection to the jabber.org server.
      	   val config: ConnectionConfiguration = new ConnectionConfiguration(xmppIp,xmppPort.toInt,xmppDomain)
      	   config.setSASLAuthenticationEnabled(true)
      	   val conn: XMPPConnection = new XMPPConnection(config)
      	   import org.jivesoftware.smack.XMPPException
      	   try { 
      	       conn.connect()  
               System.out.println("---ABLE TO CONNECT:"+conn.isConnected())
      	       conn.login("admin", "letmein")
	       System.out.println("success:"+conn.isAuthenticated())
               System.out.println("---ABLE TO LOGIN:"+roomJid)

	       import com.whatamidoing.services.xmpp.BasicCreateRoom
	       def basicCreateRoom = BasicCreateRoom(roomJid,conn)
	       basicCreateRoom.createRoom

      	    
     	    } finally {
               conn.disconnect();
	   }

      }
      case _ => {}


      }

}

object CreateRoom {

       def props() = Props(new CreateRoom())
}