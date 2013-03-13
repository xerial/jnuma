import sbt._
import sbt.Keys._
import xerial.sbt.Pack._

object Build extends sbt.Build {

  private def profile = System.getProperty("xerial.profile", "default")

  def releaseResolver(v: String): Option[Resolver] = {
    profile match {
      case "default" => {
        val nexus = "https://oss.sonatype.org/"
        if (v.trim.endsWith("SNAPSHOT"))
          Some("snapshots" at nexus + "content/repositories/snapshots")
        else
          Some("releases" at nexus + "service/local/staging/deploy/maven2")
      }
      case p => {
        scala.Console.err.println("unknown xerial.profile:%s".format(p))
        None
      }
    }
  }

  val SCALA_VERSION = "2.9.2"

  lazy val defaultJavacOptions = Seq("-encoding", "UTF-8", "-source", "1.6")
  lazy val defaultScalacOptions = Seq("-encoding", "UTF-8", "-deprecation", "-unchecked", "-target:jvm-1.6")

  lazy val root = Project(
    id = "jnuma",
    base = file("."),
    settings = Defaults.defaultSettings ++ packSettings ++
      Seq(
        organization := "org.xerial",
        organizationName := "Xerial Project",
        organizationHomepage := Some(new URL("http://xerial.org/")),
        description := "A library for creating numa-aware buffer in Java/Scala",
        scalaVersion := SCALA_VERSION,
        publishMavenStyle := true,
        publishArtifact in Test := false,
        publishTo <<= version { (v) => releaseResolver(v) },
        pomIncludeRepository := { _ => false },
        // custom settings here
        crossPaths := false,
        libraryDependencies ++= Seq(
          // Add dependent jars here
          "org.xerial" % "xerial-core" % "3.0" % "test",
          "org.scalatest" %% "scalatest" % "1.8" % "test"
        ),
        javacOptions in Compile := defaultJavacOptions ++ Seq("-target", "1.6"),
        javacOptions in Compile in doc := defaultJavacOptions ++ Seq("-windowtitle", "xerial.jnuma API", "-linkoffline", "http://docs.oracle.com/javase/6/docs/api/", "http://docs.oracle.com/javase/6/docs/api/"),
//        scalacOptions in Compile := defaultScalacOptions,
//        scalacOptions in doc <++= (baseDirectory in LocalProject("jnuma"), version) map { (bd, v) =>
//          val tree = if(v.endsWith("-SNAPSHOT")) "develop" else "master"
//          Seq(
//            "-sourcepath", bd.getAbsolutePath,
//            "-doctitle", "Xerial JNuma Version %s".format(v)
//          )
//        },
        pomExtra := {
          <url>http://xerial.org/</url>
            <licenses>
              <license>
                <name>Apache 2</name>
                <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
              </license>
            </licenses>
            <scm>
              <connection>scm:git:github.com/xerial/jnuma.git</connection>
              <developerConnection>scm:git:git@github.com:xerial/jnuma.git</developerConnection>
              <url>github.com/xerial/jnuma.git</url>
            </scm>
            <properties>
              <scala.version>
                {SCALA_VERSION}
              </scala.version>
              <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
            </properties>
            <developers>
              <developer>
                <id>leo</id>
                <name>Taro L. Saito</name>
                <url>http://xerial.org/leo</url>
              </developer>
            </developers>
        }

      )
  )
}
