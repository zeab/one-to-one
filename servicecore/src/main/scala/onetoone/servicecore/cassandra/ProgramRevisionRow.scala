package onetoone.servicecore.cassandra

//Imports
import onetoone.servicecore.Tier

case class ProgramRevisionRow(
                              programId: String,
                              name: String,
                              tiers: List[Tier],
                              startDateTime: String,
                              finalDateTime: String
                            )
