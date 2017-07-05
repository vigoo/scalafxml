package scalafxml.demo

import java.util.{Locale, MissingResourceException, ResourceBundle}

import scalafx.Includes._
import scalafx.application.JFXApp
import scalafx.scene.Scene
import scalafxml.core.FXMLView
import scalafxml.guice.GuiceDependencyResolver
import com.google.inject.{AbstractModule, Guice}

object GuiceDemo extends JFXApp {

  val module = new AbstractModule {
    def configure() {
      bind(classOf[TestDependency]).toInstance(new TestDependency("guice dependency"))
    }
  }
  implicit val injector = Guice.createInjector(module)

  stage = new JFXApp.PrimaryStage() {
    val resourceBundle = ResourceBundle.getBundle("scalafxml.demo.Localization")
    title = resourceBundle.getString("TEXT_HELLO_WORLD")

    scene = new Scene(FXMLView(getClass.getResource("startscreen.fxml"), new GuiceDependencyResolver(), Some(resourceBundle)))

  }
}