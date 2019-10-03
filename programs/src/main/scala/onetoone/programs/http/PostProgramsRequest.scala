package onetoone.programs.http

import onetoone.servicecore.Tier

case class PostProgramsRequest(
                                programId:String,
                                name: String,
                                tiers: List[Tier],
                                startDateTime: Option[String] = None,
                                finalDateTime: Option[String] = None
                              )