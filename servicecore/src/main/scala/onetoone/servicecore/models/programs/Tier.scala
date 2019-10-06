package onetoone.servicecore.models.programs

case class Tier(
                 level: Int,
                 name: String,
                 exp: Int,
                 earnProfiles: Set[EarnProfile]
               )
