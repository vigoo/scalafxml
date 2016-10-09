package scalafxml.demo.nested

import scalafx.scene.layout.VBox
import scalafxml.core.macros.{nested, sfxml}

@sfxml
class WindowController(nested: VBox,
                       @nested[NestedController] nestedController: NestedControllerInterface) {

  println(s"Window controller initialized with nested control $nested and controller $nestedController")
  nestedController.doSomething()
}
