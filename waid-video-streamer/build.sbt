import com.typesafe.sbt.SbtNativePackager.packageArchetype
import com.typesafe.sbt.packager.archetypes.ServerLoader.{SystemV, Upstart}

packageArchetype.java_server


organization  := "com.waid"

name := "waid-video-streamer"

version       := "0.1"

scalaVersion  := "2.11.7"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

libraryDependencies ++= {
  val akkaV = "2.3.9"
  val sprayV = "1.3.3"
  Seq(
  "io.spray"            %%  "spray-can"     % sprayV,
  "io.spray"            %%  "spray-routing" % sprayV,
  "io.spray"            %%  "spray-testkit" % sprayV  % "test",
  "com.typesafe.akka"   %%  "akka-actor"    % akkaV,
  "com.waid"            %%   "waid-graph"   % "0.0.1",
  "com.typesafe.akka"   %%  "akka-testkit"  % akkaV   % "test",
  "org.specs2"          %%  "specs2-core"   % "2.3.11" % "test"
  )

}


Revolver.settings: Seq[sbt.Setting[_]]

Revolver.enableDebugging(port=5151, suspend = true)
lazy val root = (project in file(".")).enablePlugins(SbtTwirl).enablePlugins(JavaServerAppPackaging)


mainClass in Compile := Some("com.waid.stream.Boot")