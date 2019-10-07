package onetoone.servicecore.models.programs

case class Level(
                 level: Int,
                 name: String,
                 pointsToNextLevel: Set[Set[LevelConditional]],
                 earnProfiles: Set[EarnProfile]
               )
