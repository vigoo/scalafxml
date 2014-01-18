
import sbt._
import Keys._

object Build extends Build {

  lazy val commonSettings = Defaults.defaultSettings ++
    Seq(
      organization := "org.scalafxml",
      version := "0.1",
      scalaVersion := "2.10.3",
      resolvers += Resolver.sonatypeRepo("releases"),
      libraryDependencies ++= Seq(
	"org.scalafx" % "scalafx_2.10" % "1.0.0-M6",
	"org.scalatest" % "scalatest_2.10" % "2.0" % "test"),

      unmanagedJars in Compile += Attributed.blank(file(System.getenv("JAVA_HOME") + "/jre/lib/jfxrt.jar")),
      fork := true,
      exportJars := true,

      addCompilerPlugin("org.scalamacros" % "paradise" % "2.0.0-M1" cross CrossVersion.full)
    )

  lazy val root: Project = Project("root", file("."),
    settings = commonSettings ++ Seq(
      run <<= run in Compile in core
    )) aggregate("scalafxml-core-macros", "scalafxml-core", "scalafxml-subcut", "scalafxml-guice", "scalafxml-demo")

  lazy val core = Project("scalafxml-core", file("core"),
    settings = commonSettings ++ Seq(
      description := "ScalaFXML core module"
    ))
    .dependsOn(coreMacros)

  lazy val coreMacros = Project("scalafxml-core-macros", file("core-macros"),
    settings = commonSettings ++ Seq(
      description := "ScalaFXML macros",
      libraryDependencies <+= (scalaVersion)("org.scala-lang" % "scala-reflect" % _)
    ))

  lazy val subcutSettings = commonSettings ++ Seq(
    description := "SubCut based dependency resolver for ScalaFXML",
    libraryDependencies += "com.escalatesoft.subcut" % "subcut_2.10" % "2.0")

  lazy val subcut = Project("scalafxml-subcut", file("subcut"),
    settings = subcutSettings)
    .aggregate(core)
    .dependsOn(core)

  lazy val guiceSettings = commonSettings ++ Seq(
    description := "Guice based dependency resolver for ScalaFXML",
    libraryDependencies += "com.google.inject" % "guice" % "3.0"
  )

  lazy val guice = Project("scalafxml-guice", file("guice"),
    settings = guiceSettings)
    .aggregate(core)
    .dependsOn(core)

  lazy val demo = Project("scalafxml-demo", file("demo"),
    settings = subcutSettings ++ Seq(
      description := "ScalaFXML demo applications"
    ))
    .dependsOn(core, subcut, guice)

}
