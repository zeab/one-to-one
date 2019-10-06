package onetoone.servicecore.models.programs

case class EarnProfile(
                        earnRate: Double,
                        userType: String = "base",
                        tank: String = "base"
                      )
