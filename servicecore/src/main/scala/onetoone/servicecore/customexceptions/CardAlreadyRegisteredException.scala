package onetoone.servicecore.customexceptions

final case class CardAlreadyRegisteredException(
                                          private val message: String = "card is already registered",
                                          private val cause: Throwable = None.orNull
                                        ) extends Exception(message, cause)