package onetoone.servicecore.models.http.programs

case class Level(
                 level: Int,
                 name: String,
                 pointsToNextLevel: Set[Set[LevelConditional]],
                 earnProfiles: Set[EarnProfile]
               )
