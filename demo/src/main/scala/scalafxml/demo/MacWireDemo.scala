package scalafxml.demo

import java.util.{MissingResourceException, ResourceBundle}

import scalafx.application.JFXApp
import scalafx.scene.Scene
import scalafx.Includes._
import scalafxml.core.FXMLView
import scalafxml.macwire.MacWireDependencyResolver
import com.softwaremill.macwire._

object MacWireDemo extends JFXApp {

  class Module {
    def testDependency = TestDependency("MacWire dependency")
  }

  lazy val wired: Wired = wiredInModule(new Module)

  stage = new JFXApp.PrimaryStage() {
    val resourceBundle = ResourceBundle.getBundle("scalafxml.demo.Localization")

    title = resourceBundle.getString("TEXT_HELLO_WORLD")
    scene = new Scene(FXMLView(getClass.getResource("startscreen.fxml"), new MacWireDependencyResolver(wired), Some(resourceBundle)))

  }
}
