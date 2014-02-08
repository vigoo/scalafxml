package scalafxml.demo.getcontroller


import scalafx.application.{Platform, JFXApp}
import scalafx.Includes._
import scalafxml.core.macros.sfxml
import scalafx.scene.Scene
import scala.reflect.runtime.universe.typeOf
import scalafxml.core.DependenciesByType
import scalafxml.core.FXMLLoader
import scalafxml.demo.unitconverter.{MMtoInches, InchesToMM, UnitConverters, UnitConverter}
import scalafx.scene.control.{ComboBox, TextField}
import javafx.beans.binding.StringBinding
import scalafx.event.ActionEvent
import javafx.{scene => jfxs}

/**
 * Public interface of our controller which will be available through FXMLLoader
 */
trait UnitConverterInterface {
  def setInitialValue(value: Double)
}

/** Our controller class, implements UnitConverterInterface */
@sfxml
class UnitConverterPresenter(
                              private val from: TextField,
                              private val to: TextField,
                              private val types: ComboBox[UnitConverter],
                              private val converters: UnitConverters)
  extends UnitConverterInterface {

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

  def setInitialValue(value: Double) {
    from.text = value.toString
  }
}


object GetControllerDemo extends JFXApp {

  // Instead of FXMLView, we create a new ScalaFXML loader
  val loader = new FXMLLoader(getClass.getResource("unitconverter.fxml"),
    new DependenciesByType(Map(
      typeOf[UnitConverters] -> new UnitConverters(InchesToMM, MMtoInches))))

  // Load the FXML, the controller will be instantiated
  loader.load()

  // Get the scene root
  val root = loader.getRoot[jfxs.Parent]

  // Get the controller. We cannot use the controller class itself here,
  // because it is transformed by the macro - but we can use the trait it
  // implements!
  val controller = loader.getController[UnitConverterInterface]
  controller.setInitialValue(10)

  stage = new JFXApp.PrimaryStage() {
    title = "Unit converter"
    scene = new Scene(root)
  }
}