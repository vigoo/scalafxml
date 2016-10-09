package scalafxml.demo.nested

import scalafx.application.JFXApp
import scalafx.scene.Scene
import scalafx.Includes._
import scalafxml.core.{DependenciesByType, FXMLView}

object NestedDemo extends JFXApp {

  val root = FXMLView(getClass.getResource("window.fxml"),
    new DependenciesByType(Map.empty))

  stage = new JFXApp.PrimaryStage() {
    title = "Nested controllers demo"
    scene = new Scene(root)
  }
}