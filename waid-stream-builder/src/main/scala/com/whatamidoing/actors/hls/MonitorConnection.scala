package com.whatamidoing.actors.hls

import org.zeromq.ZMQ
/**
 * Created by kodjobaah on 13/06/2014.
 */
class MonitorConnection(context: ZMQ.Context ) extends Thread {

  val socket = context.socket(ZMQ.PAIR)
  assert(socket != null)

  val monitor = socket.connect("inproc://monitor.req")
  assert(monitor != null)

  override def run() {
    while (true) {

        val event: ZMQ.Event = ZMQ.Event.recv(socket)
        assert (event != null)

      event.getEvent match {

        // listener specific
        case ZMQ.EVENT_LISTENING =>
          println("listening:"+event.getAddress)
        case ZMQ.EVENT_ACCEPTED =>
          println("event_accepted:"+event.getAddress)
        case ZMQ.EVENT_CONNECTED =>
          println("event_connected:"+event.getAddress)
        case ZMQ.EVENT_CONNECT_DELAYED =>
          println("event_connect_delayed:"+event.getAddress)
        case ZMQ.EVENT_CLOSE_FAILED =>
          println("event_close_failed:"+event.getAddress)
        case ZMQ.EVENT_CLOSED =>
          println("event_closed:"+event.getAddress)
        case ZMQ.EVENT_DISCONNECTED =>
          println("evnet_disconnected:"+event.getAddress)
      }
    }
  }

}

