package onetoone.servicecore.directives

//Akka
import akka.http.scaladsl.marshalling.{Marshaller, ToEntityMarshaller}
import akka.http.scaladsl.model._
//Circe
import io.circe.Encoder
import io.circe.syntax._

trait Marshallers {

  def jsonMarshaller[A: Encoder]: ToEntityMarshaller[A] =
    Marshaller.withFixedContentType(ContentTypes.`application/json`) { body =>
      HttpEntity(ContentTypes.`application/json`, body.asJson.noSpaces)
    }

  def textMarshaller[A]: ToEntityMarshaller[A] =
    Marshaller.withFixedContentType(ContentTypes.`text/plain(UTF-8)`) { body =>
      HttpEntity(ContentTypes.`text/plain(UTF-8)`, body.toString)
    }

  implicit final def marshaller[A: Encoder]: ToEntityMarshaller[A] =
    Marshaller.oneOf(jsonMarshaller, textMarshaller)

}
