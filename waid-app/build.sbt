name := """waid-app"""

//version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

net.virtualvoid.sbt.graph.Plugin.graphSettings

resolvers ++= Seq(
"anormcypher" at "http://repo.anormcypher.org/"
//"Typesafe Releases" at "http://repo.typsafe.com/typesafe/releases/"
)
resolvers += Resolver.mavenLocal

//resolvers += Resolver.typesafeIvyRepo("releases")
//resolvers += Resolver.typesafeRepo("releases")


libraryDependencies ++= Seq(
    "com.jhlabs" % "filters" % "2.0.235-1",
    "com.typesafe.akka" %% "akka-actor" % "2.3.4",
    "com.typesafe.akka" %% "akka-testkit" % "2.3.4",

    "com.typesafe.slick" %% "slick" % "3.0.0",
    "com.github.tminglei" %% "slick-pg" % "0.9.0",/* enum support, you might not need that */
    "com.typesafe.play" %% "play-slick" % "1.0.0",
    "com.typesafe.play" %% "play-slick-evolutions" % "1.0.0",
    "org.postgresql" % "postgresql" % "9.4-1201-jdbc41",
    "org.slf4j" % "slf4j-nop" % "1.7.12",

    //"org.scalamock" %% "scalamock-scalatest-support" % "3.0.1" % "test" exclude ("org.scalatest", "scalatest_2.10"),
    "net.java.dev.jna" % "jna" % "3.5.2",
    "org.apache.commons" % "commons-lang3" % "3.0",
    "commons-cli" % "commons-cli" % "1.2",
    "ch.qos.logback" % "logback-core" % "1.0.13",
    "ch.qos.logback" % "logback-classic" % "1.0.13",
    "ch.qos.logback" % "logback-access" % "1.0.13",
    "org.mindrot" % "jbcrypt" % "0.3m",
    "org.anormcypher" %% "anormcypher" % "0.5.1",
    "org.apache.commons" % "commons-email" % "1.3.1",
    "org.neo4j" % "neo4j-kernel" % "2.0.0-M06" % "test" classifier "tests" classifier "",
    "joda-time" % "joda-time" % "2.2",
    "org.mockito" % "mockito-all" % "1.9.5",
    "org.apache.commons" % "commons-io" % "1.3.2",
    "org.eclipse.jetty" % "jetty-websocket" % "8.1.13.v20130916",
    "com.waid" %% "waid-graph" % "0.0.1",
    "org.neo4j" % "neo4j-cypher" % "2.0.0-M06" % "test",
    "org.scalatest" %% "scalatest" % "2.1.6" % "test",
    "org.specs2" %% "specs2-core" % "3.6.2" % "test",
    "junit" % "junit" % "4.11" % "test",
    "com.novocode" % "junit-interface" % "0.10" % "test",
    "org.ostermiller" % "utils" % "1.07.00",
    "org.scalamock" %% "scalamock-scalatest-support" % "3.1.2" % "test",

    //"com.etaty.rediscala" %% "rediscala" % "1.4.0",
    filters,
    ws
)


routesGenerator := InjectedRoutesGenerator

checksums := Seq("")

testOptions += Tests.Argument(TestFrameworks.JUnit, "-v")
