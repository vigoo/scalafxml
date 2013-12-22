package scalafxml.subcut

import com.escalatesoft.subcut.inject.BindingModule
import scala.reflect.runtime.universe.Type
import scalafxml.core.ControllerDependencyResolver
import scalafxml.subcut.SubCutHelper._

/** SubCut based dependency resolver for ScalaFXML controllers */
class SubCutDependencyResolver(implicit val bindingModule: BindingModule) extends ControllerDependencyResolver {

    def get(paramName: String, dependencyType: Type): Option[Any] = {
      injectOptional(bindingModule, dependencyType)
    }
}
