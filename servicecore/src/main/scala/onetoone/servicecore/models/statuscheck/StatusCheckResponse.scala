package onetoone.servicecore.models.statuscheck

case class StatusCheckResponse(
                                `type`: String,
                                status: Boolean,
                                id: String
                              )
