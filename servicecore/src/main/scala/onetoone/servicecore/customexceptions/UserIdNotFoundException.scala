package onetoone.servicecore.customexceptions

final case class UserIdNotFoundException(
                                          private val message: String = "user id not found",
                                          private val cause: Throwable = None.orNull
                                        ) extends Exception(message, cause)
