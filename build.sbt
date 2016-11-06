import sbt.Keys._
import sbt._
import xerial.sbt.Sonatype._

val jfxrtJar = file(System.getenv("JAVA_HOME") + "/jre/lib/ext/jfxrt.jar")
//println(s"Using JavaFX jar $jfxrtJar")

lazy val commonSettings =
  Seq(
    organization := "org.scalafx",
    version := "0.3",
    crossScalaVersions := Seq("2.11.8", "2.12.0"),
    scalaVersion := crossScalaVersions { versions => versions.head }.value,
    scalacOptions ++= Seq("-deprecation"),
    resolvers += Resolver.sonatypeRepo("releases"),
    libraryDependencies ++= Seq(
      "org.scalafx" %% "scalafx" % "8.0.102-R11",
      "org.scalatest" %% "scalatest" % "3.0.0" % "test"),

    unmanagedJars in Compile += Attributed.blank(jfxrtJar),
    fork := true,
    exportJars := true,

    addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full),

    pomExtra :=
      <url>https://github.com/vigoo/scalafxml</url>
        <scm>
          <url>github.com:vigoo/scalafxml.git</url>
          <connection>scm:git@github.com:vigoo/scalafxml.git</connection>
        </scm>
        <licenses>
          <license>
            <name>Apache 2</name>
            <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
          </license>
        </licenses>
        <developers>
          <developer>
            <id>vigoo</id>
            <name>Daniel Vigovszky</name>
            <url>https://github.com/vigoo</url>
          </developer>
          <developer>
            <id>jpsacha</id>
            <name>Jarek Sacha</name>
            <url>https://github.com/jpsacha</url>
          </developer>
        </developers>
  ) ++ sonatypeSettings

lazy val root: Project = Project("scalafxml-root", file("."),
  settings = commonSettings ++ Seq(
    run := (run in Compile in core).evaluated,
    publishArtifact := false
  )) aggregate(coreMacros, core, subcut, macwire, guice, demo)

lazy val core = Project("scalafxml-core-sfx8", file("core"),
  settings = commonSettings ++ Seq(
    description := "ScalaFXML core module"
  ))
  .dependsOn(coreMacros)

lazy val coreMacros = Project("scalafxml-core-macros-sfx8", file("core-macros"),
  settings = commonSettings ++ Seq(
    description := "ScalaFXML macros",
    libraryDependencies += scalaVersion("org.scala-lang" % "scala-reflect" % _).value
  ))

lazy val subcutSettings = commonSettings ++ Seq(
  description := "SubCut based dependency resolver for ScalaFXML",
  libraryDependencies += "com.escalatesoft.subcut" %% "subcut" % "2.1")

lazy val subcut = Project("scalafxml-subcut-sfx8", file("subcut"),
  settings = subcutSettings)
  .aggregate(core)
  .dependsOn(core)

lazy val guiceSettings = commonSettings ++ Seq(
  description := "Guice based dependency resolver for ScalaFXML",
  libraryDependencies += "com.google.inject" % "guice" % "4.1.0"
)

lazy val guice = Project("scalafxml-guice-sfx8", file("guice"),
  settings = guiceSettings)
  .aggregate(core)
  .dependsOn(core)

lazy val macwireSettings = commonSettings ++ Seq(
  description := "MacWire based dependency resolver for ScalaFXML",
  libraryDependencies ++= Seq(
    "com.softwaremill.macwire" %% "macros" % "2.2.5" % "provided",
    "com.softwaremill.macwire" %% "util"   % "2.2.5",
    "com.softwaremill.macwire" %% "proxy"  % "2.2.5"
  )
)

lazy val macwire = Project("scalafxml-macwire-sfx8", file("macwire"), settings = macwireSettings)
  .aggregate(core)
  .dependsOn(core)

lazy val demo = Project("scalafxml-demo-sfx8", file("demo"),
  settings = subcutSettings ++ Seq(
    description := "ScalaFXML demo applications",
    publishArtifact := false,
    libraryDependencies ++= Seq(
      "com.softwaremill.macwire" %% "macros" % "2.2.5" % "provided"
    )
  ))
  .dependsOn(core, subcut, guice, macwire)
