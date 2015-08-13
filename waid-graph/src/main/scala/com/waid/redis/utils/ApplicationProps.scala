package com.waid.redis.utils

import com.typesafe.config._

object ApplicationProps {

    /** ConfigFactory.load() defaults to the following in order: 
      * system properties 
      * application.conf 
      * application.json 
      * application.properties 
      * reference.conf 
      * 
      * So a system property set in the application will override file properties 
      * if it is set before ConfigFactory.load is called. 
      * eg System.setProperty("environment", "production") 
      */ 

    val envConfig = ConfigFactory.load("application")

    /** ConfigFactory.load(String) can load other files. 
      * File extension must be conf, json, or properties. 
      * The extension can be omitted in the load argument. 
      */

    val redisServer = envConfig getString "redis.server"
    val redisPort = envConfig getInt "redis.port"

}