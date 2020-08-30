package info.genghis.pichost.error

trait AppError extends Exception {
  override def getMessage: String = this match {
    case AppConfigError(errors) => errors.map(err => s"Cannot load $err from configuration.").mkString("\n")
  }
}

case class AppConfigError(errors: List[String]) extends AppError