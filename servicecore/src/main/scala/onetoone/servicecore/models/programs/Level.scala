package onetoone.servicecore.models.programs

case class Level(
                 level: Int,
                 name: String,
                 pointsToNextLevel: Int,
                 earnProfiles: Set[EarnProfile]
               )
