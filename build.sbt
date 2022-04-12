import sbt.Keys._
import sbt._
import xerial.sbt.Sonatype._

import scala.xml.transform.{RewriteRule, RuleTransformer}
import scala.xml.{Node => XmlNode, NodeSeq => XmlNodeSeq, _}

lazy val osName = System.getProperty("os.name") match {
  case n if n.startsWith("Linux") => "linux"
  case n if n.startsWith("Mac") => "mac"
  case n if n.startsWith("Windows") => "win"
  case _ => throw new Exception("Unknown platform!")
}

def isScala2_13plus(scalaVersion: String): Boolean = CrossVersion.partialVersion(scalaVersion) match {
  case Some((2, n)) if n >= 13 => true
  case _ => false
}

lazy val commonSettings =
  Seq(
    organization := "org.scalafx",
    version := "0.5",
    crossScalaVersions := Seq("2.13.6", "2.12.14", "2.11.12"),
    scalaVersion := crossScalaVersions.value.head,
    scalacOptions ++= Seq("-deprecation"),
    // If using Scala 2.13 or better, enable macro processing through compiler option
    scalacOptions += (if (isScala2_13plus(scalaVersion.value)) "-Ymacro-annotations" else ""),
    // If using Scala 2.12 or lower, enable macro processing through compiler plugin
    libraryDependencies ++= (
      if (!isScala2_13plus(scalaVersion.value))
        Seq(compilerPlugin(
          "org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full))
      else
        Seq.empty[sbt.ModuleID]
      ),
    resolvers += Resolver.sonatypeRepo("releases"),
    resolvers += Resolver.sonatypeRepo("snapshots"),
    libraryDependencies ++= Seq(
      "org.scalafx" %% "scalafx" % "15.0.1-R21",
      "org.scalatest" %% "scalatest" % "3.2.9" % "test"),

    // Add JavaFX dependencies, mark as "provided", so they can be later removed from published POM
    libraryDependencies ++= Seq("base", "controls", "fxml", "graphics", "media", "swing", "web").map(
      m => "org.openjfx" % s"javafx-$m" % "19-ea+5" % "provided" classifier osName),

    // Use `pomPostProcess` to remove dependencies marked as "provided" from publishing in POM
    // This is to avoid dependency on wrong OS version JavaFX libraries
    // See also [https://stackoverflow.com/questions/27835740/sbt-exclude-certain-dependency-only-during-publish]
    pomPostProcess := { node: XmlNode =>
      new RuleTransformer(new RewriteRule {
        override def transform(node: XmlNode): XmlNodeSeq = node match {
          case e: Elem if e.label == "dependency" && e.child.exists(c => c.label == "scope" && c.text == "provided") =>
            val organization = e.child.filter(_.label == "groupId").flatMap(_.text).mkString
            val artifact = e.child.filter(_.label == "artifactId").flatMap(_.text).mkString
            val version = e.child.filter(_.label == "version").flatMap(_.text).mkString
            Comment(s"provided dependency $organization#$artifact;$version has been omitted")
          case _ => node
        }
      }).transform(node).head
    },

    fork := true,
    exportJars := true,

    publishMavenStyle := true,
    publishTo := sonatypePublishTo.value,
    sonatypeProjectHosting := Some(GitHubHosting("vigoo", "scalafxml", "daniel.vigovszky@gmail.com")),

    licenses := Seq("APL2" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt")),

    developers := List(
      Developer(id="vigoo", name="Daniel Vigovszky", email="daniel.vigovszky@gmail.com", url=url("https://vigoo.github.io")),
      Developer(id="jpsacha", name="Jarek Sacha", email="", url=url("https://github.com/jpsacha"))
    )
  )

lazy val root: Project = Project("scalafxml-root", file(".")).settings(commonSettings).settings(
    run := (run in Compile in core).evaluated,
    publishArtifact := false
  ) aggregate(coreMacros, core, macwire, guice, demo)

lazy val core = Project("scalafxml-core-sfx8", file("core")).settings(commonSettings).settings(
    description := "ScalaFXML core module"
  )
  .dependsOn(coreMacros)

lazy val coreMacros = Project("scalafxml-core-macros-sfx8", file("core-macros")).settings(commonSettings).settings(
    description := "ScalaFXML macros"
)

lazy val guiceSettings = Seq(
  description := "Guice based dependency resolver for ScalaFXML",
  libraryDependencies += "com.google.inject" % "guice" % "5.0.1"
)

lazy val guice = Project("scalafxml-guice-sfx8", file("guice"))
  .settings(commonSettings)
  .settings(guiceSettings)
  .aggregate(core)
  .dependsOn(core)

lazy val macwireSettings = Seq(
  description := "MacWire based dependency resolver for ScalaFXML",
  libraryDependencies ++= Seq(
    "com.softwaremill.macwire" %% "macros" % "2.3.7" % "provided",
    "com.softwaremill.macwire" %% "util"   % "2.3.7",
    "com.softwaremill.macwire" %% "proxy"  % "2.3.7"
  )
)

lazy val macwire = Project("scalafxml-macwire-sfx8", file("macwire"))
  .settings(commonSettings)
  .settings(macwireSettings)
  .aggregate(core)
  .dependsOn(core)

lazy val demo = Project("scalafxml-demo-sfx8", file("demo"))
  .settings(commonSettings)
  .settings(
    description := "ScalaFXML demo applications",
    publishArtifact := false,
    libraryDependencies ++= Seq(
      "com.softwaremill.macwire" %% "macros" % "2.3.7" % "provided",
      "com.jfoenix" % "jfoenix" % "9.0.10"
    ),
    libraryDependencies ++= Seq("base", "controls", "fxml", "graphics", "media", "swing", "web").map(
      m => "org.openjfx" % s"javafx-$m" % "19-ea+5" classifier osName)
  )
  .dependsOn(core, guice, macwire)
