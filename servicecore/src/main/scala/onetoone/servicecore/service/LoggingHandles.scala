package onetoone.servicecore.service

//Imports
import net.logstash.logback.argument.StructuredArgument
import net.logstash.logback.argument.StructuredArguments.value

trait LoggingHandles {

  def logFlatten(args: AnyRef*): Array[Object] = {
    args.flatMap {
      case s: Seq[_] => s
      case x => Seq(x)
    }.toArray.asInstanceOf[Array[Object]]
  }

  def logUniqueId(id: String): StructuredArgument = value("unique-id", id)

  def logStatus(status: Boolean): StructuredArgument = value("status", status)

  def logExternalId(implicit id: String): StructuredArgument = value("external-id", id)

  def logExceptionClass(exceptionClass: String): StructuredArgument = value("exception-class", exceptionClass)

}
