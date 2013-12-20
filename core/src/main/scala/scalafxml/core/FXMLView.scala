package scalafxml.core

import javafx.{fxml => jfxf}
import javafx.{util => jfxu}
import javafx.{scene => jfxs}
import java.net.URL

object FXMLView {
  
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