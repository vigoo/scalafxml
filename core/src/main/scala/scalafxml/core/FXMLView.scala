package scalafxml.core

import javafx.{fxml => jfxf}
import javafx.{util => jfxu}
import javafx.{scene => jfxs}
import java.net.URL

/** Factory for FXML based views */
object FXMLView {

  /** Creates the JavaFX node representing the control described in FXML
    * 
    * @param fxml URL to the FXML to be loaded
    * @param dependencies dependency resolver for finding non-bound dependencies
    * @return the JavaFX node
  def apply(fxml: URL, dependencies: ControllerDependencyResolver): jfxs.Parent =
    jfxf.FXMLLoader.load(
      fxml, 
      null,
      new jfxf.JavaFXBuilderFactory(),
      new jfxu.Callback[Class[_], Object] {
        override def call(cls: Class[_]): Object = 
            FxmlProxyGenerator(cls, dependencies)
      })
}
