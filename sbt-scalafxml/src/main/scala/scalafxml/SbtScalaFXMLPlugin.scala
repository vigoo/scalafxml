package scalafxml

import sbt.Keys._
import sbt._
import scalafxml.codegen.CodeGenerator

class SbtScalaFXMLPlugin extends AutoPlugin {
  object autoImport {
    lazy val generateSources =
      Def.task {
        val log = streams.value.log

        val fxmlResources = (Compile / resources).value.filter(_.getName.endsWith(".fxml"))
        val sourcesDir = (Compile / sourceManaged).value

        val cachedFun = FileFunction.cached(
          streams.value.cacheDirectory / "scalafxml"
        ) { input: Set[File] =>
          input.foldLeft(Set.empty[File]) { (result, fxml) =>
            val fs = CodeGenerator.generateSource(
                fxml,
                sourcesDir,
                log
              )
            result union fs.toSet
          }
        }

        cachedFun(fxmlResources.toSet).toSeq
      }
  }


  import autoImport._

  override def projectSettings: Seq[Def.Setting[_]] =
    Seq(
      Compile / sourceGenerators += generateSources.taskValue,
      Compile / packageSrc / mappings ++= {
        val base = (Compile / sourceManaged).value
        val files = (Compile / managedSources).value
        files.map(f => (f, f.relativeTo(base).get.getPath))
      }
    )
}
