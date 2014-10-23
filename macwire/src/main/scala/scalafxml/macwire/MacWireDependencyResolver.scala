package scalafxml.macwire

import com.softwaremill.macwire.Wired
import scala.reflect.runtime.universe._
import scalafxml.core.ControllerDependencyResolver

/** MacWire based dependency resolver for ScalaFXML controllers */
class MacWireDependencyResolver(wired: Wired) extends ControllerDependencyResolver {

    def get(paramName: String, dependencyType: Type): Option[Any] = {
      val rm = runtimeMirror(getClass.getClassLoader)
      val cls = Class.forName(rm.runtimeClass(dependencyType).getName)
      try {
          Some(wired.lookupSingleOrThrow(cls))
      } catch {
        case _ : Throwable => None
      }
    }
}
