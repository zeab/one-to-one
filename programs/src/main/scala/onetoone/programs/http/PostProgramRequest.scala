package onetoone.programs.http

//Imports
import onetoone.servicecore.models.http.programs.Level

case class PostProgramRequest(
                               programId: String,
                               name: String,
                               levels: Set[Level],
                               startDateTime: Option[Long] = None,
                               endDateTime: Option[Long] = None
                             )
