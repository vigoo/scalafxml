
import sbt._
import Keys._

object Build extends Build {

	lazy val commonSettings = Defaults.defaultSettings ++
		Seq(
			version := "0.1",
			scalaVersion := "2.10.3",
			libraryDependencies ++= Seq(
				"org.scalafx" % "scalafx_2.10" % "1.0.0-M6",
				"org.scala-lang" % "scala-compiler" % "2.10.3",
				"org.scalatest" % "scalatest_2.10" % "2.0" % "test"),

			unmanagedJars in Compile += Attributed.blank(file(System.getenv("JAVA_HOME") + "/jre/lib/jfxrt.jar")),
			fork := true			
		)

	lazy val core = Project("core", file("core"),
						settings = commonSettings)

  lazy val subcutSettings = commonSettings ++ Seq(
    libraryDependencies += "com.escalatesoft.subcut" % "subcut_2.10" % "2.0")

	lazy val subcut = Project("subcut", file("subcut"),
		settings = subcutSettings)
		.aggregate(core)
		.dependsOn(core)

  lazy val guiceSettings = commonSettings ++ Seq(
    libraryDependencies += "com.google.inject" % "guice" % "3.0"
  )

  lazy val guice = Project("guice", file("guice"),
    settings = guiceSettings)
    .aggregate(core)
    .dependsOn(core)

	lazy val demo = Project("demo", file("demo"),
    settings = subcutSettings)
		.aggregate(core, subcut, guice)
		.dependsOn(core, subcut, guice)

}