package scalafxml.demo

import java.util.{Locale, MissingResourceException, ResourceBundle}

import scalafx.application.JFXApp
import scalafx.Includes._
import scalafx.scene.Scene
import scala.reflect.runtime.universe.typeOf
import scalafxml.core.{DependenciesByType, FXMLView}

object SimpleDemo extends JFXApp {
  val resourceBundle = ResourceBundle.getBundle("scalafxml.demo.Localization", new Locale("sv", "SE"))

  val root = FXMLView(getClass.getResource("startscreen.fxml"),
    new DependenciesByType(Map(
      typeOf[TestDependency] -> new TestDependency("hello world"))),
    Some(resourceBundle))

  stage = new JFXApp.PrimaryStage() {
    title = resourceBundle.getString("TEXT_HELLO_WORLD")
    scene = new Scene(root)

  }
}