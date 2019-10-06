package onetoone.servicecore.customexceptions

final case class InvalidQueryException(
                                     private val message: String,
                                     private val cause: Throwable = None.orNull
                                   ) extends Exception(message, cause)