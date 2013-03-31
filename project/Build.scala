import sbt._
import Keys._

/**
 * http://www.scala-sbt.org/release/docs/Getting-Started/Full-Def.html
 * https://github.com/harrah/xsbt/wiki/Full-Configuration
 * https://github.com/harrah/xsbt/wiki/Full-Configuration-Example
 * https://github.com/scalaz/scalaz/blob/master/project/ScalazBuild.scala
 */

object BuildSettings {
  val buildOrganization = "example.com"
  val buildVersion = "0.0.1-SNAPSHOT"
  val buildScalaVersion = "2.10.1"

  val buildSettings = Defaults.defaultSettings ++ Seq (
    organization := buildOrganization,
    version := buildVersion,
    scalaVersion := buildScalaVersion,
    shellPrompt  := ShellPrompt.buildShellPrompt,
    //crossScalaVersions := Seq("2.9.2", "2.10.0-M2")
    resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
  )
}

// Shell prompt which shows the current project, 
// git branch and build version
object ShellPrompt {
  object devnull extends ProcessLogger {
    def info (s: => String) {}
    def error (s: => String) { }
    def buffer[T] (f: => T): T = f
  }
  def currBranch = (
    ("git status -sb" lines_! devnull headOption)
      getOrElse "-" stripPrefix "## "
    )

  val buildShellPrompt = {
    (state: State) => {
      val currProject = Project.extract (state).currentProject.id
      "%s:%s:%s> ".format (
        currProject, currBranch, BuildSettings.buildVersion
      )
    }
  }
}

object Dependencies {
  object V {
    val lift = "2.5-SNAPSHOT"
    val logback = "1.0.11"
    val slf4j = "1.7.5"
    val specs2 = "1.14"
  }

  val lift = "net.liftweb" %% "lift-webkit" % V.lift withSources()

  val logbackcore    = "ch.qos.logback" % "logback-core"     % V.logback withSources()
  val logbackclassic = "ch.qos.logback" % "logback-classic"  % V.logback withSources()

  val slf4j = "org.slf4j" % "slf4j-api" % V.slf4j withSources()

  val specs2 = "org.specs2" %% "specs2" % V.specs2 % "test" withSources()
}

object LiftQuickstartBuild extends Build {
  import BuildSettings._
  import Dependencies._

  import com.github.siasia.PluginKeys._
  import fi.jawsy.sbtplugins.jrebel.JRebelPlugin._

  val commonDeps = Seq (
    slf4j,
    logbackcore,
    logbackclassic,
    specs2
  )

  val websiteDeps = Seq (
    lift
  )

  lazy val example = Project ("lift-quickstart-root", file ("."),
    settings = BuildSettings.buildSettings
  ) aggregate (backend, website)

  lazy val backend = Project ("lift-quickstart-backend", file ("backend"),
    settings = BuildSettings.buildSettings ++ Seq (libraryDependencies ++= commonDeps)
  )

  lazy val website = Project ("lift-quickstart-website", file("website"),
    settings = BuildSettings.buildSettings ++ Seq (libraryDependencies ++= commonDeps ++ websiteDeps ++ Seq(
      "org.eclipse.jetty" % "jetty-webapp" % "8.1.10.v20130312" % "container",
      //next line needed cause of https://github.com/harrah/xsbt/issues/499
      "org.eclipse.jetty.orbit" % "javax.servlet" % "3.0.0.v201112011016" % "container" artifacts (Artifact("javax.servlet", "jar", "jar"))
    )) ++ com.github.siasia.WebPlugin.webSettings ++ Seq(
      scanDirectories in Compile := Nil,
      port in config("container") := 8080
    ) ++ jrebelSettings ++ Seq (
      jrebel.webLinks <++= webappResources in Compile
    )
  ) dependsOn (backend)
}
