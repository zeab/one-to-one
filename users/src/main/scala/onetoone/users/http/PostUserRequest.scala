package onetoone.users.http

case class PostUserRequest(
                            email: String,
                            programId: String,
                            userType: String,
                            name: String
                          )
