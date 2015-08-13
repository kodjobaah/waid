package com.whatamidoing.actors.red5.services

import com.xuggle.xuggler.IStreamCoder
import com.xuggle.xuggler.IContainer
import com.xuggle.xuggler.IContainerFormat
import com.xuggle.xuggler.IStream
import com.xuggle.xuggler.IVideoResampler
import com.xuggle.xuggler.IAudioResampler
import com.xuggle.xuggler.ICodec
import com.xuggle.xuggler.IPixelFormat
import com.xuggle.xuggler.IRational
import com.xuggle.xuggler.IPacket
import com.xuggle.xuggler.IVideoPicture
import com.xuggle.xuggler.IAudioSamples
import com.xuggle.mediatool.IMediaWriter
import com.xuggle.mediatool.ToolFactory

import java.awt.image.BufferedImage
import play.Logger

class Xuggler( rtmpUrl: String, streamName: String) {

  Logger.info("INSIDE CONSTRUCTOR:"+rtmpUrl+streamName)
  def this() = this("","")

  //Accessing the constants
  import Xuggler._

  //ToolFactory.makeWriter("rtmp://192.168.0.101:1935/HTTP@FLV/"+streamName)
  //   ToolFactory.makeWriter("rtmp://192.168.1.110:1935/oflaDemo/"+streamName)
  //  val mediaWriter: IMediaWriter =  ToolFactory.makeWriter("rtmp://www.whatamidoing.info:1935/hlsapp/"+streamName)
  val mediaWriter: IMediaWriter =  ToolFactory.makeWriter(rtmpUrl+streamName)
  mediaWriter.getContainer()
  //mediaWriter.open();
  //mediaWriter.setForceInterleave(true);
  //val outFormat: IContainerFormat  = IContainerFormat.make();
  //outFormat.setOutputFormat("flv", rtmpUrl+streamName, null);
  //val container: IContainer  = mediaWriter.getContainer();
  //container.open(rtmpUrl+streamName, IContainer.Type.WRITE, outFormat);
  mediaWriter.addVideoStream(0, 0, ICodec.ID.CODEC_ID_FLV1, 352, 288)
 //mediaWriter.getContainer().getContainerFormat().setOutputFormat("flv",streamName,null)
  //mediaWriter.addVideoStream(0, 0, ICodec.ID.CODEC_ID_FLV1,640, 480)

  var startTime: Long = _


  var count = 0
  def transmitBufferedImage(image: BufferedImage) {
    import java.util.concurrent.TimeUnit
    import javax.imageio.ImageIO
    import java.io.File
    import play.api.Logger
    //Logger.info("ABOUT TO CREATE FILE");
    if (count == 0) {

      startTime = System.currentTimeMillis()
      Logger.info("CREATING FILE");
      val outputfile = new File("/tmp/image.jpg")
      ImageIO.write(image, "jpg", outputfile)
      count = 1
    } else {
       startTime = startTime + 100
    }
        mediaWriter.encodeVideo(0, image, startTime, TimeUnit.MILLISECONDS);

  }
  def transmitFrame(frame: Array[Byte]) = {

    // convert byte array back to BufferedImage
    import java.io.InputStream
    import java.io.ByteArrayInputStream
    import java.awt.image.BufferedImage
    import javax.imageio.ImageIO
    import play.api.Logger
    import java.util.concurrent.TimeUnit
    val in: InputStream = new ByteArrayInputStream(frame);

    //          Logger("HMM").info("inputstream:"+in);

    import com.jhlabs.image.UnsharpFilter
    val filter: UnsharpFilter = new UnsharpFilter()
    val bImageFromConvert: BufferedImage = filter.filter(ImageIO.read(in),null);


    //         Logger("MyApp").info("just before sending %s".format(bImageFromConvert))

    if (bImageFromConvert != null)
      mediaWriter.encodeVideo(0, bImageFromConvert, System.nanoTime() - startTime, TimeUnit.NANOSECONDS);

  }

  def close(){
    mediaWriter.getContainer().writeHeader()
  }

}

object Xuggler {

  import play.api.Play
  implicit var currentPlay = Play.current
  val rtmpUrl: String = Play.current.configuration.getString("rtmp.url").get

  def apply(streamName: String) = new Xuggler(rtmpUrl,streamName)

}
