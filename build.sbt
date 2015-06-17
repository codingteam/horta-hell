name := "horta-hell"

version := "0.11"

mainClass in (Compile, run) := Some("ru.org.codingteam.horta.Application")

scalaVersion := "2.11.4"

scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature")

resolvers += Resolver.jcenterRepo

libraryDependencies ++= Seq(
  "org.scalikejdbc" %% "scalikejdbc" % "2.1.2",
  "org.scalatest" %% "scalatest" % "2.1.5" % "test",
  "com.googlecode.flyway" % "flyway-core" % "2.3",
  "me.fornever" %% "platonus" % "0.2.1",
  "com.typesafe.akka" %% "akka-actor" % "2.3.6",
  "com.typesafe.akka" %% "akka-slf4j" % "2.3.6",
  "com.typesafe.akka" %% "akka-testkit" % "2.3.6" % "test",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0",
  "ch.qos.logback" % "logback-classic" % "1.1.2",
  "org.slf4j" % "jcl-over-slf4j" % "1.7.7",
  "org.slf4j" % "log4j-over-slf4j" % "1.7.7",
  "org.igniterealtime.smack" % "smack" % "3.2.1",
  "org.igniterealtime.smack" % "smackx" % "3.2.1",
  "javax.transaction" % "jta" % "1.1",
  "com.h2database" % "h2" % "1.3.173",
  "joda-time" % "joda-time" % "2.3",
  "org.joda" % "joda-convert" % "1.5",
  "org.apache.commons" % "commons-lang3" % "3.0",
  "org.jsoup" % "jsoup" % "1.7.3",
  "io.spray" %%  "spray-json" % "1.3.1"
)