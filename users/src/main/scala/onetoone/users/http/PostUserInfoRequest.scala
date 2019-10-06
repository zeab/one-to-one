package onetoone.users.http

case class PostUserInfoRequest(
                                userId: String,
                                birthday: String,
                                languagePreference: String,
                                contactInformation: String,
                                contactPreference: String
                              )
