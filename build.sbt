name := "horta-hell"

version := "0.9"

mainClass in (Compile, run) := Some("ru.org.codingteam.horta.Application")

scalaVersion := "2.10.3"

scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature")

resolvers ++= Seq(
  "codingteam" at "http://fornever.me:18080/repository/codingteam",
  "codingteam-snapshots" at "http://fornever.me:18080/repository/codingteam-snapshots"
)

libraryDependencies ++= Seq(
  "com.googlecode.flyway" % "flyway-core" % "2.3",
  "me.fornever" %% "platonus" % "0.2-SNAPSHOT" changing(),
  "com.typesafe.akka" % "akka-actor_2.10" % "2.2.3",
  "org.igniterealtime.smack" % "smack" % "3.2.1",
  "org.igniterealtime.smack" % "smackx" % "3.2.1",
  "javax.transaction" % "jta" % "1.1",
  "com.github.scala-incubator.io" %% "scala-io-core" % "0.4.2",
  "com.github.scala-incubator.io" %% "scala-io-file" % "0.4.2",
  "com.h2database" % "h2" % "1.3.173",
  "joda-time" % "joda-time" % "2.3",
  "org.joda" % "joda-convert" % "1.5",
  "org.apache.commons" % "commons-lang3" % "3.0",
  "org.jsoup" % "jsoup" % "1.7.3"
)

com.github.retronym.SbtOneJar.oneJarSettings