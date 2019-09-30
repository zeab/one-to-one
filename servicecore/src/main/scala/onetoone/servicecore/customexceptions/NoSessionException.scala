package onetoone.servicecore.customexceptions

final case class NoSessionException(
                                     private val message: String = "there is no session for cassandra to talk too",
                                     private val cause: Throwable = None.orNull
                                   ) extends Exception(message, cause)