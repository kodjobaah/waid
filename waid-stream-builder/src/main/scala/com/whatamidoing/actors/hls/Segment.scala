package com.whatamidoing.actors.hls

import java.util.concurrent.TimeUnit

import com.xuggle.xuggler.IAudioSamples.Format
import com.xuggle.xuggler._
import com.xuggle.mediatool.IMediaWriter
import com.xuggle.mediatool.ToolFactory

import java.awt.image._
import org.slf4j.LoggerFactory
import com.typesafe.config.ConfigFactory
import java.awt.geom.AffineTransform
import java.awt.Graphics2D
import java.io.{InputStream, File}
import javax.imageio.ImageIO
import java.util.Properties
import org.javasimon.{Split, Stopwatch, SimonManager}

class Segment(initTime: Long,segDirectory: String, streamName: String,fps: Int) {

  val sName = streamName

  val stopwatch: Stopwatch  = SimonManager.getStopwatch(streamName)

  val split: Split  = stopwatch.start(); // start the stopwatch
  var duration: Double = 0L

  var activeSegment = true

  def log = LoggerFactory.getLogger(classOf[Segment])

  def this() = this(0,"", "",0)

  var startTime: Long = initTime
  var audioStartTime: Long = initTime

  var count = 0
  var mediaWriter: IMediaWriter = null

  def init() {
   mediaWriter = ToolFactory.makeWriter(segDirectory +"/"+ streamName)

   if (mediaWriter.getContainer.isOpened) {
      log info "open before"
    } else {
      log info "not opened beofre"
    }
    log info ("this is mediawrite:" + mediaWriter)

    val streamId: Int = mediaWriter.addVideoStream(0, 0, ICodec.ID.CODEC_ID_H264, 352, 288)


    val audioId: Int = mediaWriter.addAudioStream(1,0,ICodec.ID.CODEC_ID_AAC,1,16000)
    val videoStream: IStream = mediaWriter.getContainer.getStream(streamId)
    val in: InputStream = classOf[Segment] getResourceAsStream ("mpegts-ipod320.properties")
    val props: Properties = new Properties()
    props.load(in)
    import com.xuggle.xuggler.Configuration


//    log.info("Before ------------------")
 //  Configuration.printConfigurable(System.out,mediaWriter.getContainer)
//    Configuration.printConfigurable(System.out,videoStream.getStreamCoder)
    val videoCoder = videoStream.getStreamCoder
     videoCoder.setChannels(0)
    videoCoder.setSampleFormat(IAudioSamples.Format.FMT_S16)
      videoCoder.setSampleRate(16000)

    val retval: Int = Configuration.configure(props, videoCoder)
    videoCoder.setFlag(IStreamCoder.Flags.FLAG_QSCALE, false)
    videoCoder.setProperty("nr", 0)
    videoCoder.setProperty("mbd", 0)
    // g / gop should be less than a segment so at least one key frame is in a segment
    //val gops:Int = 15 // (fps / segment length) == gops
    //videoCoder.setProperty("g", gops)
    //videoCoder.setNumPicturesInGroupOfPictures(gops)
    // previously used with mpeg-ts
    //videoCoder.setProperty("level",3)
    videoCoder.setProperty("async", 2)
    //videoCoder.setProperty("vsync", 1)
    videoCoder.setBitRate(350000)
    videoCoder.setNumPicturesInGroupOfPictures(45)
    videoCoder.setPixelType(IPixelFormat.Type.YUV420P)

    val frameRate: IRational  = IRational.make(fps,1)
    videoCoder.setFrameRate(frameRate)

    videoCoder.setTimeBase(IRational.make(1,fps))
    videoCoder.setBitRateTolerance(videoCoder.getBitRate() / 2)
    videoCoder.setGlobalQuality(0)

    /*
    val codecOptions: IMetaData  = IMetaData.make()
    codecOptions.setValue("tune", "zerolatency")
    */

    //videoCoder.setProperty("tune","zerolatency")

    //videoCoder.setProperty("preset","veryslow")
    //videoCoder.setProperty("crf",0)
 //  log.info("After ------------------")
  // // Configuration.printConfigurable(System.out,videoStream.getStreamCoder)
  //  Configuration.printConfigurable(System.out,mediaWriter.getContainer)
    if (mediaWriter.getContainer.isOpened) {
      log info "open after:"+retval
    } else {
      log info "not after beofre:"+retval
    }
    //mediaWriter.getContainer().getContainerFormat().setOutputFormat("flv",streamName,null)
    //mediaWriter.addVideoStream(0, 0, ICodec.ID.CODEC_ID_FLV1,640, 480)

  }

  def addSound(sample: Array[Short], diff: Int ): Unit = {

    audioStartTime = audioStartTime + (diff * 1000)
    log.info("addsound ["+sample.length+"] ["+audioStartTime+"]")
    mediaWriter.encodeAudio(1,sample,audioStartTime, TimeUnit.MILLISECONDS)

  }


  def addImage(image: BufferedImage, diff: Int) {
    import java.util.concurrent.TimeUnit
      startTime = startTime + diff

    log.info("addImage ["+startTime+"]")


    var im = image
    if (im.getType() == BufferedImage.TYPE_INT_RGB) {

      im = convertType(im, BufferedImage.TYPE_3BYTE_BGR)
    }


    /*
    val tx: AffineTransform = AffineTransform.getScaleInstance(-1, 1)
    tx.translate(-dstbim.getWidth(null), 0)
    val op: AffineTransformOp = new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR)
    dstbim = op.filter(dstbim, null);
    */
    //if (count < 20) {
     // log.info("WRTING FILE")
     // val outputfile = new File("/tmp/raw/image" + count + ".jpg")
     // ImageIO.write(im, "jpg", outputfile)
     //  count = count + 1;
    //}

    mediaWriter.encodeVideo(0, im, startTime, TimeUnit.MILLISECONDS)
  }

  def convertType(src: BufferedImage, t: Int): BufferedImage = {
    //log.info("--------------------- TRYING TO CONVERT----")
    val cco: ColorConvertOp = new ColorConvertOp(null)
    val dest: BufferedImage = new BufferedImage(src.getWidth, src.getHeight, t)
    cco.filter(src, dest)
    //log.info("---------------------- FINNISHED CONVERTING")
    dest
  }

  def fileSize = {
    mediaWriter.getContainer.getFileSize

  }


  def bitRate = {
    mediaWriter.getContainer.getBitRate
  }

  def close() {

    //segDirectory + streamName
    //mediaWriter.getContainer.writeHeader()
    mediaWriter.flush()
    mediaWriter.close()

    val reader = ToolFactory.makeReader(segDirectory +"/"+ streamName)

    try {
      reader.open()

      if (reader.getContainer.getDuration == Global.NO_PTS) {
        duration = 0.0
      }  else {
        duration = reader.getContainer.getDuration()/1000000
      }

      println("------------------------duration["+duration+"]---------------------------")
      reader.close

    } catch {
      case ex: RuntimeException =>
        log.debug(ex.getMessage)
    }


    activeSegment = false
    split.stop()
  }

  def removeSegment = {
    val file = new File(segDirectory + streamName)
    file.delete()
  }

  def isActiveSegment = activeSegment

}

object Segment {

  val config = ConfigFactory.load()
  val segDirectory: String = config.getString("segment.directory")
  val bitrate: Int = config.getInt("segment.bitrate")

  //def apply(streamName: String) = new Segment(0,segDirectory, streamName)
  def apply(startTime: Long,sd: String, streamName: String, fps: Int) = new Segment(startTime,sd, streamName,fps)

}
