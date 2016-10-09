package scalafxml.demo

import scalafx.application.JFXApp
import scalafx.Includes._
import scalafx.scene.Scene
import scala.reflect.runtime.universe.typeOf
import scalafxml.core.{FXMLView, DependenciesByType}

object SimpleDemo extends JFXApp {

  val root = FXMLView(getClass.getResource("startscreen.fxml"),
    new DependenciesByType(Map(
      typeOf[TestDependency] -> new TestDependency("hello world"))))

  stage = new JFXApp.PrimaryStage() {
    title = "Hello world"
    scene = new Scene(root)

  }
}