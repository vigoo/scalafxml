resolvers += "sonatype-releases" at "https://oss.sonatype.org/content/repositories/releases/"

addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "1.0.0")
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "3.6")
addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.1.2")
addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.5.2")

