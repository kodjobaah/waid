package com.whatamidoing.actors.xmpp

import akka.actor.Actor
import akka.actor.Props

import models.Messages.CreateXMPPGroupMessage
import play.api.Logger

import org.jivesoftware.smackx.muc.RoomInfo


class CreateXMPPGroup() extends Actor {


  override def receive: Receive = {

    case CreateXMPPGroupMessage(roomJid, token) => {

      import org.jivesoftware.smack.Connection
      import org.jivesoftware.smack.ConnectionConfiguration
      import org.jivesoftware.smack.XMPPConnection

      Connection.DEBUG_ENABLED = true

      import org.jivesoftware.smack.SmackConfiguration
      SmackConfiguration.setLocalSocks5ProxyPort(-1)

      import play.api.Play
      implicit var currentPlay = Play.current
      val xmppDomain = Play.current.configuration.getString("xmpp.domain").get
      val xmppIp = Play.current.configuration.getString("xmpp.ip").get
      val xmppPort = Play.current.configuration.getString("xmpp.port").get
      val adminUserName = Play.current.configuration.getString("xmpp.admin.username").get
      val adminPassword = Play.current.configuration.getString("xmpp.admin.password").get

      // Create a connection to the jabber.org server.
      val config: ConnectionConfiguration = new ConnectionConfiguration(xmppIp, xmppPort.toInt, xmppDomain)
      config.setSASLAuthenticationEnabled(true)
      val conn: XMPPConnection = new XMPPConnection(config)
      import org.jivesoftware.smack.XMPPException
      try {
        conn.connect()
        Logger.debug("---ABLE TO CONNECT:" + conn.isConnected())
        conn.login(adminUserName, adminPassword)
        Logger.info("success:" + conn.isAuthenticated())
        Logger.info("---ABLE TO LOGIN:" + roomJid)
        // Create a MultiUserChat using a Connection for a room
        import org.jivesoftware.smackx.muc.MultiUserChat
        val muc: MultiUserChat = new MultiUserChat(conn, roomJid);

        // Create the room
        muc.create("initialCreation");

        // Send an empty room configuration form which indicates that we want
        // an instant room
        import org.jivesoftware.smackx.Form
        import org.jivesoftware.smackx.FormField

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
        // make the room peristent
        submitForm.setAnswer("muc#roomconfig_persistentroom", true)
        // Send the completed form (with default values) to the server to configure the room
        muc.sendConfigurationForm(submitForm)

        var info: RoomInfo = MultiUserChat.getRoomInfo(conn, roomJid);
        Logger.info("Number of occupants:" + info.getOccupantsCount());
        Logger.info("Room Subject:" + info.getSubject());


      } finally {
        conn.disconnect();
      }

    }
    case _ => {}
  }

  /**
   * If this child has been restarted due to an exception attempt redelivery
   * based on the message configured delay
   */
  override def preRestart(reason: Throwable, message: Option[Any]) {
    Logger.info("Scheduling email message to be sent after attempts:" + message.get)
    self ! message.get
  }
}

object CreateXMPPGroup {

  def props() = Props(new CreateXMPPGroup())

}