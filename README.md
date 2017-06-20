scalafxml
=========
[![Build Status](https://travis-ci.org/vigoo/scalafxml.svg?branch=master)](https://travis-ci.org/vigoo/scalafxml)


The [scalafx](http://www.scalafx.org/) library is a great UI DSL that wraps JavaFX classes and provides a nice syntax to work with them from Scala.

This library bridges [FXML](http://docs.oracle.com/javafx/2/fxml_get_started/why_use_fxml.htm) and [scalafx](https://code.google.com/p/scalafx/) by automatically building proxy classes, enabling a more clear controller syntax.

## Status
The `main` branch contains the initial implementation of the _compile time_ proxy generator, which uses [macro annotations](http://docs.scala-lang.org/overviews/macros/annotations.html). This requires the addition of the [macro paradise](http://docs.scala-lang.org/overviews/macros/paradise.html) compiler plugin, but has no runtime dependencies. It depends on [ScalaFX 8](https://github.com/scalafx/scalafx) and _JavaFX 8_.

The [`SFX-2`](https://github.com/vigoo/scalafxml/tree/SFX-2) branch is the _compile time_ proxy generator for [ScalaFX 2.2](https://github.com/scalafx/scalafx/tree/SFX-2) using _JavaFX 2_.

On the `dynamic` branch there is the first version of the proxy generator which executes runtime. This has a disadvantage of having `scala-compiler.jar` as a dependency, but has no special compile-time dependencies.

The latest published version is `0.3`. To use it in SBT add:

```scala
addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)

libraryDependencies += "org.scalafx" %% "scalafxml-core-sfx8" % "0.3"
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
class TestController(input: TextField,
                     create: Button,
                     recentInputs: ListView[String],
                     dep: AnAdditionalDependency) {

	// event handlers are simple public methods:
	def onCreate(event: ActionEvent) {
		// ...
	}
}
```

### Accessing the controller

As the *controller class* is replaced in compile time by a generated one, we cannot 
directly use it to call the controller of our views. Instead we have to define a public 
interface for them and then use it as the type given for the `getController` method of `FXMLLoader`.

The example below shows this:

```scala
trait UnitConverterInterface {
  def setInitialValue(value: Double)
}

@sfxml
class UnitConverterPresenter(// ... 
                            )
  extends UnitConverterInterface {
  
  // ...
}

// Instead of FXMLView, we create a new ScalaFXML loader
val loader = new FXMLLoader(
  getClass.getResource("unitconverter.fxml"),
  // ...
  )
  
loader.load()

val root = loader.getRoot[jfxs.Parent]

val controller = loader.getController[UnitConverterInterface]
controller.setInitialValue(10)

stage = new JFXApp.PrimaryStage() {
  title = "Unit converter"
  scene = new Scene(root)
}
```

### Nested controllers

Nested controllers can be used in a similar way as described above, by defining a public interface
for them first, using this interface as the type of the injected value in the parent controller, but 
explicitly marking the original controller class with a `@nested` annotation.

The following example demonstrates this:

```scala
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

@sfxml
class WindowController(nested: VBox,
                       @nested[NestedController] nestedController: NestedControllerInterface) {

  nestedController.doSomething()
}
```

### Third party control libraries
scalafxml recognizes factory JavaFX and ScalaFX controls, and assumes everything else to be an external non-UI dependency 
to be get from a *dependency provider*. When using third party control libraries, there are two possibilities:

* Listing the third party control package in the `@sfxml` annotation
* Using the `@FXML` annotation for these controls
 
The following example shows how to do this with [JFoenix](https://github.com/jfoenixadmin/JFoenix) using the first method:
 
```scala
@sfxml(additionalControls=List("com.jfoenix.controls"))
class TestController(input: JFXTextField,
                     create: JFXButton)
```

or with the second approach:

```scala
@sfxml
class TestController(@FXML input: JFXTextField,
                     @FXML create: JFXButton)
```

### Dependency injection
Beside the JavaFX controls, additional dependencies can be injected to the controller as well. This injection process is extensible.

#### Simple
It is also possible to simply give the dependencies _by their type_ or _by their name_:

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

#### SubCut
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

#### MacWire

The following example demonstrates how to use [MacWire](https://github.com/adamw/macwire) to inject additional dependencies:

```scala
object MacWireDemo extends JFXApp {

  class Module {
    def testDependency = TestDependency("MacWire dependency")
  }

  lazy val wired: Wired = wiredInModule(new Module)

  stage = new JFXApp.PrimaryStage() {
    title = "Hello world"
    scene = new Scene(
      FXMLView(getClass.getResource("startscreen.fxml"), 
      new MacWireDependencyResolver(wired)))
  }
}
```

#### Guice

The same example with [Guice](https://github.com/google/guice):

```scala
object GuiceDemo extends JFXApp {

  val module = new AbstractModule {
    def configure() {
      bind(classOf[TestDependency]).toInstance(new TestDependency("guice dependency"))
    }
  }
  implicit val injector = Guice.createInjector(module)

  stage = new JFXApp.PrimaryStage() {
    title = "Hello world"
    scene = new Scene(
      FXMLView(getClass.getResource("startscreen.fxml"), 
      new GuiceDependencyResolver()))
  }
}
```

## Requirements
* `sbt 0.13` is required

## Related
* [Related blog post](https://vigoo.github.io/posts/2014-01-12-scalafx-with-fxml.html) explaining how the library works.
