package scalafxml.subcut

import scala.reflect.runtime.{universe => ru}
import com.escalatesoft.subcut.inject.BindingModule

/** Helper class for [[scalafxml.subcut.SubCutDependencyResolver]] */
object SubCutHelper {

  /** Invokes dynamically the SubCut binding module's injectOptional method
    *
    * @param bindingModule  binding module to invoke
    * @param dependencyType type to pass to injectOptional as a type argument
    * @return returns the result of the invoked method
    */
  def injectOptional(bindingModule: BindingModule, dependencyType: ru.Type): Option[Any] = {
    import ru._

    val rm = runtimeMirror(bindingModule.getClass.getClassLoader)
    val instanceMirror = rm.reflect(bindingModule)
    val injectOptionalSymbols = typeOf[BindingModule].decl(TermName("injectOptional")).asTerm.alternatives
    val injectOptionalSym = injectOptionalSymbols.filter {
      case m: MethodSymbol => {
        m.paramLists.size == 2 &&
          m.paramLists(0).size == 1 &&
          m.paramLists(0)(0).typeSignature =:= typeOf[Option[String]]
      }
    }.head.asMethod

    val methodMirror = instanceMirror.reflectMethod(injectOptionalSym)
    methodMirror.apply(None, Manifest.classType(Class.forName(rm.runtimeClass(dependencyType).getName))).asInstanceOf[Option[Any]]
  }

}
