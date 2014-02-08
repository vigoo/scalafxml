package scalafxml.core

/**
 * Provides access for a wrapped controller.
 *
 * Implemented by the macro-generated controller classes.
 */
trait ControllerAccessor {

  /**
   * Gets the controller implementation casted to the given type
   * @tparam T a supertype of the original controller class
   * @return returns the original controller instance
   */
  def as[T](): T
}
