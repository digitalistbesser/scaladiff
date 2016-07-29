//
// Global settings.
//
name := "scaladiff"

organization := "de.digitalistbesser"

version := "0.1-SNAPSHOT"

startYear := Some(2016)

scalaVersion := "2.11.8"

scalacOptions := Seq(
  "-encoding", "utf8",
  "-feature",
  "-unchecked",
  "-deprecation"
)

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "2.2.4" % "test"
)
