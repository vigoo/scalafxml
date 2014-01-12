package scalafxml.demo.unitconverter

trait UnitConverter {
  val description: String
  def run(input: String): String

  override def toString = description
}

class UnitConverters(converters: UnitConverter*) {
  val available = List(converters : _*)
}

object MMtoInches extends UnitConverter {
  val description: String = "Millimeters to inches"
  def run(input: String): String = try { (input.toDouble / 25.4).toString } catch { case ex: Throwable => ex.toString }
}

object InchesToMM extends UnitConverter {
  val description: String = "Inches to millimeters"
  def run(input: String): String = try { (input.toDouble * 25.4).toString } catch { case ex: Throwable => ex.toString }
}