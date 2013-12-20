package scalafxml.demo

import scalafx.Includes._
import scalafx.application.JFXApp
import scalafx.scene.Scene
import scalafxml.core.FXMLView
import scalafxml.guice.GuiceDependencyResolver
import com.google.inject.{Guice, AbstractModule}

object GuiceDemo extends JFXApp {

  val module = new AbstractModule {
    def configure() {
      bind(classOf[TestDependency]).toInstance(new TestDependency("guice dependency"))
    }
  }
  implicit val injector = Guice.createInjector(module)

  stage = new JFXApp.PrimaryStage() {
    title = "Hello world"
    scene = new Scene(FXMLView(getClass.getResource("startscreen.fxml"), new GuiceDependencyResolver()))

  }
}