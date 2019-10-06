package onetoone.users.http

case class GetUserInfo200(
                           userId: String,
                           birthday: String,
                           languagePreference: String,
                           contactInformation: String,
                           contactPreference: String
                         )
