scalaVersion := "2.13.8"

name := "gam-report-sandbox"
version := "1.0"

libraryDependencies ++= Seq(
  "org.scala-lang.modules" %% "scala-parser-combinators" % "2.1.1",
  "com.google.api-ads" % "ads-lib" % "5.2.0",
  "com.google.api-ads" % "dfp-axis" % "5.2.0",
  "log4j" % "log4j" % "1.2.17",
  "ch.qos.logback" % "logback-classic" % "1.3.7" % Runtime,
)
