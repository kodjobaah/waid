name := """waid-graph"""

organization := "com.waid"

version := "0.0.1"

licenses += ("MIT", url("http://opensource.org/licenses/MIT"))

javacOptions ++= Seq("-source", "1.6", "-target", "1.6")

scalaVersion := "2.11.7"

crossScalaVersions := Seq("2.10.4", "2.11.7")

libraryDependencies ++= Seq(
  "net.debasishg" %% "redisclient" % "3.0" ,
  "org.mindrot" % "jbcrypt" % "0.3m",
  "com.fasterxml.uuid" % "java-uuid-generator" % "3.1.3",
  "org.scalatest" %% "scalatest" % "2.2.1" % "test"

)


//publishTo := Some(Resolver.file("file",  new File(Path.userHome.absolutePath+"/.m2/repository")))

//bintraySettings

com.typesafe.sbt.SbtGit.versionWithGit

//publishMavenStyle := true

fork in run := true
