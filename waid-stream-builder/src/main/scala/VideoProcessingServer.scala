import org.javasimon.jmx.JmxReporter
import org.slf4j.LoggerFactory
import org.zeromq.ZMQ


object VideoProcessingServer {


  val log = LoggerFactory.getLogger("SocketServer")

  def main(args: Array[String]): Unit = {

    //Kamon.start()

    val context: ZMQ.Context = ZMQ.context(1)

    val zmqThread = new VideoReceiver(context)
    sys.addShutdownHook({
      println("ShutdownHook called")
      context.term()
      zmqThread.interrupt()
      zmqThread.join
      // This application wont terminate unless you shutdown Kamon.
      //Kamon.shutdown()
    })


    val reporter: JmxReporter = JmxReporter.forDefaultManager()
      .registerSimons() // add MBean for every Simon
      .registerExistingSimons() // register also already existing ones (ExistingStopwatch in this case)
      .start(); // this performs actual MXBean registration + JmxRegisterCallback is added to manager

    zmqThread.start

  }


}