package com.whatamidoing.actors.mailbox


import akka.actor.PoisonPill

import akka.actor.ActorSystem.Settings
import akka.dispatch.{PriorityGenerator, UnboundedPriorityMailbox}

// We inherit, in this case, from UnboundedPriorityMailbox
// and seed it with the priority generator

import models.Messages._

class PriorityMailBox(settings: Settings, config: com.typesafe.config.Config)
  extends UnboundedPriorityMailbox(
    // Create a new PriorityGenerator, lower prio means more important
    PriorityGenerator {
      case RTMPMessage(message) => 10
      case RTMPCreateStream(token, streamId) => 10
      case EncodeFrame => 10
      case StopVideo => 0
      case ProblemsEncoding => 0
      case PoisonPill => 1000
      case _ => 10
    })


