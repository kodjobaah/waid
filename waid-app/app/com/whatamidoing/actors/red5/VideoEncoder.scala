package com.whatamidoing.actors.red5

import akka.actor.Actor
import akka.actor.Props

import com.whatamidoing.actors.red5.services.Xuggler
import models.Messages._
import play.api.Logger

import com.whatamidoing.utils._
class VideoEncoder(streamName: String) extends Actor {

  def nameOfStream = streamName
  val xuggler = Xuggler(streamName)
  
   override def receive: Receive = {
 
      case EncodeFrame(frame) => {
      	   import sun.misc.BASE64Decoder
           Logger("VideoEncoder.recieve").info("size of frame:"+frame.length)
           val base64: BASE64Decoder = new BASE64Decoder()
           val bytes64: Array[Byte] = base64.decodeBuffer(frame)
	   /*
           val compressBytes64: Array[Byte] = base64.decodeBuffer(frame)

	   import com.waid.compress.ArithmeticCodeCompression

	   val acc : ArithmeticCodeCompression = new ArithmeticCodeCompression()
	   val uncompressBytes64: Array[Byte] = acc.decompress(compressBytes64)
           val zippedData: Array[Byte] = base64.decodeBuffer(new String(uncompressBytes64,"UTF-8"))

	   val uncompressed: String  = Compressor.decompress(zippedData)
	   */
	   val uncompressed: String  = Compressor.decompress(bytes64)

	   val newBytes64: Array[Byte] = base64.decodeBuffer(uncompressed)

           import java.io.ByteArrayInputStream
           val bais: ByteArrayInputStream = new ByteArrayInputStream(newBytes64)

      	   import javax.imageio.ImageIO
      	   try {
    	       var bufferedImage = ImageIO.read(bais)
    	       //Logger("MyApp").info("--converted buffered image:" + bufferedImage)
    	       xuggler.transmitBufferedImage(bufferedImage)
      	   } catch {
             case ex: Throwable => {
             println(ex)
             }
      	  }

      }
      case EndTransmission => {
        xuggler.close()
        
      }
   }
  
  override def postStop() {
    xuggler.mediaWriter.close()
    
  }
   
}

object VideoEncoder {
  
  def props(streamName: String) = Props(new VideoEncoder(streamName))
  
}