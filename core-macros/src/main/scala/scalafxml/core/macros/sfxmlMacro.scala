package scalafxml.core.macros

import scala.reflect.macros.Context
import scala.language.experimental.macros
import scala.annotation.StaticAnnotation

class sfxml extends StaticAnnotation {
	def macroTransform(annottees: Any*) = macro sfxmlMacro.impl
}

object sfxmlMacro {

  def impl(c: Context)(annottees: c.Expr[Any]*): c.Expr[Any] = {

  	  import c.universe._

  	  def toType(t: Tree): Type = {
		val e = c.Expr[Any](c.typeCheck(q"null: $t"))	  		
		e.actualType
  	  }

	  def toJavaFXType(t: Tree): Option[Tree] = {	  			  		  
	  	val scalaFxType = toType(t)

	    val name = scalaFxType.typeSymbol.name
	    val pkg = scalaFxType.typeSymbol.owner    
	    
	    if (pkg.isPackageClass) {
	      val pkgName = pkg.fullName
	      if (pkgName.startsWith("scalafx.")) {
	        
	        val args = scalaFxType.asInstanceOf[TypeRefApi].args
	        
	        val jfxPkgName = pkgName.replaceFirst("scalafx.", "javafx.")
	        val jfxClassName = s"$jfxPkgName.$name"
	        val jfxClass = c.mirror.staticClass(jfxClassName)
	        
	        return Some(tq"$jfxClass[..$args]")	        
	      }
	    }
	     
	    return None // default: no conversion    
	  }  	  

	  def toJavaFXTypeOrOriginal(scalaFxType: Tree): Tree = 
    	toJavaFXType(scalaFxType) match {
	    	case Some(t) => t
	    	case None => scalaFxType
  		}

  	  def nonEmpty(ls: List[Option[Tree]]): List[Tree] =
  	  	ls.filter(!_.isEmpty).map(_.get)

	  val q"class $name(...$argss) extends $baseClass { ..$body }" = annottees.map(_.tree).toList(0)

	  println(s"Compiling ScalaFXML proxy class for $name")
	  
	  val jfxVariables = nonEmpty(argss.flatten.map {
	  	case ValDef(_, paramName, paramType, _) => {	  	
	  		toJavaFXType(paramType) match {
	  			case Some(jfxType) => Some(q"@javafx.fxml.FXML var $paramName: $jfxType = null")	  			
	  			case None => None
	  		}
	  	}
	  	case p => throw new Exception(s"Unknown parameter match: ${showRaw(p)}")
	  })

	  val eventHandlers = nonEmpty(body.map(t => t match {
	  	case DefDef(methodMods, methodName, _, methodParams, methodReturnType, _) if !methodMods.hasFlag(Flag.PRIVATE) => { 

	  		if (methodParams.length == 1) {
		  		val methodArgs = methodParams(0).map { case ValDef(pmods, pname, ptype, pdef) => ValDef(pmods, pname, toJavaFXTypeOrOriginal(ptype), pdef) }
		  		val argInstances = methodParams(0).map { case ValDef(_, pname, ptype, _) => q"new $ptype($pname)" }

		  		Some(q"""@javafx.fxml.FXML def ${methodName.toTermName}(..$methodArgs) {
		  				impl.${methodName}(..$argInstances)
		  			}""")
		  	}		  	
		  	else {
		  		throw new Exception("Multiple parameter lists are not supported")
		  	}
		  }
	  	case _ => None
	  }))

	  val constructorParams = argss.map(_.map {
	  		case ValDef(_, cParamName, cParamType, _) => 
	  			toJavaFXType(cParamType) match {
	  				case Some(_) => q"new $cParamType($cParamName)"
	  				case None => q"getDependency[$cParamType](${Literal(Constant(cParamName.decoded))})"
	  			}
	  	})

	  val injections = nonEmpty(argss.flatten.map {
	  	case ValDef(_, cParamName, cParamType, _) => 
	  			toJavaFXType(cParamType) match {
	  				case Some(_) => None
	  				case None => {
	  					val nameLiteral = Literal(Constant(cParamName.decoded))
	  					val typeLiteral = q"scala.reflect.runtime.universe.typeOf[$cParamType]"
	  					Some(
	  					q"""dependencyResolver.get($nameLiteral, $typeLiteral) match {
	  							case Some(value) => setDependency($nameLiteral, value)
	  							case None => 
	  						}""")
	  				}
	  			}
	  	case x => throw new Exception(s"Invalid constructor argument $x")
	  })

	  val proxyTree = q"""class $name(private val dependencyResolver: scalafxml.core.ControllerDependencyResolver) extends javafx.fxml.Initializable with scalafxml.core.FxmlProxyGenerator.ProxyDependencyInjection {

	  		..$injections

	  		class Controller(...$argss) extends $baseClass { ..$body }
	  		private var impl: Controller = null	  		

	  		..$jfxVariables

	  		def initialize(url: java.net.URL, rb: java.util.ResourceBundle) {
	  			impl = new Controller(...$constructorParams)
	  		}

	  		..$eventHandlers

	  	}"""

	  println(proxyTree)
	  c.Expr[Any](proxyTree)
  }
}