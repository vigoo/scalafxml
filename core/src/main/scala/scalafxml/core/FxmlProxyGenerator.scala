package scalafxml.core

import scala.reflect.runtime.universe._
import scala.tools.reflect.ToolBox
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

    val tree = classdef(typ)
    val block =  Block(tree: _*)

    val cm = reflect.runtime.currentMirror
    val toolbox = cm.mkToolBox()
    val proxy = toolbox.eval(block).asInstanceOf[Object]
    val proxyDeps = proxy.asInstanceOf[ProxyDependencyInjection]   
    
    injectDependencies(typ, dependencyResolver, proxyDeps)
    
    proxy
  }
  
  private def injectDependencies(typ: Class[_], dependencyResolver: ControllerDependencyResolver, target: ProxyDependencyInjection) {
     val t = runtimeMirror(typ.getClassLoader()).classSymbol(typ).toType
     val ctr = t.declaration(nme.CONSTRUCTOR).asMethod
     val deps = ctr.paramss.flatten.map(v => (v.name -> v.typeSignature))
    
     deps.foreach {
     	case (name, scalaFxType) => {
        val javaFxType = toJavaFXType(scalaFxType)
        
	        javaFxType match {
	          case None => injectDependency(dependencyResolver, name.toString(), scalaFxType, target)
	          case Some(_) =>
	        }
     	}
     }
  }
  
  private def injectDependency(dependencyResolver: ControllerDependencyResolver, dependencyName: String, dependencyType: Type, target: ProxyDependencyInjection) {
    dependencyResolver.get(dependencyName, dependencyType) match {
      case Some(value) => target.setDependency(dependencyName, value)
      case None =>
    }
    
  }
            
  private def controllerType(typ: Class[_]): Tree =
    Ident(runtimeMirror(typ.getClassLoader()).classSymbol(typ))
      
  def toJavaFXType(scalaFxType: Type): Option[Type] = {
    val name = scalaFxType.typeSymbol.name
    val pkg = scalaFxType.typeSymbol.owner    
    
    if (pkg.isPackageClass) {
      val pkgName = pkg.fullName
      if (pkgName.startsWith("scalafx.")) {
        
        val args = scalaFxType.asInstanceOf[TypeRefApi].args
        
        val jfxPkgName = pkgName.replaceFirst("scalafx.", "javafx.")
        val jfxClassName = s"$jfxPkgName.$name"
        val jfxClass = runtimeMirror(getClass().getClassLoader()).staticClass(jfxClassName)
        
        return Some(appliedType(jfxClass.toTypeConstructor, args))        
      }
    }
     
    return None // default: no conversion    
  }
  
  def toJavaFXTypeOrOriginal(scalaFxType: Type): Type = 
    toJavaFXType(scalaFxType) match {
    	case Some(t) => t
    	case None => scalaFxType
  }
    
  private def toJavaFXParam(param: Symbol): ValDef = {
	  val term = param.asTerm
	  ValDef(Modifiers(PARAM), term.name.toTermName, asTypeTree(toJavaFXTypeOrOriginal(term.typeSignature)), EmptyTree)
  	}    
    
      
  private val scala_Unit = Select(Ident(newTermName("scala")), newTypeName("Unit"))
  private val scalafxml_core_FxmlProxyGenerator_ProxyDependencyInjection = Select(Select(Select(Ident(newTermName("scalafxml")), newTermName("core")), newTermName("FxmlProxyGenerator")), newTypeName("ProxyDependencyInjection"))
  private val java_net_URL = Select(Select(Ident(newTermName("java")), newTermName("net")), newTypeName("URL"))
  private val java_util_ResourceBundle = Select(Select(Ident(newTermName("java")), newTermName("util")), newTypeName("ResourceBundle"))
  private val javafx_fxml_Initializable = Select(Select(Ident(newTermName("javafx")), newTermName("fxml")), newTypeName("Initializable"))
  private val javafx_fxml_FXML = Select(Select(Ident(newTermName("javafx")), newTermName("fxml")), newTypeName("FXML"))  
  private val fxmlAttribute = List(Apply(Select(New(javafx_fxml_FXML), nme.CONSTRUCTOR), List()))
  
  private def asTypeTree(t: Type) = {
    val args = t.asInstanceOf[TypeRefApi].args
    if (args.isEmpty) {
      Ident(t.typeSymbol)
    } else {
	    AppliedTypeTree(        	      
	        Ident(t.typeSymbol), 
	        args.map(argt => Ident(argt.typeSymbol)))
    }
  }
    
  private def boundJFXVariables(typ: Class[_]): List[Tree] = {
    val t = runtimeMirror(typ.getClassLoader()).classSymbol(typ).toType
    val ctr = t.declaration(nme.CONSTRUCTOR).asMethod
    val deps = ctr.paramss.flatten.map(v => (v.name -> v.typeSignature))
    
    deps.map {
      case (name, scalaFxType) => {
        val javaFxType = toJavaFXType(scalaFxType)
        
        javaFxType match {
          case Some(jfxType) =>
	        ValDef(Modifiers(PRIVATE|MUTABLE,
	        				 tpnme.EMPTY,
	        				 fxmlAttribute),
	        	   name.toTermName,
	        	   asTypeTree(jfxType),        	   
	        	   Literal(Constant(null)))
          case None => EmptyTree
        }
      }
    }.toList
  }
  
  private def invokeControllerEventHandler(method: MethodSymbol): Tree = {
    Apply(
        Select(Ident(newTermName("impl")), 
        	   method.name),
        method.paramss.head.map(p =>
          	Apply(Select(New(asTypeTree(p.typeSignature)), nme.CONSTRUCTOR),
          	      List(Ident(p.name))))    	   
    )           
  }
    
  private def eventHandlers(typ: Class[_]): List[Tree] = {
    val t = runtimeMirror(typ.getClassLoader()).classSymbol(typ).toType
    val ctr = t.declaration(nme.CONSTRUCTOR).asMethod
    val publicMethods = t.declarations
    	.filter(m => m.isMethod && m.isPublic && m.name != nme.CONSTRUCTOR)
    	.map(m => m.asMethod)
		
	publicMethods.map {
      method => DefDef(
          Modifiers(NoFlags, tpnme.EMPTY, fxmlAttribute), 
          method.name.toTermName, 
          List(), // type parameters
          method.paramss.map(set => set.map(toJavaFXParam)), // method parameters
          scala_Unit, // type
          invokeControllerEventHandler(method)) // body
    }.toList	
  }   
        
  private def boundFieldsAsParameters(typ: Class[_]) = {
    val t = runtimeMirror(typ.getClassLoader()).classSymbol(typ).toType
    val ctr = t.declaration(nme.CONSTRUCTOR).asMethod
    val deps = ctr.paramss.flatten.map(v => (v.name -> v.typeSignature))
    
    deps.map {
      case (name, scalaFxType) => {
        
        toJavaFXType(scalaFxType) match {
          // It is a scalafx type => wrapping the bound field
          case Some(_) => Apply(Select(New(asTypeTree(scalaFxType)), nme.CONSTRUCTOR), List(Ident(name.toTermName)))
          
          // It is not a scalafx type => getting value from the dependency map
          case None => Apply(
              TypeApply(
            		  Ident(newTermName("getDependency")),
            		  List(asTypeTree(scalaFxType))), 
              List(Literal(Constant(name.toString))))
        }
      }
    }.toList
  }
    
  private def createController(typ: Class[_]) =    // 
    Apply(Select(New(controllerType(typ)), nme.CONSTRUCTOR),
    		boundFieldsAsParameters(typ))		
  
  private def classdef(typ: Class[_]): List[Tree] = List(
		  ValDef(Modifiers(), newTermName("proxy"), TypeTree(), Block(
		      List(ClassDef(Modifiers(Flag.FINAL), newTypeName("$proxy"), List(), 
		          Template(
		              List(javafx_fxml_Initializable, scalafxml_core_FxmlProxyGenerator_ProxyDependencyInjection),
		              emptyValDef,
		              boundJFXVariables(typ) ++
		              List(
		                  DefDef(Modifiers(), nme.CONSTRUCTOR, List(), List(List()), TypeTree(),
		                      Block(List(
		                          Apply(Select(Super(This(tpnme.EMPTY), tpnme.EMPTY), nme.CONSTRUCTOR), List())),
		                          Literal(Constant(())))),
		                  ValDef(Modifiers(PRIVATE|MUTABLE), newTermName("impl"), controllerType(typ), Literal(Constant(null))),
		                  DefDef(Modifiers(), newTermName("initialize"), List(), 
		                      List(List(ValDef(Modifiers(Flag.PARAM), newTermName("url"), java_net_URL, EmptyTree),
		                    		  	ValDef(Modifiers(Flag.PARAM), newTermName("rb"), java_util_ResourceBundle, EmptyTree))),
		                      scala_Unit,
		                      Assign(Ident(newTermName("impl")), createController(typ)))) ++
		              eventHandlers(typ)
		          ))),
		          Apply(Select(New(Ident(newTypeName("$proxy"))), nme.CONSTRUCTOR), List()))),
		   Ident(newTermName("proxy"))) 
}