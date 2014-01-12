package scalafxml.demo.unitconverter

import scala.reflect.runtime.universe.typeOf
import scalafx.application.{Platform, JFXApp}
import scalafx.Includes._
import scalafx.scene.Scene
import scalafx.scene.control.{ComboBox, TextField}
import scalafx.event.ActionEvent
import scalafxml.core.{DependenciesByType, FXMLView}
import scalafxml.core.macros.sfxml
import javafx.beans.binding.StringBinding

@sfxml
class UnitConverterPresenter(
  private val from: TextField,
  private val to: TextField,
  private val types: ComboBox[UnitConverter],
  private val converters: UnitConverters) {

  // Filling the combo box
  for (converter <- converters.available) {
    types += converter
  }
  types.getSelectionModel.selectFirst()

  // Data binding
  to.text <== new StringBinding {
    bind(from.text.delegate, types.getSelectionModel.selectedItemProperty)
    def computeValue() = types.getSelectionModel.getSelectedItem.run(from.text.value)
  }

  // Close button event handler
  def onClose(event: ActionEvent) {
    Platform.exit()
  }
}

object ScalaFXML extends JFXApp {

  val root = FXMLView(getClass.getResource("unitconverter.fxml"),
    new DependenciesByType(Map(
      typeOf[UnitConverters] -> new UnitConverters(InchesToMM, MMtoInches))))

  stage = new JFXApp.PrimaryStage() {
    title = "Unit conversion"
    scene = new Scene(root)

  }
}
