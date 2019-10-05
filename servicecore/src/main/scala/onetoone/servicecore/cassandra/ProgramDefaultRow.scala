package onetoone.servicecore.cassandra

import onetoone.servicecore.Tier

case class ProgramDefaultRow(
                              programId: String,
                              name: String,
                              tiers: List[Tier]
                            )
