package onetoone.servicecore.models.error

case class ErrorResponse(
                          code: String,
                          error: String,
                          additional: String = ""
                        )
