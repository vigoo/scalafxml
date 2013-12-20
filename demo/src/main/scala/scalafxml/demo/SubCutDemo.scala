package scalafxml.demo

import scalafx.application.JFXApp
import scalafx.Includes._
import scalafx.scene.Scene
import scalafxml.core.FXMLView
import scalafxml.subcut.SubCutDependencyResolver
import com.escalatesoft.subcut.inject.NewBindingModule.newBindingModule

object SubCutDemo extends JFXApp {

  implicit val bindingModule = newBindingModule(module => {
    import module._

    bind [TestDependency] toSingle(new TestDependency("subcut dependency"))
  })

  stage = new JFXApp.PrimaryStage() {
    title = "Hello world"
    scene = new Scene(FXMLView(getClass.getResource("startscreen.fxml"), new SubCutDependencyResolver()))
            
  }  
}