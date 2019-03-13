import sbt.Keys._
import sbt._
import xerial.sbt.Sonatype._

val jfxrtJar = file(System.getenv("JAVA_HOME") + "/jre/lib/ext/jfxrt.jar")

lazy val commonSettings =
  Seq(
    organization := "org.scalafx",
    version := "0.5",
    //crossScalaVersions := Seq("2.11.8", "2.12.8"),
    scalaVersion := "2.12.8", //crossScalaVersions { versions => versions.head }.value,
    scalacOptions ++= Seq("-deprecation"),
    resolvers += Resolver.sonatypeRepo("releases"),
    libraryDependencies ++= Seq(
      "org.scalafx" %% "scalafx" % "11-R16",
      "org.scalatest" %% "scalatest" % "3.0.5" % "test"),

    unmanagedJars in Compile += Attributed.blank(jfxrtJar),
    fork := true,
    exportJars := true,

    addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full),

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
  libraryDependencies += "com.google.inject" % "guice" % "4.2.2"
)

lazy val guice = Project("scalafxml-guice-sfx8", file("guice"))
  .settings(commonSettings)
  .settings(guiceSettings)
  .aggregate(core)
  .dependsOn(core)

lazy val macwireSettings = Seq(
  description := "MacWire based dependency resolver for ScalaFXML",
  libraryDependencies ++= Seq(
    "com.softwaremill.macwire" %% "macros" % "2.3.1" % "provided",
    "com.softwaremill.macwire" %% "util"   % "2.3.1",
    "com.softwaremill.macwire" %% "proxy"  % "2.3.1"
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
      "com.softwaremill.macwire" %% "macros" % "2.3.1" % "provided",
      "com.jfoenix" % "jfoenix" % "9.0.8"
    )
  )
  .dependsOn(core, guice, macwire)
