package onetoone.servicecore.models.http.error

case class ErrorResponse(
                          code: String,
                          error: String,
                          additional: String = ""
                        )
