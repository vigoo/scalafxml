scalafxml
=========
[![Build Status](https://travis-ci.org/vigoo/scalafxml.svg?branch=master)](https://travis-ci.org/vigoo/scalafxml)


The [scalafx](https://code.google.com/p/scalafx/) library is a great UI DSL that wraps JavaFX classes and provides a nice syntax to work with them from Scala.

This library bridges [FXML](http://docs.oracle.com/javafx/2/fxml_get_started/why_use_fxml.htm) and [scalafx](https://code.google.com/p/scalafx/) by automatically building proxy classes, enabling a more clear controller syntax.

## Status
The `main` branch contains the initial implementation of the _compile time_ proxy generator, which uses [macro annotations](http://docs.scala-lang.org/overviews/macros/annotations.html). This requires the addition of the [macro paradise](http://docs.scala-lang.org/overviews/macros/paradise.html) compiler plugin, but has no runtime dependencies. It depends on [ScalaFX 8](https://github.com/scalafx/scalafx) and _JavaFX 8_.

The [`SFX-2`](https://github.com/vigoo/scalafxml/tree/SFX-2) branch is the _compile time_ proxy generator for [ScalaFX 2.2](https://github.com/scalafx/scalafx/tree/SFX-2) using _JavaFX 2_.

On the `dynamic` branch there is the first version of the proxy generator which executes runtime. This has a disadvantage of having `scala-compiler.jar` as a dependency, but has no special compile-time dependencies.

The latest published version is `0.2.2`. To use it in SBT add:

```scala
addCompilerPlugin("org.scalamacros" % "paradise" % "2.0.1" cross CrossVersion.full)

libraryDependencies += "org.scalafx" %% "scalafxml-core-sfx8" % "0.2.2"
```

## Example

The controller's, referenced from the FXML's through the `fx:controller` attribute, can be implemented as simple Scala classes, getting all the bound controls though the constructor:

```scala
import scalafx.scene.control.TextField
import scalafx.scene.control.Button
import scalafx.scene.control.ListView
import scalafx.event.ActionEvent
import scalafxml.core.macros.sfxml

@sfxml
class TestController(
    private val input: TextField,
    private val create: Button,
    private val recentInputs: ListView[String],
    private val dep: AnAdditionalDependency) {

	// event handlers are simple public methods:
	def onCreate(event: ActionEvent) {
		// ...
	}
}
```

Beside the JavaFX controls, additional dependencies can be injected to the controller as well. This injection process is extensible.

The following example uses [SubCut](https://github.com/dickwall/subcut) for injecting the additional dependency:

```scala
object SubCutDemo extends JFXApp {

  implicit val bindingModule = newBindingModule(module => {
    import module._

    bind [AnAdditionalDependency] toSingle(new AnAdditionalDependency("subcut dependency"))
  })

  stage = new JFXApp.PrimaryStage() {
    title = "Test window"
    scene = new Scene(
		FXMLView(
			getClass.getResource("test.fxml"),
			new SubCutDependencyResolver()))
  }
}
```

but it is also possible to simply give the dependencies _by their type_ or _by their name_:

```scala
object SimpleDemo extends JFXApp {

  stage = new JFXApp.PrimaryStage() {
    title = "Test window"
    scene = new Scene(
		FXMLView(getClass.getResource("test.fxml"),
		    new DependenciesByType(Map(
		      typeOf[AnAdditionalDependency] -> new AnAdditionalDependency("dependency by type"))))

  }
}
```

## Requirements
* `sbt 0.13` is required

## Related
* [Related blog post](http://vigoo.github.io/2014/01/12/scalafx-with-fxml.html) explaining how the library works.
