
import sbt._
import Keys._

object Build extends Build {

	lazy val commonSettings = Defaults.defaultSettings ++
		Seq(
			version := "0.1",
			scalaVersion := "2.10.3",
      		resolvers += Resolver.sonatypeRepo("releases"),
			libraryDependencies ++= Seq(
				"org.scalafx" % "scalafx_2.10" % "1.0.0-M6",
				"org.scalatest" % "scalatest_2.10" % "2.0" % "test"),

			unmanagedJars in Compile += Attributed.blank(file(System.getenv("JAVA_HOME") + "/jre/lib/jfxrt.jar")),
			fork := true,

      		addCompilerPlugin("org.scalamacros" % "paradise" % "2.0.0-M1" cross CrossVersion.full)
		)

  	lazy val root: Project = Project("root", file("."),
  								settings = commonSettings ++ Seq(
      								run <<= run in Compile in core
    						)) aggregate("core-macros", "core", "subcut", "guice", "demo")

	lazy val core = Project("core", file("core"),
						settings = commonSettings)
    				.dependsOn(coreMacros)

  	lazy val coreMacros = Project("core-macros", file("core-macros"),
            			settings = commonSettings ++ Seq(
              				libraryDependencies <+= (scalaVersion)("org.scala-lang" % "scala-reflect" % _)
            			))

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
					.dependsOn(core, subcut, guice)

}