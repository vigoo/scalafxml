package scalafxml.demo

import scalafx.application.JFXApp
import scalafx.scene.Scene
import scalafx.Includes._
import scalafxml.core.FXMLView
import scalafxml.macwire.MacWireDependencyResolver
import com.softwaremill.macwire.MacwireMacros._
import com.softwaremill.macwire.Wired

object MacWireDemo extends JFXApp {

  class Module {
    def testDependency = new TestDependency("MacWire dependency")
  }

  lazy val wired: Wired = wiredInModule(new Module)

  stage = new JFXApp.PrimaryStage() {
    title = "Hello world"
    scene = new Scene(FXMLView(getClass.getResource("startscreen.fxml"), new MacWireDependencyResolver(wired)))

  }
}
