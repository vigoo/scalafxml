package scalafxml.core

import java.net.URL
import java.util.ResourceBundle
import javafx.{fxml => jfxf}
import javafx.{util => jfxu}

/**
 * Extends the JavaFX [[javafx.fxml.FXMLLoader]] to support ScalaFXML controller classes
 *
 * The [[scalafxml.core.FXMLLoader.getController()]] method is overridden to work with
 * the original, wrapped controller instances.
 *
 * @param fxml URL to the FXML to be loaded
 * @param dependencies dependency resolver for finding non-bound dependencies
 * @param bundle optional bundle for localization resources
 */
class FXMLLoader(fxml: URL, dependencies: ControllerDependencyResolver, bundle : Option[ResourceBundle] = None)
  extends jfxf.FXMLLoader(
    fxml,
    bundle.orNull,
    new jfxf.JavaFXBuilderFactory(),
    new jfxu.Callback[Class[_], Object] {
      override def call(cls: Class[_]): Object =
        FxmlProxyGenerator(cls, dependencies)
    }) {

  override def getController[T](): T = super.getController[ControllerAccessor].as[T]()
}

