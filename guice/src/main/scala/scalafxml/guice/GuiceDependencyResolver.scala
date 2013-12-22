package scalafxml.guice

import scala.reflect.runtime.universe._
import scalafxml.core.ControllerDependencyResolver
import com.google.inject.Injector

/** Guice based dependency resolver for ScalaFXML controllers */
class GuiceDependencyResolver(implicit val injector: Injector) extends ControllerDependencyResolver {

  def get(paramName: String, dependencyType: Type): Option[Any] = {
    val rm = runtimeMirror(getClass.getClassLoader)
    val cls = Class.forName(rm.runtimeClass(dependencyType).getName)
    try {
      Some(injector.getInstance(cls))
    } catch {
      case _ : Throwable => None
    }
  }
}
