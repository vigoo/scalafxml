package scalafxml.codegen

import sbt.util.Logger

import java.io.File
import java.nio.file.Files

object CodeGenerator {
  def generateSource(fxmlSource: File, targetDir: File, log: Logger): Seq[File] = {
//    log.info(s"Generating ScalaFXML controller for $fxmlSource")

    Files.createDirectories(targetDir.toPath)
    val fxml = FXML.load(fxmlSource)
    println(fxml.controllerClass)
    println(fxml.includes)
    println(fxml.allIdentifiedComponents)

    Seq.empty // TODO
  }
}

object Test extends App {
  CodeGenerator.generateSource(
    new File("demo/src/main/resources/scalafxml/demo/thirdparty/unitconverter.fxml"),
    new File("target/test"),
    null
  )
}
