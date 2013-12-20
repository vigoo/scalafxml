package test

import org.scalatest._
import scala.reflect.runtime.universe._
import scalafxml.core.FxmlProxyGenerator
import FxmlProxyGenerator._

class ProxyGeneratorTest extends FlatSpec with Matchers {

  "toJavaFXType" should " return its input if it is not a ScalaFX type" in {
    
    toJavaFXTypeOrOriginal(typeOf[String]) should be (typeOf[String])
    toJavaFXTypeOrOriginal(typeOf[List[String]]) should be (typeOf[List[String]])
    toJavaFXTypeOrOriginal(typeOf[org.scalatest.FlatSpec]) should be (typeOf[org.scalatest.FlatSpec])
  }
  
  it should " convert ScalaFX controls to JavaFX controls" in {
    
    assert(toJavaFXTypeOrOriginal(typeOf[scalafx.scene.control.Button]) =:= typeOf[javafx.scene.control.Button])
    assert(toJavaFXTypeOrOriginal(typeOf[scalafx.scene.control.TextField]) =:= typeOf[javafx.scene.control.TextField])
    assert(toJavaFXTypeOrOriginal(typeOf[scalafx.scene.control.ListView[String]]) =:= typeOf[javafx.scene.control.ListView[String]])
  }
  

  // TODO: test generated proxies
}