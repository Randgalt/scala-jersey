package no.found.scala.jersey

import javax.ws.rs.container.{AsyncResponse, ContainerRequestContext}
import javax.ws.rs.core.Response.Status
import javax.ws.rs.core.{MultivaluedMap, Response}

import no.found.adminconsole.api.v1.util.AuthModel.AuthzMetadata

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

object Api {
  import reflect.runtime.universe.TypeTag

  sealed trait RequestMeta[E] {
    val auth: AuthzMetadata

    val requestContext: ContainerRequestContext

    val response: AsyncResponse

    val entity: Option[E] = None

    def segment(name: String): Option[String] = None

    def query(name: String): Option[String] = None

    def queryAsBool(name: String): Option[Boolean] = None

    def queryAsInt(name: String): Option[Int] = None

    def queryAsLong(name: String): Option[Long] = None

    def queryAsDouble(name: String): Option[Double] = None
  }

  sealed trait Operation

  protected[jersey] sealed class OpBase[T, E](
    val method: String,
    val op: Op,
    val entityTag: Option[TypeTag[E]],
    block: RequestMeta[E] => Future[T],
    implicit val ec: ExecutionContext
  ) extends Operation {
    def apply(requestMeta: RequestMeta[E]): Future[T] = block(requestMeta)

    def entityClass(): Class[_] = entityTag.map(tag => tag.mirror.runtimeClass(tag.tpe.typeSymbol.asClass)).getOrElse(throw new UnsupportedOperationException)

    val hasEntity: Boolean = entityTag.isDefined

    def complete(future: Future[T], response: AsyncResponse): Unit = {
      future.map {
        case r: Response => response.resume(r)
        case entityStatus: EntityStatusResponse[T] => response.resume(Response.status(entityStatus.statusCode).entity(entityStatus.entity).build())
        case status: StatusResponse => response.resume(Response.status(status.statusCode).build())
        case entity @ _ => response.resume(Response.ok(entity).build())
      }
      future.recover {
        case NonFatal(e) => response.resume(e)
      }
    }
  }

  trait OpMeta {
    val description: String
    val path: String = "/"
    val tags: List[String] = List.empty
  }

  case class Authorization(scheme: String, scopes: List[String] = List.empty)

  case class ResponseHeader(name: String, description: String = "", responseClass: Class[_] = Void.TYPE, responseContainer: String = "")

  case class Op(
    override val description: String,
    override val path: String = "/",
    override val tags: List[String] = List.empty,
    notes: String = "",
    nickname: String = "",
    sudoRequired: Boolean = false,
    statusCode: Int = Status.OK.getStatusCode,
    authorizations: List[Authorization] = List.empty,
    responseHeaders: List[ResponseHeader] = List.empty
  )(implicit ec: ExecutionContext) extends OpMeta {
    def get[T](block: RequestMeta[Any] => Future[T]): Operation = {
      new OpBase[T, Any]("GET", this, None, block, ec)
    }

    def put[T, E](block: RequestMeta[E] => Future[T])(implicit tag: TypeTag[E]): Operation = {
      new OpBase[T, E]("PUT", this, Some(tag), block, ec)
    }

    def post[T, E](block: RequestMeta[E] => Future[T])(implicit tag: TypeTag[E]): Operation = {
      new OpBase[T, E]("POST", this, Some(tag), block, ec)
    }

    def delete[T](block: RequestMeta[Any] => Future[T]): Operation = {
      new OpBase[T, Any]("DELETE", this, None, block, ec)
    }

    def verb[T](method: String)(block: RequestMeta[Any] => Future[T]): Operation = {
      new OpBase[T, Any](method, this, None, block, ec)
    }

    def verbEntity[T, E](method: String)(block: RequestMeta[E] => Future[T])(implicit tag: TypeTag[E]): Operation = {
      new OpBase[T, E](method, this, Some(tag), block, ec)
    }
  }

  object RequestMeta {
    def apply[E](entityVal: Option[E], queryParams: MultivaluedMap[String, String],
      pathParams: MultivaluedMap[String, String], requestArg: ContainerRequestContext, responseArg: AsyncResponse): RequestMeta[E] = new RequestMeta[E]
    {
      override val auth: AuthzMetadata = null  // TODO

      override val requestContext: ContainerRequestContext = requestArg

      override val response: AsyncResponse = responseArg

      override val entity: Option[E] = entityVal

      override def segment(name: String): Option[String] = {
        Option(pathParams.getFirst(name))
      }

      override def query(name: String): Option[String] = {
        Option(queryParams.getFirst(name))
      }

      override def queryAsBool(name: String): Option[Boolean] = {
        query(name).map(_.toBoolean)
      }

      override def queryAsInt(name: String): Option[Int] = {
        query(name).map(_.toInt)
      }

      override def queryAsLong(name: String): Option[Long] = {
        query(name).map(_.toLong)
      }

      override def queryAsDouble(name: String): Option[Double] = {
        query(name).map(_.toDouble)
      }
    }
  }

  trait TopLevel extends OpMeta {
  }

  case class StatusResponse(statusCode: Int)
  case class EntityStatusResponse[T](entity: T, statusCode: Int)
}
