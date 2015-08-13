package akka.dispatch


import akka.dispatch._


import akka.actor.{PoisonPill}

import akka.actor.ActorSystem.{Settings}
import sun.security.krb5.Config

// We inherit, in this case, from UnboundedPriorityMailbox
// and seed it with the priority generator

import models.Messages.RTMPMessage
import models.Messages.StopVideo
import models.Messages.RTMPCreateStream
import models.Messages.EncodeFrame

class PriorityMailBox(settings: Settings, config: com.typesafe.config.Config)
  extends UnboundedPriorityMailbox(
    // Create a new PriorityGenerator, lower prio means more important
    PriorityGenerator {
      case RTMPMessage(message,token,streamId) => 10
      case RTMPCreateStream(message,token,streamId) => 10
      case EncodeFrame(s) => 10
      case StopVideo(token) => 0
      case PoisonPill    => 1000
      case _     => 50
    })


