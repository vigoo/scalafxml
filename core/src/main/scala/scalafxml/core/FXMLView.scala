package scalafxml.core

import javafx.{scene => jfxs}
import java.net.URL

/** Factory for FXML based views */
object FXMLView {

  /** Creates the JavaFX node representing the control described in FXML
    * 
    * @param fxml URL to the FXML to be loaded
    * @param dependencies dependency resolver for finding non-bound dependencies
    * @return the JavaFX node
    */
  def apply(fxml: URL, dependencies: ControllerDependencyResolver): jfxs.Parent = {
    val loader = new FXMLLoader(fxml, dependencies)
    loader.load()
    loader.getRoot[jfxs.Parent]()
  }
}
