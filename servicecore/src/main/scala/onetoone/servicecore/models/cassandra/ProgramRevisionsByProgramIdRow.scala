package onetoone.servicecore.models.cassandra

import onetoone.servicecore.models.http.programs.Level

case class ProgramRevisionsByProgramIdRow(
                                           programId:String,
                                           startDateTime: Long,
                                           endDateTime: Long,
                                           revisionId: String,
                                           name: String,
                                           levels: Set[Level]
                                         )
