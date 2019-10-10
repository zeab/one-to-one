package onetoone.servicecore.models.http.programs

case class EarnProfile(
                        earnRate: Double,
                        userType: String = "base",
                        tank: String = "base"
                      )
