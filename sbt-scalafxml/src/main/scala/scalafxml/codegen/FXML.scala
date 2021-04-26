package scalafxml.codegen

import kantan.xpath.{ Node, XmlSource }
import kantan.xpath.implicits._

import java.io.{ File, FileReader }
import javax.xml.namespace.QName
import javax.xml.stream.{ XMLInputFactory, XMLStreamConstants, XMLStreamReader }

class FXML(fragments: Seq[FXML.Fragment]) {
  lazy val controllerClass: Option[String] = fragments.collectFirst {
    case FXML.ControllerReference(ref) => ref
  }
  lazy val includes: Seq[String] = fragments.collect { case FXML.Include(pattern) => pattern }
  lazy val allIdentifiedComponents: Map[String, String] = fragments.collect {
    case FXML.IdentifiedComponent(id, typ)          => id -> typ
    case FXML.IdentifiedInclude(id, controllerType) => id -> controllerType
  }.toMap
}

object FXML {
  def load(file: File): FXML = {
    val factory = XMLInputFactory.newInstance
    val reader = factory.createXMLStreamReader(new FileReader(file))
    val fragments = readFragments(reader)
    new FXML(fragments)
  }

  private val fxController = new QName("http://javafx.com/fxml/1", "controller")
  private val fxId = new QName("http://javafx.com/fxml/1", "id")
  private val fxInclude = new QName("http://javafx.com/fxml/1", "include")

  private def readFragments(reader: XMLStreamReader): Seq[Fragment] = {
    val result = List.newBuilder[Fragment]
    while (reader.hasNext) {
      reader.next()
      if (
        reader.getEventType == XMLStreamConstants.PROCESSING_INSTRUCTION && reader.getPITarget == "import"
      ) {
        result += Include(reader.getPIData)
      } else if (reader.getEventType == XMLStreamConstants.START_ELEMENT) {
        val attribs = getAttributes(reader)
        if (attribs.contains(fxController)) {
          result += ControllerReference(attribs(fxController))
        }

        if (attribs.contains(fxId)) {
          if (reader.getName == fxInclude && attribs.contains(QName.valueOf("source"))) {
            result += IdentifiedInclude(
              attribs(fxId),
              loadControllerName(attribs(QName.valueOf("source")))
            )
          } else {
            result += IdentifiedComponent(attribs(fxId), reader.getName.getLocalPart)
          }
        }
      }
    }
    result.result()
  }

  private def getAttributes(reader: XMLStreamReader): Map[QName, String] = {
    val builder = Map.newBuilder[QName, String]
    for (i <- 0 until reader.getAttributeCount) {
      val name = reader.getAttributeName(i)
      val value = reader.getAttributeValue(i)
      builder += name -> value
    }
    builder.result()
  }

  private def loadControllerName(name: String): String =
    "TODO" // TODO

  sealed trait Fragment
  case class Include(pattern: String) extends Fragment
  case class ControllerReference(reference: String) extends Fragment
  case class IdentifiedComponent(id: String, typ: String) extends Fragment
  case class IdentifiedInclude(id: String, controllerType: String) extends Fragment
}
