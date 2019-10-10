package onetoone.servicecore.models.http.statuscheck

case class StatusCheckResponse(
                                `type`: String,
                                status: Boolean,
                                id: String
                              )
