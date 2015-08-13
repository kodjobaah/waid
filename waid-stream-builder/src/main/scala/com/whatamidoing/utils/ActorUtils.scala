package com.whatamidoing.utils

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory


object ActorUtils {

  val system = ActorSystem("whatamidoing-system")
  val cl = ActorUtils.getClass.getClassLoader
  val priority = ActorSystem("priority", ConfigFactory.load(), cl)

}