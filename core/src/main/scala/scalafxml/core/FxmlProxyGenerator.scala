package scalafxml.core

import scala.reflect.runtime.universe._
import scala.reflect.runtime.universe.Flag._

/** Proxy generator for FXML controllers
  * 
  * In the dynamic proxy version this object was responsible
  * for generating the proxy. In this static, compile-time
  * version its purpose is to pass the dependency resolver
  * to the dynamically instantiated controller class.
  */
object FxmlProxyGenerator {
  
  /** Trait implemented by the generated proxies for supporting dependency injection */
  trait ProxyDependencyInjection {
    
    /** Mutable map for storing a name->value pairs of dependencies */
    val deps = scala.collection.mutable.Map[String, Any]()
    
    /** Injects a dependency
      * 
      * @param paramName name of the constructor argument in the controller
      * @param value value to inject to the controller
      */
    def setDependency(paramName: String, value: Any): Unit = {
      deps.put(paramName, value)
    }
    
    /** Gets an injected dependency
      * 
      * @param paramName name of the constructor argument in the controller
      * @return the injected value or null
      */
    def getDependency[T](paramName: String): T = deps.getOrElse(paramName, null).asInstanceOf[T]
  }
 
  /** Creates a new controller proxy instance
    * 
    * @param typ type of the controller class (coming from the FXML)
    * @param dependencyResolver dependency resolver for finding non-FXML-bound  controller dependencies
    * @return returns the proxy for the controller
    */
  def apply(typ: Class[_], dependencyResolver: ControllerDependencyResolver = NoDependencyResolver): Object = {

    val proxy = typ.getConstructor(classOf[ControllerDependencyResolver]).newInstance(dependencyResolver)
    proxy.asInstanceOf[Object]
  }
}
