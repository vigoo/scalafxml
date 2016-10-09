package scalafxml.demo.nested

import scalafx.scene.control.Label
import scalafxml.core.macros.sfxml

trait NestedControllerInterface {
  def doSomething(): Unit
}

@sfxml
class NestedController(label: Label) extends NestedControllerInterface {

  println(s"Nested controller initialized with label: $label")

  override def doSomething(): Unit = {
    label.text = "Nested controller called!"
    println("Nested controller called")
  }
}
