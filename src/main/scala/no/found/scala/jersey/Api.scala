package no.found.scala.jersey

import javax.ws.rs.container.AsyncResponse
import javax.ws.rs.core.{MultivaluedMap, Response}

import no.found.adminconsole.api.v1.util.AuthModel.AuthzMetadata

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

object Api {

  import reflect.runtime.universe.TypeTag

  sealed trait RequestMeta[E] {
    def auth(): AuthzMetadata

    def entity(): Option[E] = None

    def segment(name: String): Option[String] = None

    def query(name: String): Option[String] = None

    def queryAsBool(name: String): Option[Boolean] = None

    def queryAsInt(name: String): Option[Int] = None

    def queryAsLong(name: String): Option[Long] = None

    def queryAsDouble(name: String): Option[Double] = None
  }

  sealed trait Op

  //noinspection NotImplementedCode
  private[jersey] sealed abstract class OpBase[T, E](
    val method: String,
    val meta: Meta,
    val entityTag: Option[TypeTag[E]],
    block: RequestMeta[E] => Future[T],
    implicit val ec: ExecutionContext
  ) extends Op {
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

  private class GetOp[T](meta: Meta, block: RequestMeta[Any] => Future[T], ec: ExecutionContext)
    extends OpBase[T, Any]("GET", meta, None, block, ec)

  private class PutOp[T, E](meta: Meta, block: RequestMeta[E] => Future[T], ec: ExecutionContext)(implicit val tag: TypeTag[E])
    extends OpBase[T, E]("PUT", meta, Some(tag), block, ec)

  private class PostOp[T, E](meta: Meta, block: RequestMeta[E] => Future[T], ec: ExecutionContext)(implicit val tag: TypeTag[E])
    extends OpBase[T, E]("POST", meta, Some(tag), block, ec)

  private class DeleteOp[T](meta: Meta, block: RequestMeta[Any] => Future[T], ec: ExecutionContext)
    extends OpBase[T, Any]("DELETE", meta, None, block, ec)

  case class Meta(
    description: String,
    path: String = "/",
    tags: String = "",
    notes: String = "",
    nickname: String = "",
    sudoRequired: Boolean = false
  )(implicit ec: ExecutionContext) {
    def get[T](block: RequestMeta[Any] => Future[T]): Op = new GetOp[T](this, block, ec)

    def put[T, E](block: RequestMeta[E] => Future[T])(implicit tag: TypeTag[E]): Op = new PutOp[T, E](this, block, ec)

    def post[T, E](block: RequestMeta[E] => Future[T])(implicit tag: TypeTag[E]): Op = new PostOp[T, E](this, block, ec)

    def delete[T](block: RequestMeta[Any] => Future[T]): Op = new DeleteOp[T](this, block, ec)
  }

  object RequestMeta {
    def apply(query: MultivaluedMap[String, String], path: MultivaluedMap[String, String]): RequestMeta[Any] = apply(
      None, query, path
    )

    def apply[E](entityVal: Option[E], queryParams: MultivaluedMap[String, String], pathParams: MultivaluedMap[String, String]): RequestMeta[E] = new RequestMeta[E] {
      override def auth(): AuthzMetadata = ???  // TODO

      override def segment(name: String): Option[String] = Option(pathParams.getFirst(name))

      override def query(name: String): Option[String] = Option(queryParams.getFirst(name))

      override def queryAsBool(name: String): Option[Boolean] = query(name).map(_.toBoolean)

      override def queryAsInt(name: String): Option[Int] = query(name).map(_.toInt)

      override def queryAsLong(name: String): Option[Long] = query(name).map(_.toLong)

      override def queryAsDouble(name: String): Option[Double] = query(name).map(_.toDouble)

      override def entity(): Option[E] = entityVal
    }
  }

  trait TopLevel {
    def meta: Meta
  }

  case class StatusResponse(statusCode: Int)
  case class EntityStatusResponse[T](entity: T, statusCode: Int)
}
