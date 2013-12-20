package scalafxml.core

import scala.reflect.runtime.universe.Type

trait ControllerDependencyResolver {

  def get(paramName: String, dependencyType: Type): Option[Any]
}

object NoDependencyResolver extends ControllerDependencyResolver {
  
  def get(paramName: String, dependencyType: Type): Option[Any] = None
}

class ExplicitDependencies(deps: Map[String, Any]) extends ControllerDependencyResolver {
    def get(paramName: String, dependencyType: Type): Option[Any] = deps.get(paramName)
}

class DependenciesByType(deps: Map[Type, Any]) extends ControllerDependencyResolver {
    def get(paramName: String, dependencyType: Type): Option[Any] = deps.get(dependencyType)
}