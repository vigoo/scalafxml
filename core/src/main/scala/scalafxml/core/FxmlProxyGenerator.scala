package scalafxml.core

import scala.reflect.runtime.universe._
import scala.reflect.runtime.universe.Flag._

object FxmlProxyGenerator {
  
  trait ProxyDependencyInjection {
    
    val deps = scala.collection.mutable.Map[String, Any]()
    
    def setDependency(paramName: String, value: Any): Unit = {
      deps.put(paramName, value)
    }
    
    def getDependency[T](paramName: String): T = deps.getOrElse(paramName, null).asInstanceOf[T]
  }
 
  def apply(typ: Class[_], dependencyResolver: ControllerDependencyResolver = NoDependencyResolver): Object = {

    val proxy = typ.getConstructor(classOf[ControllerDependencyResolver]).newInstance(dependencyResolver)
    proxy.asInstanceOf[Object]
  }
}