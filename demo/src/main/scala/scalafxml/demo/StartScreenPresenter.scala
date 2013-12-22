
package scalafxml.demo

import scalafx.scene.control.TextField
import scalafx.scene.control.Button
import scalafx.scene.control.ListView
import scalafx.event.ActionEvent
import scalafxml.core.macros.sfxml

case class TestDependency(val initialPath: String)

@sfxml
class StartScreenPresenter(
    private val newPhotoBookPath: TextField,
    private val btCreate: Button,
    private val recentPaths: ListView[String],
    private val testDep: TestDependency) {

  println(s"testDep is ${testDep}")

	newPhotoBookPath.text = testDep.initialPath

	def onBrowse(event: ActionEvent) {
		println(newPhotoBookPath.text)
		println("onBrowse")    
	}
  
	def onBrowseForOpen(event: ActionEvent) {
		println("onBrowseForOpen")
	}
 
	def onCreate(event: ActionEvent) {
		println("onCreate")
	} 
}