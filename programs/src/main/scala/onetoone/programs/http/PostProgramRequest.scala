package onetoone.programs.http

import onetoone.servicecore.models.programs.Tier

case class PostProgramRequest(
                               programId: String,
                               revisionId: Option[String],
                               name: String,
                               tiers: Set[Tier],
                               startDateTime: Option[String] = None,
                               endDateTime: Option[String] = None
                             )
