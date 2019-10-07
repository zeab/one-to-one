package onetoone.programs.http

//Imports
import onetoone.servicecore.models.programs.Level

case class PostProgramRequest(
                               programId: String,
                               revisionId: Option[String],
                               name: String,
                               levels: Set[Level],
                               startDateTime: Option[String] = None,
                               endDateTime: Option[String] = None
                             )
