package scalafxml.demo.thirdparty

import scala.reflect.runtime.universe.typeOf
import scalafx.application.{JFXApp, Platform}
import scalafx.Includes._
import scalafx.scene.Scene
import scalafx.scene.control.{ComboBox, TextField}
import scalafx.event.ActionEvent
import scalafxml.core.{DependenciesByType, FXMLView}
import scalafxml.core.macros.sfxml
import javafx.beans.binding.StringBinding
import javafx.fxml.FXML

import com.jfoenix.controls.JFXTextField

import scalafxml.demo.unitconverter.{InchesToMM, MMtoInches, UnitConverter, UnitConverters}

//@sfxml
//class UnitConverterPresenter(@FXML from: JFXTextField,
//                             @FXML to: JFXTextField,
//                             types: ComboBox[UnitConverter],
//                             converters: UnitConverters) {


@sfxml(additionalControls=List("com.jfoenix.controls"))
class UnitConverterPresenter(from: JFXTextField,
                             to: JFXTextField,
                             types: ComboBox[UnitConverter],
                             converters: UnitConverters) {

  // Filling the combo box
  for (converter <- converters.available) {
    types += converter
  }
  types.getSelectionModel.selectFirst()

  // Data binding
  to.text <== new StringBinding {
    bind(from.text.delegate, types.getSelectionModel.selectedItemProperty)

    def computeValue(): String = types.getSelectionModel.getSelectedItem.run(from.text.value)
  }

  // Close button event handler
  def onClose(event: ActionEvent): Unit = {
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
