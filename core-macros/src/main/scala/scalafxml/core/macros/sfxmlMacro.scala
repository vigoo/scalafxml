package scalafxml.core.macros

import scala.language.experimental.macros
import scala.annotation.StaticAnnotation
import scala.reflect.macros.blackbox

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
class sfxml(additionalControls: List[String] = List.empty) extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro sfxmlMacro.impl
}

/** Annotates a controller constructor argument to treat it as a nested controller
  * @tparam Controller The nested controller's real (generated proxy) type (the argument type should be a trait implemented by
  *                    the inner controller)
  */
class nested[Controller] extends StaticAnnotation {

}

class TypeCheckHelper[T] {
}

/** Macro transformation implementation */
object sfxmlMacro {

  def impl(c: blackbox.Context)(annottees: c.Expr[Any]*): c.Expr[Any] = {

    import c.universe._

    sealed trait InputType
    case class WrapWithScalaFX(jfxType: Tree, sfxType: Tree) extends InputType
    case class UseJavaFX(jfxType: Tree) extends InputType
    case class NestedController(controllerType: Tree, interfaceType: Tree) extends InputType
    case object GetFromDependencies extends InputType

    /** Resolves a type tree to a type */
    def toType(t: Tree): Type = {
      val e = c.Expr[Any](c.typecheck(q"new scalafxml.core.macros.TypeCheckHelper[$t]"))
      e.actualType match {
        case TypeRef(_, _, params) => params.head
      }
    }

    def evalTree[T](tree: Tree) = c.eval(c.Expr[T](c.untypecheck(tree.duplicate)))

    def additionalControls: List[String] = {
      c.prefix.tree match {
        case q"new sfxml(additionalControls = $additionalControls)" =>
          evalTree[List[String]](additionalControls)
        case q"new sfxml($additionalControls)" =>
          evalTree[List[String]](additionalControls)
        case q"new sfxml()" =>
          List.empty
      }
    }

    def isNestedAnnotation(t: Tree): Option[Tree] = {
      t match {
        case Apply(Select(New(AppliedTypeTree(Ident(TypeName("nested")), List(controllerType))), termNames.CONSTRUCTOR), List()) =>
          Some(controllerType)
        case _ =>
          None
      }
    }

    def isFxmlAnnotation(t: Tree): Boolean = {
      t match {
        case Apply(Select(New(Ident(TypeName("FXML"))), termNames.CONSTRUCTOR), List()) =>
          true
        case _ =>
          false
      }
    }

    /** Determines whether an input type of the annotated constructor
      * needs to be wrapped or fetched from the dependency provider
      *
      * @param t type tree possibly representing a ScalaFX type
      * @return returns one of the cases of the InputType ADT
      */
    def determineInputType(t: Tree, modifiers: Modifiers): InputType = {
      val unknownType = toType(t)

      val nestedControllerType: Option[Tree] = modifiers.annotations.map(isNestedAnnotation(_)).flatMap(_.toList).headOption
      nestedControllerType match {
        case Some(controllerType) => NestedController(controllerType, t)
        case None =>

          val name = unknownType.typeSymbol.name
          val pkg = unknownType.typeSymbol.owner

          val controlPrefixes = "javafx." :: additionalControls

          // We simply replace the package to javafx from scalafx,
          // and keep everything else
          if (pkg.isPackageClass) {
            val pkgName = pkg.fullName
            if (pkgName.startsWith("scalafx.")) {

              val args = unknownType.asInstanceOf[TypeRefApi].args

              val jfxPkgName = pkgName.replaceFirst("scalafx.", "javafx.")
              val jfxClassName = s"$jfxPkgName.$name"
              val jfxClass = c.mirror.staticClass(jfxClassName)

              WrapWithScalaFX(tq"$jfxClass[..$args]", t)
            } else if (controlPrefixes.exists(prefix => pkgName.startsWith(prefix))) {
              // If it is already a JavaFX type, we leave it as it is
              UseJavaFX(t)
            } else {
              if (modifiers.annotations.exists(isFxmlAnnotation(_))) {
                UseJavaFX(t)
              } else {
                GetFromDependencies
              }
            }
          } else {
            GetFromDependencies // default: no conversion
          }
      }
    }

    /** Converts a ScalaFX type tree to JavaFX type tree, or keep it untouched
      *
      * @param unknownType a type tree possibly representing a ScalaFX type
      * @return a type tree which is either modified to be a JavaFX type, or is untouched
      */
    def toJavaFXTypeOrOriginal(unknownType: Tree, modifiers: Modifiers): Tree =
      determineInputType(unknownType, modifiers) match {
        case WrapWithScalaFX(jfxType, _) => jfxType
        case UseJavaFX(jfxType) => jfxType
        case NestedController(controllerType, _) => controllerType
        case GetFromDependencies => unknownType
      }

    /** Filters out empty elements from a list of AST */
    def nonEmpty(ls: List[Option[Tree]]): List[Tree] =
      ls.flatMap(_.toList)

    // Extracting the name, constructor arguments, base class and body
    // from the annotated class
    val q"class $name(...$argss) extends $baseClass with ..$traits { ..$body }" = annottees.map(_.tree).head

    /** Bindable public JavaFX variables for the proxy,
      * generated from the constructor arguments of the controller
      * which have a ScalaFX type
      */
    val jfxVariables = nonEmpty(argss.flatten.map {
      case ValDef(paramModifiers, paramName, paramType, _) =>
        determineInputType(paramType, paramModifiers) match {
          case WrapWithScalaFX(jfxType, _) => Some(q"@javafx.fxml.FXML var $paramName: $jfxType = null")
          case UseJavaFX(jfxType) => Some(q"@javafx.fxml.FXML var $paramName: $jfxType = null")
          case NestedController(controllerType, _) => Some(q"@javafx.fxml.FXML var $paramName: $controllerType = null")
          case GetFromDependencies => None
        }
      case p =>
        throw new Exception(s"Unknown parameter match: ${showRaw(p)}")
    })

    /** Event handler delegates for the proxy, converting from JavaFX event argument types
      * to ScalaFX event argument types
      */
    val eventHandlers = nonEmpty(body.map {
      case DefDef(methodMods, methodName, _, methodParams, methodReturnType, _) if !methodMods.hasFlag(Flag.PRIVATE) =>
        val methodArgs = methodParams.map(_.map {
          case ValDef(pmods, pname, ptype, pdef) => ValDef(pmods, pname, toJavaFXTypeOrOriginal(ptype, pmods), pdef)
        })
        val argInstances = methodParams.map(_.map {
          case ValDef(pmods, pname, ptype, _) =>
            determineInputType(ptype, pmods) match {
              case WrapWithScalaFX(_, sfxType) => q"new $sfxType($pname)"
              case UseJavaFX(_) => q"$pname"
              case NestedController(_, interfaceType) => q"$pname.as[$interfaceType]()"
              case GetFromDependencies => q"$pname"
            }
        })

        Some(
          q"""@javafx.fxml.FXML def ${methodName.toTermName}(...$methodArgs) {
		            impl.${methodName.toTermName}(...$argInstances)
	            }
          """)
      case _ => None
    })

    /** List of values to be passed to the controller's constructor */
    val constructorParams = argss.map(_.map {
      case ValDef(cParamModifiers, cParamName, cParamType, _) =>
        determineInputType(cParamType, cParamModifiers) match {
          case WrapWithScalaFX(_, sfxType) => q"new $sfxType($cParamName)"
          case UseJavaFX(_) => q"$cParamName"
          case NestedController(_, interfaceType) => q"$cParamName.as[$interfaceType]()"
          case GetFromDependencies => q"getDependency[$cParamType](${Literal(Constant(cParamName.decodedName.toString))})"
        }
    })

    /** List of calls to the dependency resolver passed to the proxy as a constructor
      * argument, to get the controller's dependencies and store them through the
      * ProxyDependencyInjection trait.
      */
    val injections = nonEmpty(argss.flatten.map {
      case ValDef(cParamModifiers, cParamName, cParamType, _) =>
        determineInputType(cParamType, cParamModifiers) match {
          case GetFromDependencies =>
            val nameLiteral = Literal(Constant(cParamName.decodedName.toString))
            val typeLiteral = q"scala.reflect.runtime.universe.typeOf[$cParamType]"
            Some(
              q"""dependencyResolver.get($nameLiteral, $typeLiteral) match {
                    case Some(value) => setDependency($nameLiteral, value)
                    case None =>
	  	            }
              """)
          case _ =>
            None
        }
      case x => throw new Exception(s"Invalid constructor argument $x")
    })

    /** AST of the proxy class */
    val proxyTree =
    q"""class $name(private val dependencyResolver: scalafxml.core.ControllerDependencyResolver) extends javafx.fxml.Initializable with scalafxml.core.FxmlProxyGenerator.ProxyDependencyInjection with scalafxml.core.ControllerAccessor {

          ..$injections

          class Controller(...$argss) extends $baseClass with ..$traits { ..$body }
          private var impl: Controller = null

          ..$jfxVariables

          def initialize(url: java.net.URL, rb: java.util.ResourceBundle) {
            impl = new Controller(...$constructorParams)
          }

          ..$eventHandlers

          def as[T](): T = impl.asInstanceOf[T]

	  	}"""

    // Returning the proxy class
    c.Expr[Any](proxyTree)
  }
}
