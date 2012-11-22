import sbt._
import sbt.Keys._
import xerial.sbt.Pack._

object Build extends sbt.Build {

  lazy val root = Project(
    id = "jnuma",
    base = file("."),
    settings = Defaults.defaultSettings ++ packSettings ++
      Seq(
        organization := "org.xerial",
        organizationName := "Xerial Project",
        organizationHomepage := Some(new URL("http://xerial.org/")),
        description := "A library for creating numa-aware buffer in Java/Scala",
        scalaVersion := "2.9.2",
        publishMavenStyle := true,
        publishArtifact in Test := false,
        crossPaths := false,
        // custom settings here
        crossPaths := false,
        libraryDependencies ++= Seq(
          // Add dependent jars here
          "org.xerial" % "xerial-core" % "3.0" % "test",
          "org.scalatest" %% "scalatest" % "1.8" % "test"
        )
      )
  )
}
