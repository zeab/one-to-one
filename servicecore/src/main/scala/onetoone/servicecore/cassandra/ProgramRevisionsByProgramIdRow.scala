package onetoone.servicecore.cassandra

import onetoone.servicecore.models.programs.Level

case class ProgramRevisionsByProgramIdRow(
                                           programId:String,
                                           startDateTime: String,
                                           endDateTime: String,
                                           revisionId: String,
                                           name: String,
                                           levels: Set[Level]
                                         )
