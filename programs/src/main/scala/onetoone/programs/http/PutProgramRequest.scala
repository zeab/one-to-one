package onetoone.programs.http

//Imports
import onetoone.servicecore.models.http.programs.Level

case class PutProgramRequest(
                              programId: String,
                              revisionId: String,
                              name: Option[String] = None,
                              levels: Option[Set[Level]] = None,
                              startDateTime: Option[String] = None,
                              endDateTime: Option[String] = None
                            )
