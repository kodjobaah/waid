import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import com.whatamidoing.actors.hls.MonitorConnection
import com.whatamidoing.services.hls.StreamProcessor
import org.zeromq.ZMQ

/**
 * Created by kodjobaah on 13/06/2014.
 */
class VideoReceiver(context: ZMQ.Context) extends Thread {


  val config = ConfigFactory.load()
  val system = ActorSystem("videoprocessing")
  val serverName = config.getString("server.name")
  val serverPort = config.getInt("server.port")


  override def run() {
    //  Setting up the proxy
    val frontend: ZMQ.Socket = context.socket(ZMQ.ROUTER)
    val backend: ZMQ.Socket = context.socket(ZMQ.DEALER)
    frontend.bind("tcp://*:12345")
    backend.bind("inproc://backend")

    val streamProcessor = new StreamProcessor(context)
    streamProcessor.start()

    val monitor = new MonitorConnection(context)
    monitor.start()

    ZMQ.proxy(frontend, backend, null)

    frontend.close()
    backend.close()
  }


}
