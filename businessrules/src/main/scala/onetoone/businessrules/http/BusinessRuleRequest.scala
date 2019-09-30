package onetoone.businessrules.http

case class BusinessRuleRequest(
                                name: String,
                                startTimestamp: String,
                                endTimestamp: String,
                                peopleWhoAreAffected: List[String],
                                productsWhoAreAffected: List[String],
                                rule: String
                              )
