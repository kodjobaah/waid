
import com.typesafe.sbt.SbtNativePackager._
import spray.revolver.RevolverPlugin.Revolver

name := "waid-stream-builder"

version := "1.0"

scalaVersion := "2.11.7"

//crossScalaVersions := Seq("2.10.5", "2.11.7")

//packagerSettings

conflictWarning := ConflictWarning.disable

net.virtualvoid.sbt.graph.Plugin.graphSettings

packageArchetype.java_application

resolvers ++= Seq(
//  "kamon snapsho"  at "http://snapshots.kamon.io/",
  "Spray repository" at "http://repo.spray.io/",
"Sonatype snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/",
"Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/",
"anormcypher" at "http://repo.anormcypher.org/"
)

resolvers += Resolver.mavenLocal
resolvers += DefaultMavenRepository


libraryDependencies ++= {
  val akkaV = "2.3.6"
  val sprayV = "1.3.0"
  val atmosV = "1.3.0"
  val kamonVersion = "0.3.4"
  Seq(
    "io.spray"            %   "spray-caching"             % sprayV,
    "io.spray"            %%   "spray-json"               % sprayV,
    "io.spray"            %   "spray-can"                 % sprayV,
    "io.spray"            %   "spray-routing"             % sprayV,
    "io.spray"            %   "spray-client"              % "1.2.0",
    "com.typesafe.akka"   %%   "akka-actor"           % akkaV exclude("com.typesafe.akka","akka-actor_2.10") ,
    //"com.typesafe.atmos"  %   "trace-akka-2.2.1_2.10"   % atmosV ,
    "com.typesafe.akka"   %%   "akka-slf4j"           % akkaV exclude("com.typesafe.akka","akka-actor_2.10") ,
    "net.java.dev.jna"    %   "jna"                       % "3.4.0",
    "com.github.jnr"      %   "jnr-constants"             % "0.8.2",
    "joda-time"           %   "joda-time"                 % "2.2",
    "org.apache.commons"  %   "commons-io"                % "1.3.2",
    "commons-logging"     %   "commons-logging"           % "1.1.1",
    "org.greencheek.spray"%   "spray-cache-spymemcached"  % "0.1.6",
    "org.ostermiller"     %   "utils"                     % "1.07.00",
    "org.apache.commons"  %   "commons-lang3"             % "3.0",
    "commons-cli"         %   "commons-cli"               % "1.2",
    "ch.qos.logback"      %   "logback-core"              % "1.0.13",
    "ch.qos.logback"      %   "logback-classic"           % "1.0.13",
    "ch.qos.logback"      %   "logback-access"            % "1.0.13",
    "org.mindrot"         %   "jbcrypt"                   % "0.3m",
    "org.anormcypher"     % "anormcypher_2.10"     % "0.4.3",
    "org.apache.commons"  %   "commons-email"             % "1.3.1",
    "org.apache.mina"     %   "mina-core"                 % "2.0.7",
    "org.javasimon"       %   "javasimon-core"            % "3.5.0",
    "com.jhlabs"          %   "filters"                   % "2.0.235-1",
    "com.waid"            %%   "waid-graph"            % "0.0.1" exclude("com.typesafe.akka","akka-actor_2.11"),
    //"com.typesafe.akka"   %%    "akka-testkit"        % akkaV,
    "io.spray"            %   "spray-testkit"             % sprayV  % "test",
    //"org.scalatest"       %%  "scalatest"      % "2.1.5"   % "test",
   // "org.scalamock" %% "scalamock-scalatest-support" % "3.0.1" % "test",
    "org.neo4j"           %   "neo4j-kernel"    % "2.0.0-M06" % "test" classifier "tests" classifier "",
    "org.neo4j"           %   "neo4j-cypher" % "2.0.0-M06" % "test",
    "junit"               %   "junit"                     % "4.11"  % "test",
    "org.mockito"         %   "mockito-all"               % "1.9.5",

    //"io.kamon" %% "kamon-core" % kamonVersion exclude("com.typesafe.akka", "akka-actor"),
    //"io.kamon" %% "kamon-akka" % kamonVersion,
    //"io.kamon" %% "kamon-scala" % kamonVersion,
    //"io.kamon" %% "kamon-statsd" % kamonVersion exclude("com.typesafe.akka", "akka-actor"),
   // "io.kamon" %% "kamon-play" % kamonVersion,
    //"io.kamon" %% "kamon-log-reporter" % kamonVersion exclude("com.typesafe.akka", "akka-actor"),
   // "io.kamon" %% "kamon-system-metrics" % kamonVersion,
  "org.aspectj" % "aspectjweaver" % "1.8.1"
  )

}


//aspectjSettings

scalacOptions ++= Seq(
  "-unchecked",
  "-deprecation",
  "-Xlint",
  "-Ywarn-dead-code",
  "-language:_",
  "-target:jvm-1.7",
  "-encoding", "UTF-8"
)

parallelExecution in Test := false

javaOptions in run ++= Seq(
  "-javaagent:/Users/kodjobaah/projects/waid/waid-stream-builder/lib/weaver/aspectjweaver-1.8.6.jar",
  "-Dorg.aspectj.tracing.factory=default",
  "-Djava.library.path=/usr/local/lib"
)

//javaOptions in run += "-javaagent:" + System.getProperty("user.home") + "/.ivy2/cache/org.aspectj/aspectjweaver/jars/aspectjweaver-1.7.3.jar"

//javaOptions <++= AspectjKeys.weaverOptions in Aspectj

// when you call "sbt run" aspectj weaving kicks in

fork in run := true

connectInput in run := true

outputStrategy in run := Some(StdoutOutput)

net.virtualvoid.sbt.graph.Plugin.graphSettings

mainClass in (Compile, run) := Some("VideoProcessingServer")

//Revolver.settings.settings
seq(Revolver.settings: _*)

