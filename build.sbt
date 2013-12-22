name := "horta-hell"

version := "0.1-SNAPSHOT"

mainClass in (Compile, run) := Some("ru.org.codingteam.horta.Application")

scalaVersion := "2.10.3"

scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature")

resolvers ++= Seq(
  "clojars" at "http://clojars.org/repo/",
  "clojure-releases" at "http://build.clojure.org/releases"
)

libraryDependencies ++= Seq(
  "platonus" % "platonus" % "0.1.19",
  "com.typesafe.akka" % "akka-actor_2.10" % "2.2.3",
  "org.igniterealtime.smack" % "smack" % "3.2.1",
  "org.igniterealtime.smack" % "smackx" % "3.2.1",
  "javax.transaction" % "jta" % "1.1",
  "com.github.scala-incubator.io" %% "scala-io-core" % "0.4.2",
  "com.github.scala-incubator.io" %% "scala-io-file" % "0.4.2",
  "com.h2database" % "h2" % "1.3.173",
  "joda-time" % "joda-time" % "2.3",
  "org.joda" % "joda-convert" % "1.5"
)