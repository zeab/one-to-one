package onetoone.users.http

case class PostUserRequest(
                            email: String,
                            userType: String,
                            programId: String,
                            timestamp: Option[Long] = None
                          )
