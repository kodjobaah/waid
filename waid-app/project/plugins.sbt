// Comment to get more information during initialization
logLevel := Level.Info

offline := true

// The Typesafe repository
resolvers ++= Seq(
	"anormcypher" at "http://repo.anormcypher.org/",
	"scct-github-repository" at "http://mtkopone.github.com/scct/maven-repo",
		"Typesafe maven repository" at "http://repo.typesafe.com/typesafe/releases/",
		"Maven Repository" at "http://repo1.maven.org/maven2/", 
		Resolver.url("Sbt Plugins Artifactory", new URL("http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases/"))(Resolver.ivyStylePatterns),
		Resolver.url("Typesafe ivy repository", new URL("http://typesafe.artifactoryonline.com/typesafe/ivy-releases/"))(Resolver.defaultIvyPatterns)
)


addSbtPlugin("com.typesafe.sbt" % "sbt-atmos-play" % "0.3.2")

libraryDependencies ++= Seq(
     "org.jacoco" % "org.jacoco.core" % "0.5.9.201207300726" artifacts(Artifact("org.jacoco.core", "jar", "jar")),
     "org.jacoco" % "org.jacoco.report" % "0.5.9.201207300726" artifacts(Artifact("org.jacoco.report", "jar", "jar")))



addSbtPlugin("de.johoop" % "jacoco4sbt" % "2.1.0")

addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.7.5")


// The Play plugin
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.4.0")

// web plugins
addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.3")

addSbtPlugin("com.vmunier" % "sbt-play-scalajs" % "0.2.6")

addSbtPlugin("com.typesafe.sbt" % "sbt-gzip" % "1.0.0")

addSbtPlugin("com.typesafe.sbt" % "sbt-coffeescript" % "1.0.0")

addSbtPlugin("com.typesafe.sbt" % "sbt-less" % "1.0.0")

addSbtPlugin("com.typesafe.sbt" % "sbt-jshint" % "1.0.1")

addSbtPlugin("com.typesafe.sbt" % "sbt-rjs" % "1.0.1")

addSbtPlugin("com.typesafe.sbt" % "sbt-digest" % "1.0.0")

addSbtPlugin("com.typesafe.sbt" % "sbt-mocha" % "1.0.0")
