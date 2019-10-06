package onetoone.servicecore.cassandra

import onetoone.servicecore.models.programs.Tier

case class ProgramRevisionsByProgramIdRow(
                                           programId:String,
                                           startDateTime: String,
                                           endDateTime: String,
                                           revisionId: String,
                                           name: String,
                                           tiers: Set[Tier]
                                         )
