package scalafxml.core.macros

import scala.reflect.macros.Context
import scala.language.experimental.macros
import scala.annotation.StaticAnnotation

/** Annotates a class to generate a ScalaFXML controller around it
  * 
  * == Overview ==
  * The annotated class will be moved to an inner class in the generated
  * proxy, named Controller. The proxy gets the annotated class' name,
  * and will have a constructor receiving a [[scalafxml.core.ControllerDependencyResolver]].
  * It implements the [[javafx.fxml.Initializable]] interface.
  * 
  * The generated proxy has all the ScalaFX types from the original class' 
  * constructor as public JavaFX variables annotated with the  [[javafx.fxml.FXML]] attribute.
  * 
  * All the public methods of the controller are copied to the proxy, delegating the call
  * to the inner controller, converting JavaFX event arguments to ScalaFX event arguments.
  * 
  * The controller itself is instantiated in the proxy's initialize method.
  */
class sfxml extends StaticAnnotation {
	def macroTransform(annottees: Any*) = macro sfxmlMacro.impl
}

/** Macro transformation implementation */
object sfxmlMacro {

  def impl(c: Context)(annottees: c.Expr[Any]*): c.Expr[Any] = {

    import c.universe._

    /** Resolves a type tree to a type */
    def toType(t: Tree): Type = {
      val e = c.Expr[Any](c.typeCheck(q"null: $t"))
      e.actualType
    }

    /** Converts ScalaFX types to JavaFX types, if possible 
      * @param t type tree possibly representing a ScalaFX type
      * @return returns a modified type tree if it was a ScalaFX type, otherwise None
      */
    def toJavaFXType(t: Tree): Option[Tree] = {
      val scalaFxType = toType(t)

      val name = scalaFxType.typeSymbol.name
      val pkg = scalaFxType.typeSymbol.owner

      // We simply replace the package to javafx from scalafx,
      // and keep everything else
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

    /** Converts a ScalaFX type tree to JavaFX type tree, or keep it untouched 
      * @param scalaFxType a type tree possibly representing a ScalaFX type
      * @return a type tree which is either modified to be a JavaFX type, or is untouched
      */
    def toJavaFXTypeOrOriginal(scalaFxType: Tree): Tree =
      toJavaFXType(scalaFxType) match {
	case Some(t) => t
	case None => scalaFxType
      }

    /** Filters out empty elements from a list of AST */
    def nonEmpty(ls: List[Option[Tree]]): List[Tree] =
      ls.filter(!_.isEmpty).map(_.get)

    // Extracting th ename, constructor arguments, base class and body
    // from the annotated class
    val q"class $name(...$argss) extends $baseClass { ..$body }" = annottees.map(_.tree).toList(0)

    println(s"Compiling ScalaFXML proxy class for $name")

    /** Bindable public JavaFX variables for the proxy,
      * generated from the constructor arguments of the controller
      * which have a ScalaFX type
      */
    val jfxVariables = nonEmpty(argss.flatten.map {
      case ValDef(_, paramName, paramType, _) => {
	toJavaFXType(paramType) match {
	  case Some(jfxType) => Some(q"@javafx.fxml.FXML var $paramName: $jfxType = null")
	  case None => None
	}
      }
      case p => throw new Exception(s"Unknown parameter match: ${showRaw(p)}")
    })

    /** Event handler delegates for the proxy, converting from JavaFX event argument types
      *  to ScalaFX event argument types
      */
    val eventHandlers = nonEmpty(body.map(t => t match {
      case DefDef(methodMods, methodName, _, methodParams, methodReturnType, _) if !methodMods.hasFlag(Flag.PRIVATE) => {

        // ..we don't support multiple event handler parameter lists currently
	if (methodParams.length == 1) {
	  val methodArgs = methodParams(0).map { 
            case ValDef(pmods, pname, ptype, pdef) => ValDef(pmods, pname, toJavaFXTypeOrOriginal(ptype), pdef) }
	  val argInstances = methodParams(0).map { 
            case ValDef(_, pname, ptype, _) => q"new $ptype($pname)" }

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

    /** List of values to be passed to the controller's constructor */
    val constructorParams = argss.map(_.map {
      case ValDef(_, cParamName, cParamType, _) =>
	toJavaFXType(cParamType) match {
	  case Some(_) => q"new $cParamType($cParamName)"
	  case None => q"getDependency[$cParamType](${Literal(Constant(cParamName.decoded))})"
	}
    })

    /** List of calls to the dependency resolver passed to the proxy as a constructor
      * argument, to get the controller's dependencies and store them through the
      * ProxyDependencyInjection trait.
      */
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

    /** AST of the proxy class */
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

    // Returning the proxy class
    c.Expr[Any](proxyTree)
  }
}
