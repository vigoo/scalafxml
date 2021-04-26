import sbt.Keys._
import sbt._
import sbt.internal.inc.ScalaInstance
import xerial.sbt.Sonatype._

import scala.xml.transform.{ RewriteRule, RuleTransformer }
import scala.xml.{ Node => XmlNode, NodeSeq => XmlNodeSeq, _ }

lazy val osName = System.getProperty("os.name") match {
  case n if n.startsWith("Linux")   => "linux"
  case n if n.startsWith("Mac")     => "mac"
  case n if n.startsWith("Windows") => "win"
  case _                            => throw new Exception("Unknown platform!")
}

def isScala2_13plus(scalaVersion: String): Boolean =
  CrossVersion.partialVersion(scalaVersion) match {
    case Some((2, n)) if n >= 13 => true
    case _                       => false
  }

val scala212 = "2.12.13"
val scala213 = "2.13.5"
val scala3 = "3.0.0-RC3"

lazy val commonSettings =
  Seq(
    organization           := "org.scalafx",
    version                := "2.0",
    scalaVersion           := scala212,
    scalacOptions ++= Seq("-deprecation"),
    // If using Scala 2.13 or better, enable macro processing through compiler option
    scalacOptions += (if (isScala2_13plus(scalaVersion.value)) "-Ymacro-annotations" else ""),
    // If using Scala 2.12 or lower, enable macro processing through compiler plugin
    libraryDependencies ++= (
      if (!isScala2_13plus(scalaVersion.value) && !ScalaInstance.isDotty(scalaVersion.value))
        Seq(compilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full))
      else
        Seq.empty[sbt.ModuleID]
    ),
    resolvers += Resolver.sonatypeRepo("snapshots"),
    libraryDependencies ++= Seq(
      "org.scalafx"   %% "scalafx"   % "16.0.0-R22",
      "org.scalatest" %% "scalatest" % "3.2.8" % "test"
    ),
    // Add JavaFX dependencies, mark as "provided", so they can be later removed from published POM
    libraryDependencies ++= Seq("base", "controls", "fxml", "graphics", "media", "swing", "web")
      .map(m => "org.openjfx" % s"javafx-$m" % "17-ea+7" % "provided" classifier osName),
    // Use `pomPostProcess` to remove dependencies marked as "provided" from publishing in POM
    // This is to avoid dependency on wrong OS version JavaFX libraries
    // See also [https://stackoverflow.com/questions/27835740/sbt-exclude-certain-dependency-only-during-publish]
    pomPostProcess         := { node: XmlNode =>
      new RuleTransformer(new RewriteRule {
        override def transform(node: XmlNode): XmlNodeSeq = node match {
          case e: Elem
              if e.label == "dependency" && e.child
                .exists(c => c.label == "scope" && c.text == "provided") =>
            val organization = e.child.filter(_.label == "groupId").flatMap(_.text).mkString
            val artifact = e.child.filter(_.label == "artifactId").flatMap(_.text).mkString
            val version = e.child.filter(_.label == "version").flatMap(_.text).mkString
            Comment(s"provided dependency $organization#$artifact;$version has been omitted")
          case _ => node
        }
      }).transform(node).head
    },
    fork                   := true,
    exportJars             := true,
    publishMavenStyle      := true,
    publishTo              := sonatypePublishTo.value,
    sonatypeProjectHosting := Some(
      GitHubHosting("vigoo", "scalafxml", "daniel.vigovszky@gmail.com")
    ),
    licenses               := Seq("APL2" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt")),
    developers             := List(
      Developer(
        id = "vigoo",
        name = "Daniel Vigovszky",
        email = "daniel.vigovszky@gmail.com",
        url = url("https://vigoo.github.io")
      ),
      Developer(
        id = "jpsacha",
        name = "Jarek Sacha",
        email = "",
        url = url("https://github.com/jpsacha")
      )
    )
  )

lazy val root2: Project = Project("scalafxml-root-2", file("."))
  .settings(
    run             := (core / Compile / run).evaluated,
    publishArtifact := false
  ) aggregate (coreMacros, core, sbtScalafxml, macwire, guice, demo)

lazy val root3: Project = Project("scalafxml-root-3", file("scala3"))
  .settings(
    run             := (core / Compile / run).evaluated,
    publishArtifact := false
  ) aggregate (core, sbtScalafxml, guice)

lazy val sbtScalafxml = Project("sbt-scalafxml", file("sbt-scalafxml"))
  .settings(commonSettings)
  .settings(
    sbtPlugin    := true,
    scalaVersion := scala212,
    crossVersion := CrossVersion.disabled,
    libraryDependencies ++= Seq(
      "org.scalameta" %% "scalameta"        % "4.3.21",
      "org.scalameta" %% "scalafmt-dynamic" % "2.7.5",
      "com.nrinaudo"  %% "kantan.xpath"     % "0.5.2"
    )
  )

lazy val core = Project("scalafxml-core", file("core"))
  .settings(commonSettings)
  .settings(
    description        := "ScalaFXML core module",
    crossScalaVersions := Seq(scala3, scala213, scala212)
  )

lazy val coreMacros = Project("scalafxml-core-macros", file("core-macros"))
  .settings(commonSettings)
  .settings(
    description        := "ScalaFXML macros",
    crossScalaVersions := Seq(scala213, scala212)
  )

lazy val guice = Project("scalafxml-guice", file("guice"))
  .settings(commonSettings)
  .settings(
    description                               := "Guice based dependency resolver for ScalaFXML",
    libraryDependencies += "com.google.inject" % "guice" % "5.0.1",
    crossScalaVersions                        := Seq(scala3, scala213, scala212)
  )
  .dependsOn(core)

lazy val macwire = Project("scalafxml-macwire", file("macwire"))
  .settings(commonSettings)
  .settings(
    description        := "MacWire based dependency resolver for ScalaFXML",
    crossScalaVersions := Seq(scala213, scala212),
    libraryDependencies ++= Seq(
      "com.softwaremill.macwire" %% "macros" % "2.3.7" % "provided",
      "com.softwaremill.macwire" %% "util"   % "2.3.7",
      "com.softwaremill.macwire" %% "proxy"  % "2.3.7"
    )
  )
  .dependsOn(core, coreMacros)

lazy val demo = Project("scalafxml-demo", file("demo"))
  .settings(commonSettings)
  .settings(
    description        := "ScalaFXML demo applications",
    crossScalaVersions := Seq(scala213, scala212),
    publishArtifact    := false,
    libraryDependencies ++= Seq(
      "com.softwaremill.macwire" %% "macros"  % "2.3.7" % "provided",
      "com.jfoenix"               % "jfoenix" % "9.0.10"
    ),
    libraryDependencies ++= Seq("base", "controls", "fxml", "graphics", "media", "swing", "web")
      .map(m => "org.openjfx" % s"javafx-$m" % "17-ea+7" classifier osName)
  )
  .dependsOn(core, coreMacros, guice, macwire)
