package com.whatamidoing.actors.hls

import org.slf4j.{Logger, LoggerFactory}
import org.zeromq.ZMQ
/**
 * Created by kodjobaah on 13/06/2014.
 */
class MonitorConnection(context: ZMQ.Context ) extends Thread {


  val log: Logger = LoggerFactory.getLogger(classOf[MonitorConnection])
  val socket = context.socket(ZMQ.PAIR)
  assert(socket != null)

  val monitor = socket.connect("inproc://monitor.socket")
  assert(monitor != null)

  override def run() {
    while (true) {

      val zmqEvent: ZMQ.Event = ZMQ.Event.recv(socket)
      assert (zmqEvent != null)
      val event = zmqEvent.getEvent

      if (event == ZMQ.EVENT_LISTENING) {
        log.info("listening:" + zmqEvent.getAddress)
      } else if (event == ZMQ.EVENT_ACCEPTED) {
        log.info("event_accepted:" + zmqEvent.getAddress)
      } else if (event == ZMQ.EVENT_CONNECTED) {
        log.info("event_connected:" + zmqEvent.getAddress)
      } else if (event == ZMQ.EVENT_CONNECT_DELAYED) {
        log.info("event_connect_delayed:" + zmqEvent.getAddress)
      } else if (event == ZMQ.EVENT_CLOSE_FAILED) {
        log.info("event_closed_failed:" + zmqEvent.getAddress)
      } else if (event == ZMQ.EVENT_CLOSED) {
        log.info("event_closed:" + zmqEvent.getAddress)
      } else if (event == ZMQ.EVENT_DISCONNECTED) {
        log.info("event_disconnected:" + zmqEvent.getAddress)
      } else {
        log.info("event_done:" + zmqEvent.getAddress)
      }
    }
  }

}

