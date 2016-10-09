package scalafxml.core

import scala.reflect.runtime.universe.Type

/** Dependency resolver interface for controller proxies
  * 
  * The ScalaFXML controller classes use constructor injection to
  * get the FXML-bound controls and some additional dependencies.
  * These additional dependencies are resolved through this interface
  * at runtime.
  * 
  * The resolvers get both the constructor argument's name and its
  * type.
  */
trait ControllerDependencyResolver {

  /** Resolves a dependency
    * 
    * @param paramName name of the constructor argument
    * @param dependencyType type of the constructor argument
    * @return returns either some arbitrary value or none if it could not
    *         resolve the dependency.
    */ 
  def get(paramName: String, dependencyType: Type): Option[Any]
}

/** Default dependency resolver that does not resolve anything */
object NoDependencyResolver extends ControllerDependencyResolver {
  
  def get(paramName: String, dependencyType: Type): Option[Any] = None
}

/** Dependency resolver based on the constructor argument's names
  * 
  * @constructor creates a new dependency resolver based on a mapping
  * @param deps dependency mapping, from constructor argument names to values
  */
class ExplicitDependencies(deps: Map[String, Any]) extends ControllerDependencyResolver {
    def get(paramName: String, dependencyType: Type): Option[Any] = deps.get(paramName)
}

/** Dependency resolver based on the constructor argument's types
  * 
  * @constructor creates a new dependency resolver based on a mapping
  * @param deps dependency mapping, from dependency type to values
  */
class DependenciesByType(deps: Map[Type, Any]) extends ControllerDependencyResolver {
    def get(paramName: String, dependencyType: Type): Option[Any] = deps.get(dependencyType)
}
