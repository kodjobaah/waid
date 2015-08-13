/*! videojs-contrib-hls - v0.3.0 - 2014-02-25
* Copyright (c) 2014 Brightcove; Licensed  */
(function(window, videojs, document, undefined) {

videojs.hls = {
  /**
   * Whether the browser has built-in HLS support.
   */
  supportsNativeHls: (function() {
    var
      video = document.createElement('video'),
      xMpegUrl,
      vndMpeg;

    // native HLS is definitely not supported if HTML5 video isn't
    if (!videojs.Html5.isSupported()) {
      return false;
    }

    xMpegUrl = video.canPlayType('application/x-mpegURL');
    vndMpeg = video.canPlayType('application/vnd.apple.mpegURL');
    return (/probably|maybe/).test(xMpegUrl) ||
      (/probably|maybe/).test(vndMpeg);
  })()
};

var
  // the desired length of video to maintain in the buffer, in seconds
  goalBufferLength = 5,

  // a fudge factor to apply to advertised playlist bitrates to account for
  // temporary flucations in client bandwidth
  bandwidthVariance = 1.1,

  /**
   * A comparator function to sort two playlist object by bandwidth.
   * @param left {object} a media playlist object
   * @param right {object} a media playlist object
   * @return {number} Greater than zero if the bandwidth attribute of
   * left is greater than the corresponding attribute of right. Less
   * than zero if the bandwidth of right is greater than left and
   * exactly zero if the two are equal.
   */
  playlistBandwidth = function(left, right) {
    var leftBandwidth, rightBandwidth;
    if (left.attributes && left.attributes.BANDWIDTH) {
      leftBandwidth = left.attributes.BANDWIDTH;
    }
    leftBandwidth = leftBandwidth || window.Number.MAX_VALUE;
    if (right.attributes && right.attributes.BANDWIDTH) {
      rightBandwidth = right.attributes.BANDWIDTH;
    }
    rightBandwidth = rightBandwidth || window.Number.MAX_VALUE;

    return leftBandwidth - rightBandwidth;
  },

  /**
   * A comparator function to sort two playlist object by resolution (width).
   * @param left {object} a media playlist object
   * @param right {object} a media playlist object
   * @return {number} Greater than zero if the resolution.width attribute of
   * left is greater than the corresponding attribute of right. Less
   * than zero if the resolution.width of right is greater than left and
   * exactly zero if the two are equal.
   */
  playlistResolution = function(left, right) {
    var leftWidth, rightWidth;

    if (left.attributes && left.attributes.RESOLUTION && left.attributes.RESOLUTION.width) {
      leftWidth = left.attributes.RESOLUTION.width;
    }

    leftWidth = leftWidth || window.Number.MAX_VALUE;

    if (right.attributes && right.attributes.RESOLUTION && right.attributes.RESOLUTION.width) {
      rightWidth = right.attributes.RESOLUTION.width;
    }

    rightWidth = rightWidth || window.Number.MAX_VALUE;

    // NOTE - Fallback to bandwidth sort as appropriate in cases where multiple renditions
    // have the same media dimensions/ resolution
    if (leftWidth === rightWidth && left.attributes.BANDWIDTH && right.attributes.BANDWIDTH) {
      return left.attributes.BANDWIDTH - right.attributes.BANDWIDTH;
    } else {
      return leftWidth - rightWidth;
    }
  },

  /**
   * TODO - Document this great feature.
   *
   * @param playlist
   * @param time
   * @returns int
   */
  getMediaIndexByTime = function(playlist, time) {
    var index, counter, timeRanges, currentSegmentRange;

    timeRanges = [];
    for (index = 0; index < playlist.segments.length; index++) {
      currentSegmentRange = {};
      currentSegmentRange.start = (index === 0) ? 0 : timeRanges[index - 1].end;
      currentSegmentRange.end = currentSegmentRange.start + playlist.segments[index].duration;
      timeRanges.push(currentSegmentRange);
    }

    for (counter = 0; counter < timeRanges.length; counter++) {
      if (time >= timeRanges[counter].start && time < timeRanges[counter].end) {
        return counter;
      }
    }

    return -1;

  },

  /**
   * Calculate the total duration for a playlist based on segment metadata.
   * @param playlist {object} a media playlist object
   * @return {number} the currently known duration, in seconds
   */
  totalDuration = function(playlist) {
    var
      duration = 0,
      i = playlist.segments.length,
      segment;
    while (i--) {
      segment = playlist.segments[i];
      duration += segment.duration || playlist.targetDuration || 0;
    }
    return duration;
  },

  /**
   * Constructs a new URI by interpreting a path relative to another
   * URI.
   * @param basePath {string} a relative or absolute URI
   * @param path {string} a path part to combine with the base
   * @return {string} a URI that is equivalent to composing `base`
   * with `path`
   * @see http://stackoverflow.com/questions/470832/getting-an-absolute-url-from-a-relative-one-ie6-issue
   */
  resolveUrl = function(basePath, path) {
    // use the base element to get the browser to handle URI resolution
    var
      oldBase = document.querySelector('base'),
      docHead = document.querySelector('head'),
      a = document.createElement('a'),
      base = oldBase,
      oldHref,
      result;

    // prep the document
    if (oldBase) {
      oldHref = oldBase.href;
    } else {
      base = docHead.appendChild(document.createElement('base'));
    }

    base.href = basePath;
    a.href = path;
    result = a.href;

    // clean up
    if (oldBase) {
      oldBase.href = oldHref;
    } else {
      docHead.removeChild(base);
    }
    return result;
  },

  /**
   * Initializes the HLS plugin.
   * @param options {mixed} the URL to an HLS playlist
   */
  init = function(options) {
    var
      mediaSource = new videojs.MediaSource(),
      segmentParser = new videojs.hls.SegmentParser(),
      player = this,

      // async queue of Uint8Arrays to be appended to the SourceBuffer
      tags = videojs.hls.queue(function(tag) {
        player.hls.sourceBuffer.appendBuffer(tag, player);

        if (player.hls.mediaIndex === player.hls.media.segments.length) {
          mediaSource.endOfStream();
        }
      }),
      srcUrl,

      segmentXhr,
      downloadPlaylist,
      fillBuffer;

    // if the video element supports HLS natively, do nothing
    if (videojs.hls.supportsNativeHls) {
      return;
    }

    srcUrl = (function() {
      var
        extname,
        i = 0,
        j = 0,
        src = player.el().querySelector('.vjs-tech').src,
        sources = player.options().sources,
        techName,
        length = sources.length;

      // use the URL specified in options if one was provided
      if (typeof options === 'string') {
        return options;
      } else if (options) {
        return options.url;
      }

      // src attributes take precedence over source children
      if (src) {

        // assume files with the m3u8 extension are HLS
        extname = (/[^#?]*(?:\/[^#?]*\.([^#?]*))/).exec(src);
        if (extname && extname[1] === 'm3u8') {
          return src;
        }
        return;
      }

      // find the first playable source
      for (; i < length; i++) {

        // ignore sources without a specified type
        if (!sources[i].type) {
          continue;
        }

        // do nothing if the source is handled by one of the standard techs
        for (j in player.options().techOrder) {
          techName = player.options().techOrder[j];
          techName = techName[0].toUpperCase() + techName.substring(1);
          if (videojs[techName].canPlaySource({ type: sources[i].type })) {
            return;
          }
        }

        // use the plugin if the MIME type specifies HLS
        if ((/application\/x-mpegURL/).test(sources[i].type) ||
            (/application\/vnd\.apple\.mpegURL/).test(sources[i].type)) {
          return sources[i].src;
        }
      }
    })();

    if (!srcUrl) {
      // do nothing until the plugin is initialized with a valid URL
      videojs.log('hls: no valid playlist URL specified');
      return;
    }

    // expose the HLS plugin state
    player.hls.readyState = function() {
      if (!player.hls.media) {
        return 0; // HAVE_NOTHING
      }
      return 1;   // HAVE_METADATA
    };

    player.on('seeking', function() {
      var currentTime = player.currentTime();
      player.hls.mediaIndex = getMediaIndexByTime(player.hls.media, currentTime);

      // cancel outstanding requests and buffer appends
      if (segmentXhr) {
        segmentXhr.abort();
      }
      tags.tasks = [];

      // begin filling the buffer at the new position
      fillBuffer(currentTime * 1000);
    });


    /**
     * Chooses the appropriate media playlist based on the current
     * bandwidth estimate and the player size.
     * @return the highest bitrate playlist less than the currently detected
     * bandwidth, accounting for some amount of bandwidth variance
     */
    player.hls.selectPlaylist = function () {
      var
        effectiveBitrate,
        sortedPlaylists = player.hls.master.playlists.slice(),
        bandwidthPlaylists = [],
        i = sortedPlaylists.length,
        variant,
        bandwidthBestVariant,
        resolutionBestVariant;

      sortedPlaylists.sort(playlistBandwidth);

      // filter out any variant that has greater effective bitrate
      // than the current estimated bandwidth
      while (i--) {
        variant = sortedPlaylists[i];

        // ignore playlists without bandwidth information
        if (!variant.attributes || !variant.attributes.BANDWIDTH) {
          continue;
        }

        effectiveBitrate = variant.attributes.BANDWIDTH * bandwidthVariance;

        if (effectiveBitrate < player.hls.bandwidth) {
          bandwidthPlaylists.push(variant);

          // since the playlists are sorted in ascending order by
          // bandwidth, the first viable variant is the best
          if (!bandwidthBestVariant) {
            bandwidthBestVariant = variant;
          }
        }
      }

      i = bandwidthPlaylists.length;

      // sort variants by resolution
      bandwidthPlaylists.sort(playlistResolution);

      // iterate through the bandwidth-filtered playlists and find
      // best rendition by player dimension
      while (i--) {
        variant = bandwidthPlaylists[i];

        // ignore playlists without resolution information
        if (!variant.attributes || !variant.attributes.RESOLUTION ||
          !variant.attributes.RESOLUTION.width || !variant.attributes.RESOLUTION.height) {
          continue;
        }

        // since the playlists are sorted, the first variant that has
        // dimensions less than or equal to the player size is the
        // best
        if (variant.attributes.RESOLUTION.width <= player.width() &&
          variant.attributes.RESOLUTION.height <= player.height()) {
          resolutionBestVariant = variant;
          break;
        }
      }

      // fallback chain of variants
      return resolutionBestVariant || bandwidthBestVariant || sortedPlaylists[0];
    };

    /**
     * Download an M3U8 and update the current manifest object. If the provided
     * URL is a master playlist, the default variant will be downloaded and
     * parsed as well. Triggers `loadedmanifest` once for each playlist that is
     * downloaded and `loadedmetadata` after at least one media playlist has
     * been parsed. Whether multiple playlists were downloaded or not, when
     * `loadedmetadata` fires a parsed or inferred master playlist object will
     * be available as `player.hls.master`.
     *
     * @param url {string} a URL to the M3U8 file to process
     */
    downloadPlaylist = function(url) {
      var xhr = new window.XMLHttpRequest();
      xhr.open('GET', url);
      xhr.onreadystatechange = function() {
        var i, parser, playlist, playlistUri;

        if (xhr.readyState === 4) {
          if (xhr.status >= 400 || this.status === 0) {
            player.hls.error = {
              status: xhr.status,
              message: 'HLS playlist request error at URL: ' + url,
              code: (xhr.status >= 500) ? 4 : 2
            };
            player.trigger('error');
            return;
          }

          // readystate DONE
          parser = new videojs.m3u8.Parser();
          parser.push(xhr.responseText);

          // master playlists
          if (parser.manifest.playlists) {
            player.hls.master = parser.manifest;
            downloadPlaylist(resolveUrl(url, parser.manifest.playlists[0].uri));
            player.trigger('loadedmanifest');
            return;
          }

          // media playlists
          if (player.hls.master) {
            // merge this playlist into the master
            i = player.hls.master.playlists.length;

            while (i--) {
              playlist = player.hls.master.playlists[i];
              playlistUri = resolveUrl(srcUrl, playlist.uri);
              if (playlistUri === url) {
                player.hls.master.playlists[i] =
                  videojs.util.mergeOptions(playlist, parser.manifest);
              }
            }
          } else {
            // infer a master playlist if none was previously requested
            player.hls.master = {
              playlists: [parser.manifest]
            };
          }

          // always start playback with the default rendition
          if (!player.hls.media) {
            player.hls.media = player.hls.master.playlists[0];

            // update the duration
            if (parser.manifest.totalDuration) {
              player.duration(parser.manifest.totalDuration);
            } else {
              player.duration(totalDuration(parser.manifest));
            }

            // periodicaly check if the buffer needs to be refilled
            player.on('timeupdate', fillBuffer);

            player.trigger('loadedmanifest');
            player.trigger('loadedmetadata');
            fillBuffer();
            return;
          }

          // select a playlist and download its metadata if necessary
          playlist = player.hls.selectPlaylist();
          if (!playlist.segments) {
            downloadPlaylist(resolveUrl(srcUrl, playlist.uri));
          } else {
            player.hls.media = playlist;

            // update the duration
            if (player.hls.media.totalDuration) {
              player.duration(player.hls.media.totalDuration);
            } else {
              player.duration(totalDuration(player.hls.media));
            }
          }

          player.trigger('loadedmanifest');
        }
      };
      xhr.send(null);
    };

    /**
     * Determines whether there is enough video data currently in the buffer
     * and downloads a new segment if the buffered time is less than the goal.
     * @param offset (optional) {number} the offset into the downloaded segment
     * to seek to, in milliseconds
     */
    fillBuffer = function(offset) {
      var
        buffered = player.buffered(),
        bufferedTime = 0,
        segment = player.hls.media.segments[player.hls.mediaIndex],
        segmentUri,
        startTime;

      // if there is a request already in flight, do nothing
      if (segmentXhr) {
        return;
      }

      // if the video has finished downloading, stop trying to buffer
      if (!segment) {
        return;
      }

      if (buffered) {
        // assuming a single, contiguous buffer region
        bufferedTime = player.buffered().end(0) - player.currentTime();
      }

      // if there is plenty of content in the buffer, relax for awhile
      if (bufferedTime >= goalBufferLength) {
        return;
      }

      segmentUri = resolveUrl(resolveUrl(srcUrl, player.hls.media.uri || ''),
                              segment.uri);

      // request the next segment
      segmentXhr = new window.XMLHttpRequest();
      segmentXhr.open('GET', segmentUri);
      segmentXhr.responseType = 'arraybuffer';
      segmentXhr.onreadystatechange = function() {
        var playlist;

        // wait until the request completes
        if (this.readyState !== 4) {
          return;
        }

        // the segment request is no longer outstanding
        segmentXhr = null;

        // trigger an error if the request was not successful
        if (this.status >= 400) {
          player.hls.error = {
            status: this.status,
            message: 'HLS segment request error at URL: ' + segmentUri,
            code: (this.status >= 500) ? 4 : 2
          };

          // try moving on to the next segment
          player.hls.mediaIndex++;
          return;
        }

        // stop processing if the request was aborted
        if (!this.response) {
          return;
        }

        // calculate the download bandwidth
        player.hls.segmentXhrTime = (+new Date()) - startTime;
        player.hls.bandwidth = (this.response.byteLength / player.hls.segmentXhrTime) * 8 * 1000;

        // transmux the segment data from MP2T to FLV
        segmentParser.parseSegmentBinaryData(new Uint8Array(this.response));
        segmentParser.flushTags();

        // if we're refilling the buffer after a seek, scan through the muxed
        // FLV tags until we find the one that is closest to the desired
        // playback time
        if (offset !== undefined && typeof offset === "number") {
          while (segmentParser.getTags()[0].pts < offset) {
            segmentParser.getNextTag();
          }
        }

        while (segmentParser.tagsAvailable()) {
          // queue up the bytes to be appended to the SourceBuffer
          // the queue gives control back to the browser between tags
          // so that large segments don't cause a "hiccup" in playback
          tags.push(segmentParser.getNextTag().bytes);
        }

        player.hls.mediaIndex++;

        // figure out what stream the next segment should be downloaded from
        // with the updated bandwidth information
        playlist = player.hls.selectPlaylist();
        if (!playlist.segments) {
          downloadPlaylist(resolveUrl(srcUrl, playlist.uri));
        } else {
          player.hls.media = playlist;
        }
      };
      startTime = +new Date();
      segmentXhr.send(null);
    };

    // load the MediaSource into the player
    mediaSource.addEventListener('sourceopen', function() {
      // construct the video data buffer and set the appropriate MIME type
      var sourceBuffer = mediaSource.addSourceBuffer('video/flv; codecs="vp6,aac"');
      player.hls.sourceBuffer = sourceBuffer;
      sourceBuffer.appendBuffer(segmentParser.getFlvHeader());

      player.hls.mediaIndex = 0;
      downloadPlaylist(srcUrl);
    });
    player.src([{
      src: videojs.URL.createObjectURL(mediaSource),
      type: "video/flv"
    }]);

    if (player.options().autoplay) {
      player.play();
    }
  };

videojs.plugin('hls', function() {
  var initialize = function() {
    return function() {
      this.hls = initialize();
      init.apply(this, arguments);
    };
  };
  initialize().apply(this, arguments);
});

})(window, window.videojs, document);

(function(window, videojs, undefined) {
  'use strict';
  /**
   * A queue object that manages tasks that should be processed
   * serially but asynchronously. Loosely adapted from
   * https://github.com/caolan/async#queue.
   * @param worker {function} the callback to invoke with each value
   * pushed onto the queue
   * @return {object} an object with an array of `tasks` that remain to
   * be processed and function `push` to add new tasks
   */
  videojs.hls.queue = function(worker) {
    var
      q = {
        tasks: [],
        running: false,
        push: function(task) {
          q.tasks.push(task);
          if (!q.running) {
            window.setTimeout(process, 0);
            q.running = true;
          }
        },
      },
      process = function() {
        var task;
        if (q.tasks.length) {
          task = q.tasks.shift();
          worker.call(this, task);
          window.setTimeout(process, 0);
        } else {
          q.running = false;
        }
      };
    return q;
  };
})(window, window.videojs);

(function(window) {

var
  hls = window.videojs.hls,

  // commonly used metadata properties
  widthBytes = new Uint8Array('width'.length),
  heightBytes = new Uint8Array('height'.length),
  videocodecidBytes = new Uint8Array('videocodecid'.length),
  i;

// calculating the bytes of common metadata names ahead of time makes the
// corresponding writes faster because we don't have to loop over the
// characters
// re-test with test/perf.html if you're planning on changing this
for (i in 'width') {
  widthBytes[i] = 'width'.charCodeAt(i);
}
for (i in 'height') {
  heightBytes[i] = 'height'.charCodeAt(i);
}
for (i in 'videocodecid') {
  videocodecidBytes[i] = 'videocodecid'.charCodeAt(i);
}

// (type:uint, extraData:Boolean = false) extends ByteArray
hls.FlvTag = function(type, extraData) {
  var
    // Counter if this is a metadata tag, nal start marker if this is a video
    // tag. unused if this is an audio tag
    adHoc = 0, // :uint

    // checks whether the FLV tag has enough capacity to accept the proposed
    // write and re-allocates the internal buffers if necessary
    prepareWrite = function(flv, count) {
      var
        bytes,
        minLength = flv.position + count;
      if (minLength < flv.bytes.byteLength) {
        // there's enough capacity so do nothing
        return;
      }

      // allocate a new buffer and copy over the data that will not be modified
      bytes = new Uint8Array(minLength * 2);
      bytes.set(flv.bytes.subarray(0, flv.position), 0);
      flv.bytes = bytes;
      flv.view = new DataView(flv.bytes.buffer);
    };

  this.keyFrame = false; // :Boolean

  switch(type) {
  case hls.FlvTag.VIDEO_TAG:
    this.length = 16;
    break;
  case hls.FlvTag.AUDIO_TAG:
    this.length = 13;
    this.keyFrame = true;
    break;
  case hls.FlvTag.METADATA_TAG:
    this.length = 29;
    this.keyFrame = true;
    break;
  default:
    throw("Error Unknown TagType");
  }

  this.bytes = new Uint8Array(16384);
  this.view = new DataView(this.bytes.buffer);
  this.bytes[0] = type;
  this.position = this.length;
  this.keyFrame = extraData; // Defaults to false

  // presentation timestamp
  this.pts = 0;
  // decoder timestamp
  this.dts = 0;

  // ByteArray#writeBytes(bytes:ByteArray, offset:uint = 0, length:uint = 0)
  this.writeBytes = function(bytes, offset, length) {
    var
      start = offset || 0,
      end;
    length = length || bytes.byteLength;
    end = start + length;

    prepareWrite(this, length);
    this.bytes.set(bytes.subarray(start, end), this.position);

    this.position += length;
    this.length = Math.max(this.length, this.position);
  };

  // ByteArray#writeByte(value:int):void
  this.writeByte = function(byte) {
    prepareWrite(this, 1);
    this.bytes[this.position] = byte;
    this.position++;
    this.length = Math.max(this.length, this.position);
  };

  // ByteArray#writeShort(value:int):void
  this.writeShort = function(short) {
    prepareWrite(this, 2);
    this.view.setUint16(this.position, short);
    this.position += 2;
    this.length = Math.max(this.length, this.position);
  };

  // Negative index into array
  // (pos:uint):int
  this.negIndex = function(pos) {
    return this.bytes[this.length - pos];
  };

  // The functions below ONLY work when this[0] == VIDEO_TAG.
  // We are not going to check for that because we dont want the overhead
  // (nal:ByteArray = null):int
  this.nalUnitSize = function() {
    if (adHoc === 0) {
      return 0;
    }

    return this.length - (adHoc + 4);
  };

  this.startNalUnit = function() {
    // remember position and add 4 bytes
    if (adHoc > 0) {
      throw new Error("Attempted to create new NAL wihout closing the old one");
    }

    // reserve 4 bytes for nal unit size
    adHoc = this.length;
    this.length += 4;
    this.position = this.length;
  };

  // (nal:ByteArray = null):void
  this.endNalUnit = function(nalContainer) {
    var
      nalStart, // :uint
      nalLength; // :uint

    // Rewind to the marker and write the size
    if (this.length === adHoc + 4) {
      // we started a nal unit, but didnt write one, so roll back the 4 byte size value
      this.length -= 4;
    } else if (adHoc > 0) {
      nalStart = adHoc + 4;
      nalLength = this.length - nalStart;

      this.position = adHoc;
      this.view.setUint32(this.position, nalLength);
      this.position = this.length;

      if (nalContainer) {
        // Add the tag to the NAL unit
        nalContainer.push(this.bytes.subarray(nalStart, nalStart + nalLength));
      }
    }

    adHoc = 0;
  };

  /**
   * Write out a 64-bit floating point valued metadata property. This method is
   * called frequently during a typical parse and needs to be fast.
   */
  // (key:String, val:Number):void
  this.writeMetaDataDouble = function(key, val) {
    var i;
    prepareWrite(this, 2 + key.length + 9);

    // write size of property name
    this.view.setUint16(this.position, key.length);
    this.position += 2;

    // this next part looks terrible but it improves parser throughput by
    // 10kB/s in my testing

    // write property name
    if (key === 'width') {
      this.bytes.set(widthBytes, this.position);
      this.position += 5;
    } else if (key === 'height') {
      this.bytes.set(heightBytes, this.position);
      this.position += 6;
    } else if (key === 'videocodecid') {
      this.bytes.set(videocodecidBytes, this.position);
      this.position += 12;
    } else {
      for (i in key) {
        this.bytes[this.position] = key.charCodeAt(i);
        this.position++;
      }
    }

    // skip null byte
    this.position++;

    // write property value
    this.view.setFloat64(this.position, val);
    this.position += 8;

    // update flv tag length
    this.length = Math.max(this.length, this.position);
    ++adHoc;
  };

  // (key:String, val:Boolean):void
  this.writeMetaDataBoolean = function(key, val) {
    var i;
    prepareWrite(this, 2);
    this.view.setUint16(this.position, key.length);
    this.position += 2;
    for (i in key) {
      console.assert(key.charCodeAt(i) < 255);
      prepareWrite(this, 1);
      this.bytes[this.position] = key.charCodeAt(i);
      this.position++;
    }
    prepareWrite(this, 2);
    this.view.setUint8(this.position, 0x01);
    this.position++;
    this.view.setUint8(this.position, val ? 0x01 : 0x00);
    this.position++;
    this.length = Math.max(this.length, this.position);
    ++adHoc;
  };

  // ():ByteArray
  this.finalize = function() {
    var
      dtsDelta, // :int
      len; // :int

    switch(this.bytes[0]) {
      // Video Data
    case hls.FlvTag.VIDEO_TAG:
      this.bytes[11] = ((this.keyFrame || extraData) ? 0x10 : 0x20 ) | 0x07; // We only support AVC, 1 = key frame (for AVC, a seekable frame), 2 = inter frame (for AVC, a non-seekable frame)
      this.bytes[12] = extraData ?  0x00 : 0x01;

      dtsDelta = this.pts - this.dts;
      this.bytes[13] = (dtsDelta & 0x00FF0000) >>> 16;
      this.bytes[14] = (dtsDelta & 0x0000FF00) >>>  8;
      this.bytes[15] = (dtsDelta & 0x000000FF) >>>  0;
      break;

    case hls.FlvTag.AUDIO_TAG:
      this.bytes[11] = 0xAF; // 44 kHz, 16-bit stereo
      this.bytes[12] = extraData ? 0x00 : 0x01;
      break;

    case hls.FlvTag.METADATA_TAG:
      this.position = 11;
      this.view.setUint8(this.position, 0x02); // String type
      this.position++;
      this.view.setUint16(this.position, 0x0A); // 10 Bytes
      this.position += 2;
      // set "onMetaData"
      this.bytes.set([0x6f, 0x6e, 0x4d, 0x65,
                      0x74, 0x61, 0x44, 0x61,
                      0x74, 0x61], this.position);
      this.position += 10;
      this.bytes[this.position] = 0x08; // Array type
      this.position++;
      this.view.setUint32(this.position, adHoc);
      this.position = this.length;
      this.bytes.set([0, 0, 9], this.position);
      this.position += 3; // End Data Tag
      this.length = this.position;
      break;
    }

    len = this.length - 11;

    // write the DataSize field
    this.bytes[ 1] = (len & 0x00FF0000) >>> 16;
    this.bytes[ 2] = (len & 0x0000FF00) >>>  8;
    this.bytes[ 3] = (len & 0x000000FF) >>>  0;
    // write the Timestamp
    this.bytes[ 4] = (this.pts & 0x00FF0000) >>> 16;
    this.bytes[ 5] = (this.pts & 0x0000FF00) >>>  8;
    this.bytes[ 6] = (this.pts & 0x000000FF) >>>  0;
    this.bytes[ 7] = (this.pts & 0xFF000000) >>> 24;
    // write the StreamID
    this.bytes[ 8] = 0;
    this.bytes[ 9] = 0;
    this.bytes[10] = 0;

    this.view.setUint32(this.length, this.length);
    this.length += 4;
    this.position += 4;

    // trim down the byte buffer to what is actually being used
    this.bytes = this.bytes.subarray(0, this.length);
    this.frameTime = hls.FlvTag.frameTime(this.bytes);
    console.assert(this.bytes.byteLength === this.length);
    return this;
  };
};

hls.FlvTag.AUDIO_TAG = 0x08; // == 8, :uint
hls.FlvTag.VIDEO_TAG = 0x09; // == 9, :uint
hls.FlvTag.METADATA_TAG = 0x12; // == 18, :uint

// (tag:ByteArray):Boolean {
hls.FlvTag.isAudioFrame = function(tag) {
  return hls.FlvTag.AUDIO_TAG === tag[0];
};

// (tag:ByteArray):Boolean {
hls.FlvTag.isVideoFrame = function(tag) {
  return hls.FlvTag.VIDEO_TAG === tag[0];
};

// (tag:ByteArray):Boolean {
hls.FlvTag.isMetaData = function(tag) {
  return hls.FlvTag.METADATA_TAG === tag[0];
};

// (tag:ByteArray):Boolean {
hls.FlvTag.isKeyFrame = function(tag) {
  if (hls.FlvTag.isVideoFrame(tag)) {
    return tag[11] === 0x17;
  }

  if (hls.FlvTag.isAudioFrame(tag)) {
    return true;
  }

  if (hls.FlvTag.isMetaData(tag)) {
    return true;
  }

  return false;
};

// (tag:ByteArray):uint {
hls.FlvTag.frameTime = function(tag) {
  var pts = tag[ 4] << 16; // :uint
  pts |= tag[ 5] <<  8;
  pts |= tag[ 6] <<  0;
  pts |= tag[ 7] << 24;
  return pts;
};

})(this);

(function(window) {

/**
 * Parser for exponential Golomb codes, a variable-bitwidth number encoding
 * scheme used by h264.
 */
window.videojs.hls.ExpGolomb = function(workingData) {
  var
    // the number of bytes left to examine in workingData
    workingBytesAvailable = workingData.byteLength,

    // the current word being examined
    workingWord = 0, // :uint

    // the number of bits left to examine in the current word
    workingBitsAvailable = 0; // :uint;

  // ():uint
  this.length = function() {
    return (8 * workingBytesAvailable);
  };

  // ():uint
  this.bitsAvailable = function() {
    return (8 * workingBytesAvailable) + workingBitsAvailable;
  };

  // ():void
  this.loadWord = function() {
    var
      position = workingData.byteLength - workingBytesAvailable,
      workingBytes = new Uint8Array(4),
      availableBytes = Math.min(4, workingBytesAvailable);

    // console.assert(availableBytes > 0);
    if (availableBytes === 0) {
      throw new Error('no bytes available');
    }

    workingBytes.set(workingData.subarray(position,
                                          position + availableBytes));
    workingWord = new DataView(workingBytes.buffer).getUint32(0);

    // track the amount of workingData that has been processed
    workingBitsAvailable = availableBytes * 8;
    workingBytesAvailable -= availableBytes;
  };

  // (count:int):void
  this.skipBits = function(count) {
    var skipBytes; // :int
    if (workingBitsAvailable > count) {
      workingWord          <<= count;
      workingBitsAvailable -= count;
    } else {
      count -= workingBitsAvailable;
      skipBytes = count / 8;

      count -= (skipBytes * 8);
      workingBytesAvailable -= skipBytes;

      this.loadWord();

      workingWord <<= count;
      workingBitsAvailable -= count;
    }
  };

  // (size:int):uint
  this.readBits = function(size) {
    var
      bits = Math.min(workingBitsAvailable, size), // :uint
      valu = workingWord >>> (32 - bits); // :uint

    console.assert(size < 32, 'Cannot read more than 32 bits at a time');

    workingBitsAvailable -= bits;
    if (workingBitsAvailable > 0) {
      workingWord <<= bits;
    } else if (workingBytesAvailable > 0) {
      this.loadWord();
    }

    bits = size - bits;
    if (bits > 0) {
      return valu << bits | this.readBits(bits);
    } else {
      return valu;
    }
  };

  // ():uint
  this.skipLeadingZeros = function() {
    var leadingZeroCount; // :uint
    for (leadingZeroCount = 0 ; leadingZeroCount < workingBitsAvailable ; ++leadingZeroCount) {
      if (0 !== (workingWord & (0x80000000 >>> leadingZeroCount))) {
        // the first bit of working word is 1
        workingWord <<= leadingZeroCount;
        workingBitsAvailable -= leadingZeroCount;
        return leadingZeroCount;
      }
    }

    // we exhausted workingWord and still have not found a 1
    this.loadWord();
    return leadingZeroCount + this.skipLeadingZeros();
  };

  // ():void
  this.skipUnsignedExpGolomb = function() {
    this.skipBits(1 + this.skipLeadingZeros());
  };

  // ():void
  this.skipExpGolomb = function() {
    this.skipBits(1 + this.skipLeadingZeros());
  };

  // ():uint
  this.readUnsignedExpGolomb = function() {
    var clz = this.skipLeadingZeros(); // :uint
    return this.readBits(clz + 1) - 1;
  };

  // ():int
  this.readExpGolomb = function() {
    var valu = this.readUnsignedExpGolomb(); // :int
    if (0x01 & valu) {
      // the number is odd if the low order bit is set
      return (1 + valu) >>> 1; // add 1 to make it even, and divide by 2
    } else {
      return -1 * (valu >>> 1); // divide by two then make it negative
    }
  };

  // Some convenience functions
  // :Boolean
  this.readBoolean = function() {
    return 1 === this.readBits(1);
  };

  // ():int
  this.readUnsignedByte = function() {
    return this.readBits(8);
  };

  this.loadWord();

};
})(this);

(function(window) {
  var

    ExpGolomb = window.videojs.hls.ExpGolomb,
    FlvTag = window.videojs.hls.FlvTag,

    H264ExtraData = function() {
      this.sps = []; // :Array
      this.pps = []; // :Array

      this.addSPS = function(size) { // :ByteArray
        console.assert(size > 0);
        var tmp = new Uint8Array(size); // :ByteArray
        this.sps.push(tmp);
        return tmp;
      };

      this.addPPS = function(size) { // :ByteArray
        console.assert(size);
        var tmp = new Uint8Array(size); // :ByteArray
        this.pps.push(tmp);
        return tmp;
      };

      this.extraDataExists = function() { // :Boolean
        return 0 < this.sps.length;
      };

      // (sizeOfScalingList:int, expGolomb:ExpGolomb):void
      this.scaling_list = function(sizeOfScalingList, expGolomb) {
        var
          lastScale = 8, // :int
          nextScale = 8, // :int
          j,
          delta_scale; // :int

        for (j = 0; j < sizeOfScalingList; ++j) {
          if (0 !== nextScale) {
            delta_scale = expGolomb.readExpGolomb();
            nextScale = (lastScale + delta_scale + 256) % 256;
            //useDefaultScalingMatrixFlag = ( j = = 0 && nextScale = = 0 )
          }

          lastScale = (nextScale === 0) ? lastScale : nextScale;
          // scalingList[ j ] = ( nextScale == 0 ) ? lastScale : nextScale;
          // lastScale = scalingList[ j ]
        }
      };

      /**
       *              NAL unit
       * |- NAL header -|------ RBSP ------|
       *
       * NAL unit: Network abstraction layer unit. The combination of a NAL
       * header and an RBSP.
       * NAL header: the encapsulation unit for transport-specific metadata in
       * an h264 stream. Exactly one byte.
       * RBSP: raw bit-stream payload. The actual encoded video data.
       *
       * SPS: sequence parameter set. Part of the RBSP. Metadata to be applied
       * to a complete video sequence, like width and height.
       */
      this.getSps0Rbsp = function() { // :ByteArray
        // remove emulation bytes. Is this nesessary? is there ever emulation
        // bytes in the SPS?
        var
          spsCount = 0,
          sps0 = this.sps[0], // :ByteArray
          rbspCount = 0,
          start = 1, // :uint
          end = sps0.byteLength - 2, // :uint
          rbsp = new Uint8Array(sps0.byteLength), // :ByteArray
          offset = 0; // :uint

        // H264 requires emulation bytes (0x03) be dropped to interpret NAL
        // units. For instance, 0x8a03b4 should be read as 0x8ab4.
        for (offset = start ; offset < end ;) {
          if (3 !== sps0[offset + 2]) {
            offset += 3;
          } else if (0 !== sps0[offset + 1]) {
            offset += 2;
          } else if (0 !== sps0[offset + 0]) {
            offset += 1;
          } else {
            rbsp.set([0x00, 0x00], rbspCount);
            spsCount += 2;
            rbspCount += 2;

            if (offset > start) {
              // If there are bytes to write, write them
              rbsp.set(sps0.subarray(start, offset - start), rbspCount);
              spsCount += offset - start;
              rbspCount += offset - start;
            }

            // skip the emulation bytes
            offset += 3;
            start = offset;
          }
        }

        // copy any remaining bytes
        rbsp.set(sps0.subarray(spsCount), rbspCount); // sps0.readBytes(rbsp, rbsp.length);

        return rbsp;
      };

      // (pts:uint):FlvTag
      this.metaDataTag = function(pts) {
        var
          tag = new FlvTag(FlvTag.METADATA_TAG), // :FlvTag
          expGolomb, // :ExpGolomb
          profile_idc, // :int
          chroma_format_idc, // :int
          imax, // :int
          i, // :int

          pic_order_cnt_type, // :int
          num_ref_frames_in_pic_order_cnt_cycle, // :uint

          pic_width_in_mbs_minus1, // :int
          pic_height_in_map_units_minus1, // :int

          frame_mbs_only_flag, // :int
          frame_cropping_flag, // :Boolean

          frame_crop_left_offset = 0, // :int
          frame_crop_right_offset = 0, // :int
          frame_crop_top_offset = 0, // :int
          frame_crop_bottom_offset = 0, // :int

          width,
          height;

          tag.dts = pts;
          tag.pts = pts;
          expGolomb = new ExpGolomb(this.getSps0Rbsp());

        // :int = expGolomb.readUnsignedByte(); // profile_idc u(8)
        profile_idc = expGolomb.readUnsignedByte();

        // constraint_set[0-5]_flag, u(1), reserved_zero_2bits u(2), level_idc u(8)
        expGolomb.skipBits(16);

        // seq_parameter_set_id
        expGolomb.skipUnsignedExpGolomb();

        if (profile_idc === 100 ||
            profile_idc === 110 ||
            profile_idc === 122 ||
            profile_idc === 244 ||
            profile_idc === 44 ||
            profile_idc === 83 ||
            profile_idc === 86 ||
            profile_idc === 118 ||
            profile_idc === 128) {
          chroma_format_idc = expGolomb.readUnsignedExpGolomb();
          if (3 === chroma_format_idc) {
            expGolomb.skipBits(1); // separate_colour_plane_flag
          }
          expGolomb.skipUnsignedExpGolomb(); // bit_depth_luma_minus8
          expGolomb.skipUnsignedExpGolomb(); // bit_depth_chroma_minus8
          expGolomb.skipBits(1); // qpprime_y_zero_transform_bypass_flag
          if (expGolomb.readBoolean()) { // seq_scaling_matrix_present_flag
            imax = (chroma_format_idc !== 3) ? 8 : 12;
            for (i = 0 ; i < imax ; ++i) {
              if (expGolomb.readBoolean()) { // seq_scaling_list_present_flag[ i ]
                if (i < 6) {
                  this.scaling_list(16, expGolomb);
                } else {
                  this.scaling_list(64, expGolomb);
                }
              }
            }
          }
        }

        expGolomb.skipUnsignedExpGolomb(); // log2_max_frame_num_minus4
        pic_order_cnt_type = expGolomb.readUnsignedExpGolomb();

        if ( 0 === pic_order_cnt_type ) {
          expGolomb.readUnsignedExpGolomb(); //log2_max_pic_order_cnt_lsb_minus4
        } else if ( 1 === pic_order_cnt_type ) {
          expGolomb.skipBits(1); // delta_pic_order_always_zero_flag
          expGolomb.skipExpGolomb(); // offset_for_non_ref_pic
          expGolomb.skipExpGolomb(); // offset_for_top_to_bottom_field
          num_ref_frames_in_pic_order_cnt_cycle = expGolomb.readUnsignedExpGolomb();
          for(i = 0 ; i < num_ref_frames_in_pic_order_cnt_cycle ; ++i) {
            expGolomb.skipExpGolomb(); // offset_for_ref_frame[ i ]
          }
        }

        expGolomb.skipUnsignedExpGolomb(); // max_num_ref_frames
        expGolomb.skipBits(1); // gaps_in_frame_num_value_allowed_flag
        pic_width_in_mbs_minus1 = expGolomb.readUnsignedExpGolomb();
        pic_height_in_map_units_minus1 = expGolomb.readUnsignedExpGolomb();

        frame_mbs_only_flag = expGolomb.readBits(1);
        if (0 === frame_mbs_only_flag) {
          expGolomb.skipBits(1); // mb_adaptive_frame_field_flag
        }

        expGolomb.skipBits(1); // direct_8x8_inference_flag
        frame_cropping_flag = expGolomb.readBoolean();
        if (frame_cropping_flag) {
          frame_crop_left_offset = expGolomb.readUnsignedExpGolomb();
          frame_crop_right_offset = expGolomb.readUnsignedExpGolomb();
          frame_crop_top_offset = expGolomb.readUnsignedExpGolomb();
          frame_crop_bottom_offset = expGolomb.readUnsignedExpGolomb();
        }

        width = ((pic_width_in_mbs_minus1 + 1) * 16) - frame_crop_left_offset * 2 - frame_crop_right_offset * 2;
        height = ((2 - frame_mbs_only_flag) * (pic_height_in_map_units_minus1 + 1) * 16) - (frame_crop_top_offset * 2) - (frame_crop_bottom_offset * 2);

        tag.writeMetaDataDouble("videocodecid", 7);
        tag.writeMetaDataDouble("width", width);
        tag.writeMetaDataDouble("height", height);
        // tag.writeMetaDataDouble("videodatarate", 0 );
        // tag.writeMetaDataDouble("framerate", 0);

        return tag;
      };

      // (pts:uint):FlvTag
      this.extraDataTag = function(pts) {
        var
          i,
          tag = new FlvTag(FlvTag.VIDEO_TAG, true);

        tag.dts = pts;
        tag.pts = pts;

        tag.writeByte(0x01);// version
        tag.writeByte(this.sps[0][1]);// profile
        tag.writeByte(this.sps[0][2]);// compatibility
        tag.writeByte(this.sps[0][3]);// level
        tag.writeByte(0xFC | 0x03); // reserved (6 bits), NULA length size - 1 (2 bits)
        tag.writeByte(0xE0 | 0x01 ); // reserved (3 bits), num of SPS (5 bits)
        tag.writeShort( this.sps[0].length ); // data of SPS
        tag.writeBytes( this.sps[0] ); // SPS

        tag.writeByte( this.pps.length ); // num of PPS (will there ever be more that 1 PPS?)
        for (i = 0 ; i < this.pps.length ; ++i) {
          tag.writeShort(this.pps[i].length); // 2 bytes for length of PPS
          tag.writeBytes(this.pps[i]); // data of PPS
        }

        return tag;
      };
    },

    // incomplete, see Table 7.1 of ITU-T H.264 for 12-32
    NALUnitType = {
      unspecified: 0,
      slice_layer_without_partitioning_rbsp_non_idr: 1,
      slice_data_partition_a_layer_rbsp: 2,
      slice_data_partition_b_layer_rbsp: 3,
      slice_data_partition_c_layer_rbsp: 4,
      slice_layer_without_partitioning_rbsp_idr: 5,
      sei_rbsp: 6,
      seq_parameter_set_rbsp: 7,
      pic_parameter_set_rbsp: 8,
      access_unit_delimiter_rbsp: 9,
      end_of_seq_rbsp: 10,
      end_of_stream_rbsp: 11
    };

  window.videojs.hls.H264Stream = function() {
    var
      next_pts, // :uint;
      next_dts, // :uint;
      pts_delta = -1, // :int

      h264Frame, // :FlvTag

      oldExtraData = new H264ExtraData(), // :H264ExtraData
      newExtraData = new H264ExtraData(), // :H264ExtraData

      nalUnitType = -1, // :int

      state; // :uint;

    this.tags = [];

    //(pts:uint, dts:uint, dataAligned:Boolean):void
    this.setNextTimeStamp = function(pts, dts, dataAligned) {
      if (pts_delta < 0) {
        // We assume the very first pts is less than 0x8FFFFFFF (max signed
        // int32)
        pts_delta = pts;
      }

      // We could end up with a DTS less than 0 here. We need to deal with that!
      next_pts = pts - pts_delta;
      next_dts = dts - pts_delta;

      // If data is aligned, flush all internal buffers
      if (dataAligned) {
        this.finishFrame();
      }
    };

    this.finishFrame = function() {
      if (h264Frame) {
        // Push SPS before EVERY IDR frame for seeking
        if (newExtraData.extraDataExists()) {
          oldExtraData = newExtraData;
          newExtraData = new H264ExtraData();
        }

        if (h264Frame.keyFrame) {
          // Push extra data on every IDR frame in case we did a stream change + seek
          this.tags.push(oldExtraData.metaDataTag(h264Frame.pts));
          this.tags.push(oldExtraData.extraDataTag(h264Frame.pts));
        }

        h264Frame.endNalUnit();
        this.tags.push(h264Frame);
      }

      h264Frame = null;
      nalUnitType = -1;
      state = 0;
    };

    // (data:ByteArray, o:int, l:int):void
    this.writeBytes = function(data, offset, length) {
      var
        nalUnitSize, // :uint
        start, // :uint
        end, // :uint
        t; // :int

      // default argument values
      offset = offset || 0;
      length = length || 0;

      if (length <= 0) {
        // data is empty so there's nothing to write
        return;
      }

      // scan through the bytes until we find the start code (0x000001) for a
      // NAL unit and then begin writing it out
      // strip NAL start codes as we go
      switch (state) {
      default:
        /* falls through */
      case 0:
        state = 1;
        /* falls through */
      case 1:
        // A NAL unit may be split across two TS packets. Look back a bit to
        // make sure the prefix of the start code wasn't already written out.
        if (data[offset] <= 1) {
          nalUnitSize = h264Frame ? h264Frame.nalUnitSize() : 0;
          if (nalUnitSize >= 1 && h264Frame.negIndex(1) === 0) {
            // ?? ?? 00 | O[01] ?? ??
            if (data[offset] === 1 &&
                nalUnitSize >= 2 &&
                h264Frame.negIndex(2) === 0) {
              // ?? 00 00 : 01
              if (3 <= nalUnitSize && 0 === h264Frame.negIndex(3)) {
                h264Frame.length -= 3; // 00 00 00 : 01
              } else {
                h264Frame.length -= 2; // 00 00 : 01
              }

              state = 3;
              return this.writeBytes(data, offset + 1, length - 1);
            }

            if (length > 1 && data[offset] === 0 && data[offset + 1] === 1) {
              // ?? 00 | 00 01
              if (nalUnitSize >= 2 && h264Frame.negIndex(2) === 0) {
                h264Frame.length -= 2; // 00 00 : 00 01
              } else {
                h264Frame.length -= 1; // 00 : 00 01
              }

              state = 3;
              return this.writeBytes(data, offset + 2, length - 2);
            }

            if (length > 2 &&
                data[offset] === 0 &&
                data[offset + 1] === 0 &&
                data[offset + 2] === 1) {
              // 00 : 00 00 01
              h264Frame.length -= 1;
              state = 3;
              return this.writeBytes(data, offset + 3, length - 3);
            }
          }
        }
        // allow fall through if the above fails, we may end up checking a few
        // bytes a second time. But that case will be VERY rare
        state = 2;
        /* falls through */
      case 2:
        // Look for start codes in the data from the current offset forward
        start = offset;
        end = start + length;
        for (t = end - 3; offset < t;) {
          if (data[offset + 2] > 1) {
            // if data[offset + 2] is greater than 1, there is no way a start
            // code can begin before offset + 3
            offset += 3;
          } else if (data[offset + 1] !== 0) {
              offset += 2;
          } else if (data[offset] !== 0) {
              offset += 1;
          } else {
            // If we get here we have 00 00 00 or 00 00 01
            if (data[offset + 2] === 1) {
              if (offset > start) {
                h264Frame.writeBytes(data, start, offset - start);
              }
              state = 3;
              offset += 3;
              return this.writeBytes(data, offset, end - offset);
            }

            if (end - offset >= 4 &&
                data[offset + 2] === 0 &&
                data[offset + 3] === 1) {
              if (offset > start) {
                h264Frame.writeBytes(data, start, offset - start);
              }
              state = 3;
              offset += 4;
              return this.writeBytes(data, offset, end - offset);
            }

            // We are at the end of the buffer, or we have 3 NULLS followed by
            // something that is not a 1, either way we can step forward by at
            // least 3
            offset += 3;
          }
        }

        // We did not find any start codes. Try again next packet
        state = 1;
        h264Frame.writeBytes(data, start, length);
        return;
      case 3:
        // The next byte is the first byte of a NAL Unit

        if (h264Frame) {
          // we've come to a new NAL unit so finish up the one we've been
          // working on

          switch (nalUnitType) {
          case NALUnitType.seq_parameter_set_rbsp:
            h264Frame.endNalUnit(newExtraData.sps);
            break;
          case NALUnitType.pic_parameter_set_rbsp:
            h264Frame.endNalUnit(newExtraData.pps);
            break;
          case NALUnitType.slice_layer_without_partitioning_rbsp_idr:
            h264Frame.keyFrame = true;
            h264Frame.endNalUnit();
            break;
          default:
            h264Frame.endNalUnit();
            break;
          }
        }

        // setup to begin processing the new NAL unit
        nalUnitType = data[offset] & 0x1F;
        if (h264Frame && 9 === nalUnitType) {
          this.finishFrame(); // We are starting a new access unit. Flush the previous one
        }

        // finishFrame may render h264Frame null, so we must test again
        if (!h264Frame) {
          h264Frame = new FlvTag(FlvTag.VIDEO_TAG);
          h264Frame.pts = next_pts;
          h264Frame.dts = next_dts;
        }

        h264Frame.startNalUnit();
        // We know there will not be an overlapping start code, so we can skip
        // that test
        state = 2;
        return this.writeBytes(data, offset, length);
      } // switch
    };
  };
})(this);

(function(window) {
var
  FlvTag = window.videojs.hls.FlvTag,
  adtsSampleingRates = [
    96000, 88200,
    64000, 48000,
    44100, 32000,
    24000, 22050,
    16000, 12000
  ];

window.videojs.hls.AacStream = function() {
  var 
    next_pts, // :uint
    pts_delta = -1, // :int
    state, // :uint
    pes_length, // :int

    adtsProtectionAbsent, // :Boolean
    adtsObjectType, // :int
    adtsSampleingIndex, // :int
    adtsChanelConfig, // :int
    adtsFrameSize, // :int
    adtsSampleCount, // :int
    adtsDuration, // :int

    aacFrame, // :FlvTag = null;
    extraData; // :uint;

  this.tags = [];

  // (pts:uint, pes_size:int, dataAligned:Boolean):void
  this.setNextTimeStamp = function(pts, pes_size, dataAligned) {
    if (0 > pts_delta) {
      // We assume the very firts pts is less than 0x80000000
      pts_delta = pts;
    }

    next_pts = pts - pts_delta;
    pes_length = pes_size;

    // If data is aligned, flush all internal buffers
    if (dataAligned) {
      state = 0;
    }
  };

  // (data:ByteArray, o:int = 0, l:int = 0):void
  this.writeBytes = function(data, o, l) {
    var
      e, // :int
      newExtraData, // :uint
      bytesToCopy; // :int

    // default arguments
    o = o || 0;
    l = l || 0;

    // Do not allow more than 'pes_length' bytes to be written
    l = (pes_length < l ? pes_length : l);
    pes_length -= l;
    e = o + l;
    while (o < e) {
      switch (state) {
      default:
        state = 0;
        break;
      case 0:
        if (o >= e) {
          return;
        }
        if (0xFF !== data[o]) {
          console.assert(false, 'Error no ATDS header found');
          o += 1;
          state = 0;
          return;
        }

        o += 1;
        state = 1;
        break;
      case 1:
        if (o >= e) {
          return;
        }
        if (0xF0 !== (data[o] & 0xF0)) {
          console.assert(false, 'Error no ATDS header found');
          o +=1;
          state = 0;
          return;
        }

        adtsProtectionAbsent = !!(data[o] & 0x01);

        o += 1;
        state = 2;
        break;
      case 2:
        if (o >= e) {
          return;
        }
        adtsObjectType = ((data[o] & 0xC0) >>> 6) + 1;
        adtsSampleingIndex = ((data[o] & 0x3C) >>> 2);
        adtsChanelConfig = ((data[o] & 0x01) << 2);

        o += 1;
        state = 3;
        break;
      case 3: 
        if (o >= e) {
          return;
        }
        adtsChanelConfig |= ((data[o] & 0xC0) >>> 6);
        adtsFrameSize = ((data[o] & 0x03) << 11);

        o += 1;
        state = 4;
        break;
      case 4: 
        if (o >= e) {
          return;
        }
        adtsFrameSize |= (data[o] << 3);

        o += 1;
        state = 5;
        break;
      case 5:
        if(o >= e) {
          return;
        }
        adtsFrameSize |= ((data[o] & 0xE0) >>> 5);
        adtsFrameSize -= (adtsProtectionAbsent ? 7 : 9);

        o += 1;
        state = 6;
        break;
      case 6: 
        if (o >= e) {
          return;
        }
        adtsSampleCount = ((data[o] & 0x03) + 1) * 1024;
        adtsDuration = (adtsSampleCount * 1000) / adtsSampleingRates[adtsSampleingIndex];

        newExtraData = (adtsObjectType << 11) |
                       (adtsSampleingIndex << 7) |
                       (adtsChanelConfig << 3);
        if (newExtraData !== extraData) {
          aacFrame = new FlvTag(FlvTag.METADATA_TAG);
          aacFrame.pts = next_pts;
          aacFrame.dts = next_pts;

          // AAC is always 10
          aacFrame.writeMetaDataDouble("audiocodecid", 10); 
          aacFrame.writeMetaDataBoolean("stereo", 2 === adtsChanelConfig);
          aacFrame.writeMetaDataDouble ("audiosamplerate", adtsSampleingRates[adtsSampleingIndex]);
          // Is AAC always 16 bit?
          aacFrame.writeMetaDataDouble ("audiosamplesize", 16); 

          this.tags.push(aacFrame);

          extraData = newExtraData;
          aacFrame = new FlvTag(FlvTag.AUDIO_TAG, true);
          aacFrame.pts = aacFrame.dts;
          // For audio, DTS is always the same as PTS. We want to set the DTS
          // however so we can compare with video DTS to determine approximate
          // packet order
          aacFrame.pts = next_pts; 
          aacFrame.view.setUint16(aacFrame.position, newExtraData);
          aacFrame.position += 2;
          aacFrame.length = Math.max(aacFrame.length, aacFrame.position);

          this.tags.push(aacFrame);
        }

        // Skip the checksum if there is one
        o += 1;
        state = 7;
        break;
      case 7:
        if (!adtsProtectionAbsent) {
          if (2 > (e - o)) {
            return;
          } else {
            o += 2;
          }
        }

        aacFrame = new FlvTag(FlvTag.AUDIO_TAG);
        aacFrame.pts = next_pts;
        aacFrame.dts = next_pts;
        state = 8;
        break;
      case 8:
        while (adtsFrameSize) {
          if (o >= e) {
            return;
          }
          bytesToCopy = (e - o) < adtsFrameSize ? (e - o) : adtsFrameSize;
          aacFrame.writeBytes(data, o, bytesToCopy);
          o += bytesToCopy;
          adtsFrameSize -= bytesToCopy;
        }

        this.tags.push(aacFrame);

        // finished with this frame
        state = 0;
        next_pts += adtsDuration;
      }
    }
  };
};

})(this);

(function(window) {
  var
    videojs = window.videojs,
    FlvTag = videojs.hls.FlvTag,
    H264Stream = videojs.hls.H264Stream,
    AacStream = videojs.hls.AacStream,
    MP2T_PACKET_LENGTH,
    STREAM_TYPES;

  /**
   * An object that incrementally transmuxes MPEG2 Trasport Stream
   * chunks into an FLV.
   */
  videojs.hls.SegmentParser = function() {
    var
      self = this,
      parseTSPacket,
      streamBuffer = new Uint8Array(MP2T_PACKET_LENGTH),
      streamBufferByteCount = 0,
      h264Stream = new H264Stream(),
      aacStream = new AacStream(),
      seekToKeyFrame = false;

    // expose the stream metadata
    self.stream = {
      // the mapping between transport stream programs and the PIDs
      // that form their elementary streams
      programMapTable: {}
    };

    // For information on the FLV format, see
    // http://download.macromedia.com/f4v/video_file_format_spec_v10_1.pdf.
    // Technically, this function returns the header and a metadata FLV tag
    // if duration is greater than zero
    // duration in seconds
    // @return {object} the bytes of the FLV header as a Uint8Array
    self.getFlvHeader = function(duration, audio, video) { // :ByteArray {
      var
        headBytes = new Uint8Array(3 + 1 + 1 + 4),
        head = new DataView(headBytes.buffer),
        metadata,
        result;

      // default arguments
      duration = duration || 0;
      audio = audio === undefined? true : audio;
      video = video === undefined? true : video;

      // signature
      head.setUint8(0, 0x46); // 'F'
      head.setUint8(1, 0x4c); // 'L'
      head.setUint8(2, 0x56); // 'V'

      // version
      head.setUint8(3, 0x01);

      // flags
      head.setUint8(4, (audio ? 0x04 : 0x00) | (video ? 0x01 : 0x00));

      // data offset, should be 9 for FLV v1
      head.setUint32(5, headBytes.byteLength);

      // init the first FLV tag
      if (duration <= 0) {
        // no duration available so just write the first field of the first
        // FLV tag
        result = new Uint8Array(headBytes.byteLength + 4);
        result.set(headBytes);
        result.set([0, 0, 0, 0], headBytes.byteLength);
        return result;
      }

      // write out the duration metadata tag
      metadata = new FlvTag(FlvTag.METADATA_TAG);
      metadata.pts = metadata.dts = 0;
      metadata.writeMetaDataDouble("duration", duration);
      result = new Uint8Array(headBytes.byteLength + metadata.byteLength);
      result.set(head);
      result.set(head.bytesLength, metadata.finalize());

      return result;
    };

    self.flushTags = function() {
      h264Stream.finishFrame();
    };
    self.doSeek = function() {
      self.flushTags();
      aacStream.tags.length = 0;
      h264Stream.tags.length = 0;
      seekToKeyFrame = true;
    };

    self.tagsAvailable = function() { // :int {
      var i, pts; // :uint

      if (seekToKeyFrame) {
        for (i = 0 ; i < h264Stream.tags.length && seekToKeyFrame; ++i) {
          if (h264Stream.tags[i].keyFrame) {
            seekToKeyFrame = false; // We found, a keyframe, stop seeking
          }
        }

        if (seekToKeyFrame) {
          // we didnt find a keyframe. yet
          h264Stream.tags.length = 0;
          return 0;
        }

        // TODO we MAY need to use dts, not pts
        h264Stream.tags = h264Stream.tags.slice(i);
        pts = h264Stream.tags[0].pts;

        // Remove any audio before the found keyframe
        while( 0 < aacStream.tags.length && pts > aacStream.tags[0].pts ) {
          aacStream.tags.shift();
        }
      }

      return h264Stream.tags.length + aacStream.tags.length;
    };

    self.getNextTag = function() { // :ByteArray {
      var tag; // :FlvTag; // return tags in approximate dts order

      if (0 === self.tagsAvailable()) {
        throw new Error("getNextTag() called when 0 == tagsAvailable()");
      }

      if (0 < h264Stream.tags.length) {
        if (0 < aacStream.tags.length && aacStream.tags[0].dts < h264Stream.tags[0].dts) {
          tag = aacStream.tags.shift();
        } else {
          tag = h264Stream.tags.shift();
        }
      } else if ( 0 < aacStream.tags.length ) {
        tag = aacStream.tags.shift();
      } else {
        // We dont have any tags available to return
        return new Uint8Array();
      }

      return tag.finalize();
    };

    self.parseSegmentBinaryData = function(data) { // :ByteArray) {
      var
        dataPosition = 0,
        dataSlice;

      // To avoid an extra copy, we will stash overflow data, and only
      // reconstruct the first packet. The rest of the packets will be
      // parsed directly from data
      if (streamBufferByteCount > 0) {
        if (data.byteLength + streamBufferByteCount < MP2T_PACKET_LENGTH) {
          // the current data is less than a single m2ts packet, so stash it
          // until we receive more

          // ?? this seems to append streamBuffer onto data and then just give up. I'm not sure why that would be interesting.
          videojs.log('data.length + streamBuffer.length < MP2T_PACKET_LENGTH ??');
          streamBuffer.readBytes(data, data.length, streamBuffer.length);
          return;
        } else {
          // we have enough data for an m2ts packet
          // process it immediately
          dataSlice = data.subarray(0, MP2T_PACKET_LENGTH - streamBufferByteCount);
          streamBuffer.set(dataSlice, streamBufferByteCount);

          parseTSPacket(streamBuffer);

          // reset the buffer
          streamBuffer = new Uint8Array(MP2T_PACKET_LENGTH);
          streamBufferByteCount = 0;
        }
      }

      while (true) {
        // Make sure we are TS aligned
        while(dataPosition < data.byteLength  && data[dataPosition] !== 0x47) {
          // If there is no sync byte skip forward until we find one
          // TODO if we find a sync byte, look 188 bytes in the future (if
          // possible). If there is not a sync byte there, keep looking
          dataPosition++;
        }

        // base case: not enough data to parse a m2ts packet
        if (data.byteLength - dataPosition < MP2T_PACKET_LENGTH) {
          if (data.byteLength - dataPosition > 0) {
            // there are bytes remaining, save them for next time
            streamBuffer.set(data.subarray(dataPosition),
                             streamBufferByteCount);
            streamBufferByteCount += data.byteLength - dataPosition;
          }
          return;
        }

        // attempt to parse a m2ts packet
        if (parseTSPacket(data.subarray(dataPosition, dataPosition + MP2T_PACKET_LENGTH))) {
          dataPosition += MP2T_PACKET_LENGTH;
        } else {
          // If there was an error parsing a TS packet. it could be
          // because we are not TS packet aligned. Step one forward by
          // one byte and allow the code above to find the next
          videojs.log('error parsing m2ts packet, attempting to re-align');
          dataPosition++;
        }
      }
    };

    /**
     * Parses a video/mp2t packet and appends the underlying video and
     * audio data onto h264stream and aacStream, respectively.
     * @param data {Uint8Array} the bytes of an MPEG2-TS packet,
     * including the sync byte.
     * @return {boolean} whether a valid packet was encountered
     */
    // TODO add more testing to make sure we dont walk past the end of a TS
    // packet!
    parseTSPacket = function(data) { // :ByteArray):Boolean {
      var
        offset = 0, // :uint
        end = offset + MP2T_PACKET_LENGTH, // :uint

        // Payload Unit Start Indicator
        pusi = !!(data[offset + 1] & 0x40), // mask: 0100 0000

        // packet identifier (PID), a unique identifier for the elementary
        // stream this packet describes
        pid = (data[offset + 1] & 0x1F) << 8 | data[offset + 2], // mask: 0001 1111

        // adaptation_field_control, whether this header is followed by an
        // adaptation field, a payload, or both
        afflag = (data[offset + 3] & 0x30 ) >>> 4,

        patTableId, // :int
        patCurrentNextIndicator, // Boolean
        patSectionLength, // :uint

        pesPacketSize, // :int,
        dataAlignmentIndicator, // :Boolean,
        ptsDtsIndicator, // :int
        pesHeaderLength, // :int

        pts, // :uint
        dts, // :uint

        pmtCurrentNextIndicator, // :Boolean
        pmtProgramDescriptorsLength,
        pmtSectionLength, // :uint

        streamType, // :int
        elementaryPID, // :int
        ESInfolength; // :int

      // Continuity Counter we could use this for sanity check, and
      // corrupt stream detection
      // cc = (data[offset + 3] & 0x0F);

      // move past the header
      offset += 4;

      // if an adaption field is present, its length is specified by
      // the fifth byte of the PES header. The adaptation field is
      // used to specify some forms of timing and control data that we
      // do not currently use.
      if (afflag > 0x01) {
        offset += data[offset] + 1;
      }

      // Handle a Program Association Table (PAT). PATs map PIDs to
      // individual programs. If this transport stream was being used
      // for television broadcast, a program would probably be
      // equivalent to a channel. In HLS, it would be very unusual to
      // create an mp2t stream with multiple programs.
      if (0x0000 === pid) {
        // The PAT may be split into multiple sections and those
        // sections may be split into multiple packets. If a PAT
        // section starts in this packet, PUSI will be true and the
        // first byte of the playload will indicate the offset from
        // the current position to the start of the section.
        if (pusi) {
          offset += 1 + data[offset];
        }
        patTableId = data[offset];

        if (patTableId !== 0x00) {
          videojs.log('the table_id of the PAT should be 0x00 but was' +
                      patTableId.toString(16));
        }

        // the current_next_indicator specifies whether this PAT is
        // currently applicable or is part of the next table to become
        // active
        patCurrentNextIndicator = !!(data[offset + 5] & 0x01);
        if (patCurrentNextIndicator) {
          // section_length specifies the number of bytes following
          // its position to the end of this section
          patSectionLength =  (data[offset + 1] & 0x0F) << 8 | data[offset + 2];
          // move past the rest of the PSI header to the first program
          // map table entry
          offset += 8;

          // we don't handle streams with more than one program, so
          // raise an exception if we encounter one
          // section_length = rest of header + (n * entry length) + CRC
          // = 5 + (n * 4) + 4
          if ((patSectionLength - 5 - 4) / 4 !== 1) {
            throw new Error("TS has more that 1 program");
          }

          // the Program Map Table (PMT) associates the underlying
          // video and audio streams with a unique PID
          self.stream.pmtPid = (data[offset + 2] & 0x1F) << 8 | data[offset + 3];
        }
      } else if (pid === self.stream.programMapTable[STREAM_TYPES.h264] ||
                 pid === self.stream.programMapTable[STREAM_TYPES.adts]) {
        if (pusi) {
          // comment out for speed
          if (0x00 !== data[offset + 0] || 0x00 !== data[offset + 1] || 0x01 !== data[offset + 2]) {
            // look for PES start code
             throw new Error("PES did not begin with start code");
           }

          // var sid:int  = data[offset+3]; // StreamID
          pesPacketSize = (data[offset + 4] << 8) | data[offset + 5];
          dataAlignmentIndicator = (data[offset + 6] & 0x04) !== 0;
          ptsDtsIndicator = data[offset + 7];
          pesHeaderLength = data[offset + 8]; // TODO sanity check header length
          offset += 9; // Skip past PES header

          // PTS and DTS are normially stored as a 33 bit number.
          // JavaScript does not have a integer type larger than 32 bit
          // BUT, we need to convert from 90ns to 1ms time scale anyway.
          // so what we are going to do instead, is drop the least
          // significant bit (the same as dividing by two) then we can
          // divide by 45 (45 * 2 = 90) to get ms.
          if (ptsDtsIndicator & 0xC0) {
            // the PTS and DTS are not written out directly. For information on
            // how they are encoded, see
            // http://dvd.sourceforge.net/dvdinfo/pes-hdr.html
            pts = (data[offset + 0] & 0x0E) << 28
              | (data[offset + 1] & 0xFF) << 21
              | (data[offset + 2] & 0xFE) << 13
              | (data[offset + 3] & 0xFF) <<  6
              | (data[offset + 4] & 0xFE) >>>  2;
            pts /= 45;
            dts = pts;
            if (ptsDtsIndicator & 0x40) {// DTS
              dts = (data[offset + 5] & 0x0E ) << 28
                | (data[offset + 6] & 0xFF ) << 21
                | (data[offset + 7] & 0xFE ) << 13
                | (data[offset + 8] & 0xFF ) << 6
                | (data[offset + 9] & 0xFE ) >>> 2;
              dts /= 45;
            }
          }
          // Skip past "optional" portion of PTS header
          offset += pesHeaderLength;

          if (pid === self.stream.programMapTable[STREAM_TYPES.h264]) {
            h264Stream.setNextTimeStamp(pts,
                                        dts,
                                        dataAlignmentIndicator);
          } else if (pid === self.stream.programMapTable[STREAM_TYPES.adts]) {
            aacStream.setNextTimeStamp(pts,
                                       pesPacketSize,
                                       dataAlignmentIndicator);
          }
        }

        if (pid === self.stream.programMapTable[STREAM_TYPES.adts]) {
          aacStream.writeBytes(data, offset, end - offset);
        } else if (pid === self.stream.programMapTable[STREAM_TYPES.h264]) {
          h264Stream.writeBytes(data, offset, end - offset);
        }
      } else if (self.stream.pmtPid === pid) {
        // similarly to the PAT, jump to the first byte of the section
        if (pusi) {
          offset += 1 + data[offset];
        }
        if (data[offset] !== 0x02) {
          videojs.log('The table_id of a PMT should be 0x02 but was ' +
                      data[offset].toString(16));
        }

        // whether this PMT is currently applicable or is part of the
        // next table to become active
        pmtCurrentNextIndicator = !!(data[offset + 5] & 0x01);
        if (pmtCurrentNextIndicator) {
          // overwrite any existing program map table
          self.stream.programMapTable = {};
          // section_length specifies the number of bytes following
          // its position to the end of this section
          pmtSectionLength  = (data[offset + 1] & 0x0f) << 8 | data[offset + 2];
          // subtract the length of the program info descriptors
          pmtProgramDescriptorsLength = (data[offset + 10] & 0x0f) << 8 | data[offset + 11];
          pmtSectionLength -= pmtProgramDescriptorsLength;
          // skip CRC and PSI data we dont care about
          // rest of header + CRC = 9 + 4
          pmtSectionLength -= 13;

          // align offset to the first entry in the PMT
          offset += 12 + pmtProgramDescriptorsLength;

          // iterate through the entries
          while (0 < pmtSectionLength) {
            // the type of data carried in the PID this entry describes
            streamType = data[offset + 0];
            // the PID for this entry
            elementaryPID = (data[offset + 1] & 0x1F) << 8 | data[offset + 2];

            if (streamType === STREAM_TYPES.h264) {
              if (self.stream.programMapTable[streamType] &&
                  self.stream.programMapTable[streamType] !== elementaryPID) {
                throw new Error("Program has more than 1 video stream");
              }
              self.stream.programMapTable[streamType] = elementaryPID;
            } else if (streamType === STREAM_TYPES.adts) {
              if (self.stream.programMapTable[streamType] &&
                  self.stream.programMapTable[streamType] !== elementaryPID) {
                throw new Error("Program has more than 1 audio Stream");
              }
              self.stream.programMapTable[streamType] = elementaryPID;
            }
            // TODO add support for MP3 audio

            // the length of the entry descriptor
            ESInfolength = (data[offset + 3] & 0x0F) << 8 | data[offset + 4];
            // move to the first byte after the end of this entry
            offset += 5 + ESInfolength;
            pmtSectionLength -=  5 + ESInfolength;
          }
        }
        // We could test the CRC here to detect corruption with extra CPU cost
      } else if (0x0011 === pid) {
        // Service Description Table
      } else if (0x1FFF === pid) {
        // NULL packet
      } else {
        videojs.log("Unknown PID parsing TS packet: " + pid);
      }

      return true;
    };

    self.getTags = function() {
      return h264Stream.tags;
    };

    self.stats = {
      h264Tags: function() {
        return h264Stream.tags.length;
      },
      aacTags: function() {
        return aacStream.tags.length;
      }
    };
  };

  // MPEG2-TS constants
  videojs.hls.SegmentParser.MP2T_PACKET_LENGTH = MP2T_PACKET_LENGTH = 188;
  videojs.hls.SegmentParser.STREAM_TYPES = STREAM_TYPES = {
    h264: 0x1b,
    adts: 0x0f
  };

})(window);

(function(videojs, undefined) {
  var Stream = function() {
    this.init = function() {
      var listeners = {};
      /**
       * Add a listener for a specified event type.
       * @param type {string} the event name
       * @param listener {function} the callback to be invoked when an event of
       * the specified type occurs
       */
      this.on = function(type, listener) {
        if (!listeners[type]) {
          listeners[type] = [];
        }
        listeners[type].push(listener);
      };
      /**
       * Remove a listener for a specified event type.
       * @param type {string} the event name
       * @param listener {function} a function previously registered for this
       * type of event through `on`
       */
      this.off = function(type, listener) {
        var index;
        if (!listeners[type]) {
          return false;
        }
        index = listeners[type].indexOf(listener);
        listeners[type].splice(index, 1);
        return index > -1;
      };
      /**
       * Trigger an event of the specified type on this stream. Any additional
       * arguments to this function are passed as parameters to event listeners.
       * @param type {string} the event name
       */
      this.trigger = function(type) {
        var callbacks, i, length, args;
        callbacks = listeners[type];
        if (!callbacks) {
          return;
        }
        args = Array.prototype.slice.call(arguments, 1);
        length = callbacks.length;
        for (i = 0; i < length; ++i) {
          callbacks[i].apply(this, args);
        }
      };
    };
  };
  /**
   * Forwards all `data` events on this stream to the destination stream. The
   * destination stream should provide a method `push` to receive the data
   * events as they arrive.
   * @param destination {stream} the stream that will receive all `data` events
   * @see http://nodejs.org/api/stream.html#stream_readable_pipe_destination_options
   */
  Stream.prototype.pipe = function(destination) {
    this.on('data', function(data) {
      destination.push(data);
    });
  };

  videojs.hls.Stream = Stream;
})(window.videojs);

(function(videojs, parseInt, isFinite, mergeOptions, undefined) {
  var
    noop = function() {},
    parseAttributes = function(attributes) {
      var
        attrs = attributes.split(','),
        i = attrs.length,
        result = {},
        attr;

      while (i--) {
        attr = attrs[i].split('=');
        attr[0] = attr[0].replace(/^\s+|\s+$/g, '');

        // This is not sexy, but gives us the resulting object we want.
        if (attr[1]) {
          attr[1] = attr[1].replace(/^\s+|\s+$/g, '');
          if (attr[1].indexOf('"') !== -1) {
            attr[1] = attr[1].split('"')[1];
          }
          result[attr[0]] = attr[1];
        } else {
          attrs[i - 1] = attrs[i - 1] + ',' + attr[0];
        }
      }
      return result;
    },
    Stream = videojs.hls.Stream,
    LineStream,
    ParseStream,
    Parser;

  /**
   * A stream that buffers string input and generates a `data` event for each
   * line.
   */
  LineStream = function() {
    var buffer = '';
    LineStream.prototype.init.call(this);

    /**
     * Add new data to be parsed.
     * @param data {string} the text to process
     */
    this.push = function(data) {
      var nextNewline;

      buffer += data;
      nextNewline = buffer.indexOf('\n');

      for (; nextNewline > -1; nextNewline = buffer.indexOf('\n')) {
        this.trigger('data', buffer.substring(0, nextNewline));
        buffer = buffer.substring(nextNewline + 1);
      }
    };
  };
  LineStream.prototype = new Stream();

  /**
   * A line-level M3U8 parser event stream. It expects to receive input one
   * line at a time and performs a context-free parse of its contents. A stream
   * interpretation of a manifest can be useful if the manifest is expected to
   * be too large to fit comfortably into memory or the entirety of the input
   * is not immediately available. Otherwise, it's probably much easier to work
   * with a regular `Parser` object.
   *
   * Produces `data` events with an object that captures the parser's
   * interpretation of the input. That object has a property `tag` that is one
   * of `uri`, `comment`, or `tag`. URIs only have a single additional
   * property, `line`, which captures the entirety of the input without
   * interpretation. Comments similarly have a single additional property
   * `text` which is the input without the leading `#`.
   *
   * Tags always have a property `tagType` which is the lower-cased version of
   * the M3U8 directive without the `#EXT` or `#EXT-X-` prefix. For instance,
   * `#EXT-X-MEDIA-SEQUENCE` becomes `media-sequence` when parsed. Unrecognized
   * tags are given the tag type `unknown` and a single additional property
   * `data` with the remainder of the input.
   */
  ParseStream = function() {
    ParseStream.prototype.init.call(this);
  };
  ParseStream.prototype = new Stream();
  /**
   * Parses an additional line of input.
   * @param line {string} a single line of an M3U8 file to parse
   */
  ParseStream.prototype.push = function(line) {
    var match, event;
    if (line.length === 0) {
      // ignore empty lines
      return;
    }

    // URIs
    if (line[0] !== '#') {
      this.trigger('data', {
        type: 'uri',
        uri: line
      });
      return;
    }

    // Comments
    if (line.indexOf('#EXT') !== 0) {
      this.trigger('data', {
        type: 'comment',
        text: line.slice(1)
      });
      return;
    }

    // Tags
    match = /^#EXTM3U/.exec(line);
    if (match) {
      this.trigger('data', {
        type: 'tag',
        tagType: 'm3u'
      });
      return;
    }
    match = (/^#EXTINF:?([0-9\.]*)?,?(.*)?$/).exec(line);
    if (match) {
      event = {
        type: 'tag',
        tagType: 'inf'
      };
      if (match[1]) {
        event.duration = parseFloat(match[1], 10);
      }
      if (match[2]) {
        event.title = match[2];
      }
      this.trigger('data', event);
      return;
    }
    match = (/^#EXT-X-TARGETDURATION:?([0-9.]*)?/).exec(line);
    if (match) {
      event = {
        type: 'tag',
        tagType: 'targetduration'
      };
      if (match[1]) {
        event.duration = parseInt(match[1], 10);
      }
      this.trigger('data', event);
      return;
    }
    match = (/^#ZEN-TOTAL-DURATION:?([0-9.]*)?/).exec(line);
    if (match) {
      event = {
        type: 'tag',
        tagType: 'totalduration'
      };
      if (match[1]) {
        event.duration = parseInt(match[1], 10);
      }
      this.trigger('data', event);
      return;
    }
    match = (/^#EXT-X-VERSION:?([0-9.]*)?/).exec(line);
    if (match) {
      event = {
        type: 'tag',
        tagType: 'version'
      };
      if (match[1]) {
        event.version = parseInt(match[1], 10);
      }
      this.trigger('data', event);
      return;
    }
    match = (/^#EXT-X-MEDIA-SEQUENCE:?(\-?[0-9.]*)?/).exec(line);
    if (match) {
      event = {
        type: 'tag',
        tagType: 'media-sequence'
      };
      if (match[1]) {
        event.number = parseInt(match[1], 10);
      }
      this.trigger('data', event);
      return;
    }
    match = (/^#EXT-X-PLAYLIST-TYPE:?(.*)?$/).exec(line);
    if (match) {
      event = {
        type: 'tag',
        tagType: 'playlist-type'
      };
      if (match[1]) {
        event.playlistType = match[1];
      }
      this.trigger('data', event);
      return;
    }
    match = (/^#EXT-X-BYTERANGE:?([0-9.]*)?@?([0-9.]*)?/).exec(line);
    if (match) {
      event = {
        type: 'tag',
        tagType: 'byterange'
      };
      if (match[1]) {
        event.length = parseInt(match[1], 10);
      }
      if (match[2]) {
        event.offset = parseInt(match[2], 10);
      }
      this.trigger('data', event);
      return;
    }
    match = (/^#EXT-X-ALLOW-CACHE:?(YES|NO)?/).exec(line);
    if (match) {
      event = {
        type: 'tag',
        tagType: 'allow-cache'
      };
      if (match[1]) {
        event.allowed = !(/NO/).test(match[1]);
      }
      this.trigger('data', event);
      return;
    }
    match = (/^#EXT-X-STREAM-INF:?(.*)$/).exec(line);
    if (match) {
      event = {
        type: 'tag',
        tagType: 'stream-inf'
      };
      if (match[1]) {
        event.attributes = parseAttributes(match[1]);

        if (event.attributes.RESOLUTION) {
          (function() {
            var
              split = event.attributes.RESOLUTION.split('x'),
              resolution = {};
            if (split[0]) {
              resolution.width = parseInt(split[0], 10);
            }
            if (split[1]) {
              resolution.height = parseInt(split[1], 10);
            }
            event.attributes.RESOLUTION = resolution;
          })();
        }
        if (event.attributes.BANDWIDTH) {
          event.attributes.BANDWIDTH = parseInt(event.attributes.BANDWIDTH, 10);
        }
        if (event.attributes['PROGRAM-ID']) {
          event.attributes['PROGRAM-ID'] = parseInt(event.attributes['PROGRAM-ID'], 10);
        }
      }
      this.trigger('data', event);
      return;
    }
    match = (/^#EXT-X-ENDLIST/).exec(line);
    if (match) {
      this.trigger('data', {
        type: 'tag',
        tagType: 'endlist'
      });
      return;
    }

    // unknown tag type
    this.trigger('data', {
      type: 'tag',
      data: line.slice(4, line.length)
    });
  };

  /**
   * A parser for M3U8 files. The current interpretation of the input is
   * exposed as a property `manifest` on parser objects. It's just two lines to
   * create and parse a manifest once you have the contents available as a string:
   *
   * ```js
   * var parser = new videojs.m3u8.Parser();
   * parser.push(xhr.responseText);
   * ```
   *
   * New input can later be applied to update the manifest object by calling
   * `push` again.
   *
   * The parser attempts to create a usable manifest object even if the
   * underlying input is somewhat nonsensical. It emits `info` and `warning`
   * events during the parse if it encounters input that seems invalid or
   * requires some property of the manifest object to be defaulted.
   */
  Parser = function() {
    var
      self = this,
      uris = [],
      currentUri = {};
    Parser.prototype.init.call(this);

    this.lineStream = new LineStream();
    this.parseStream = new ParseStream();
    this.lineStream.pipe(this.parseStream);

    // the manifest is empty until the parse stream begins delivering data
    this.manifest = {
      allowCache: true
    };

    // update the manifest with the m3u8 entry from the parse stream
    this.parseStream.on('data', function(entry) {
      ({
        tag: function() {
          // switch based on the tag type
          (({
            'allow-cache': function() {
              this.manifest.allowCache = entry.allowed;
              if (!('allowed' in entry)) {
                this.trigger('info', {
                  message: 'defaulting allowCache to YES'
                });
                this.manifest.allowCache = true;
              }
            },
            'byterange': function() {
              var byterange = {};
              if ('length' in entry) {
                currentUri.byterange = byterange;
                byterange.length = entry.length;

                if (!('offset' in entry)) {
                  this.trigger('info', {
                    message: 'defaulting offset to zero'
                  });
                  entry.offset = 0;
                }
              }
              if ('offset' in entry) {
                currentUri.byterange = byterange;
                byterange.offset = entry.offset;
              }
            },
            'inf': function() {
              if (!this.manifest.playlistType) {
                this.manifest.playlistType = 'VOD';
                this.trigger('info', {
                  message: 'defaulting playlist type to VOD'
                });
              }
              if (!('mediaSequence' in this.manifest)) {
                this.manifest.mediaSequence = 0;
                this.trigger('info', {
                  message: 'defaulting media sequence to zero'
                });
              }
              if (entry.duration >= 0) {
                currentUri.duration = entry.duration;
              }

              this.manifest.segments = uris;

            },
            'media-sequence': function() {
              if (!isFinite(entry.number)) {
                this.trigger('warn', {
                  message: 'ignoring invalid media sequence: ' + entry.number
                });
                return;
              }
              this.manifest.mediaSequence = entry.number;
            },
            'playlist-type': function() {
              if (!(/VOD|EVENT/).test(entry.playlistType)) {
                this.trigger('warn', {
                  message: 'ignoring unknown playlist type: ' + entry.playlist
                });
                return;
              }
              this.manifest.playlistType = entry.playlistType;
            },
            'stream-inf': function() {
              this.manifest.playlists = uris;

              if (!entry.attributes) {
                this.trigger('warn', {
                  message: 'ignoring empty stream-inf attributes'
                });
                return;
              }

              if (!currentUri.attributes) {
                currentUri.attributes = {};
              }
              currentUri.attributes = mergeOptions(currentUri.attributes,
                                                   entry.attributes);
            },
            'targetduration': function() {
              if (!isFinite(entry.duration) || entry.duration < 0) {
                this.trigger('warn', {
                  message: 'ignoring invalid target duration: ' + entry.duration
                });
                return;
              }
              this.manifest.targetDuration = entry.duration;
            },
            'totalduration': function() {
              if (!isFinite(entry.duration) || entry.duration < 0) {
                this.trigger('warn', {
                  message: 'ignoring invalid total duration: ' + entry.duration
                });
                return;
              }
              this.manifest.totalDuration = entry.duration;
            }
          })[entry.tagType] || noop).call(self);
        },
        uri: function() {
          currentUri.uri = entry.uri;
          uris.push(currentUri);

          // if no explicit duration was declared, use the target duration
          if (this.manifest.targetDuration &&
              !('duration' in currentUri)) {
            this.trigger('warn', {
              message: 'defaulting segment duration to the target duration'
            });
            currentUri.duration = this.manifest.targetDuration;
          }

          // prepare for the next URI
          currentUri = {};
        },
        comment: function() {
          // comments are not important for playback
        }
      })[entry.type].call(self);
    });
  };
  Parser.prototype = new Stream();
  /**
   * Parse the input string and update the manifest object.
   * @param chunk {string} a potentially incomplete portion of the manifest
   */
  Parser.prototype.push = function(chunk) {
    this.lineStream.push(chunk);
  };
  /**
   * Flush any remaining input. This can be handy if the last line of an M3U8
   * manifest did not contain a trailing newline but the file has been
   * completely received.
   */
  Parser.prototype.end = function() {
    // flush any buffered input
    this.lineStream.push('\n');
  };

  window.videojs.m3u8 = {
    LineStream: LineStream,
    ParseStream: ParseStream,
    Parser: Parser
  };
})(window.videojs, window.parseInt, window.isFinite, window.videojs.util.mergeOptions);
