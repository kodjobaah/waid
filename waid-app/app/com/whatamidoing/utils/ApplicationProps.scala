package com.whatamidoing.utils

import play.api.Play.current
import play.api.i18n.Lang
import play.api.i18n.Messages._
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
  //val envConfig = ConfigFactory.load()
 
    val environment =   envConfig getString "thisIsWhatIAmDoing.environment" 
  	
    /** ConfigFactory.load(String) can load other files. 
      * File extension must be conf, json, or properties. 
      * The extension can be omitted in the load argument. 
      */

    val config = ConfigFactory.load(environment) // eg "test" or "test.conf" etc 
 
    /** Libraries and frameworks should contain a reference.conf 
      * which can then be validated using: 
      * config.checkValid(ConfigFactory.defaultReference(), "configurableApp") 
      */ 

    implicit val lang: Lang = Lang("en")
    val neo4jServer = envConfig getString "thisIsWhatIAmDoing.neo4jServer"
    
    //Constants from message file
    //val noTokenProvided = play.api.i18n.Messages("no.token.provided")(lang)
    //val noEmailProvided = play.api.i18n.Messages("no.email.provided")(lang)

    val redisServer = envConfig getString "redis.server"
    val redisPort = envConfig getInt "redis.port"

    val waidServer = envConfig getString "thisIsWhatIAmDoing.server"
}