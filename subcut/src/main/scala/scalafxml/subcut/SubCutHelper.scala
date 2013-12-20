package scalafxml.subcut;

import scala.reflect.runtime.{universe => ru}
import com.escalatesoft.subcut.inject.BindingModule

object SubCutHelper {

  def injectOptional(bindingModule: BindingModule, dependencyType: ru.Type): Option[Any] = {
    import ru._

    val rm = runtimeMirror(bindingModule.getClass.getClassLoader)
    val instanceMirror = rm.reflect(bindingModule)
    val injectOptionalSymbols = typeOf[BindingModule].declaration(newTermName("injectOptional")).asTerm.alternatives
    val injectOptionalSym = injectOptionalSymbols.filter {
      case m: MethodSymbol => {
                              m.paramss.size == 2 &&
                              m.paramss(0).size == 1 &&
                              m.paramss(0)(0).typeSignature =:= typeOf[Option[String]] }
    }.head.asMethod

    val methodMirror = instanceMirror.reflectMethod(injectOptionalSym)
    methodMirror.apply(None, Manifest.classType(Class.forName(rm.runtimeClass(dependencyType).getName))).asInstanceOf[Option[Any]]
  }

}
