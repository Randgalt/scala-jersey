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

  sealed trait Operation

  protected[jersey] sealed abstract class OpBase[T, E](
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

  private class GetOp[T](op: Op, block: RequestMeta[Any] => Future[T], ec: ExecutionContext)
    extends OpBase[T, Any]("GET", op, None, block, ec)

  private class PutOp[T, E](op: Op, block: RequestMeta[E] => Future[T], ec: ExecutionContext)(implicit val tag: TypeTag[E])
    extends OpBase[T, E]("PUT", op, Some(tag), block, ec)

  private class PostOp[T, E](op: Op, block: RequestMeta[E] => Future[T], ec: ExecutionContext)(implicit val tag: TypeTag[E])
    extends OpBase[T, E]("POST", op, Some(tag), block, ec)

  private class DeleteOp[T](op: Op, block: RequestMeta[Any] => Future[T], ec: ExecutionContext)
    extends OpBase[T, Any]("DELETE", op, None, block, ec)

  trait Meta {
    val description: String
    val path: String = "/"
    val tags: String = ""
    val notes: String = ""
    val nickname: String = ""
  }

  case class Op(
    override val description: String,
    override val path: String = "/",
    override val tags: String = "",
    override val notes: String = "",
    override val nickname: String = "",
    sudoRequired: Boolean = false
  )(implicit ec: ExecutionContext) extends Meta {
    def get[T](block: RequestMeta[Any] => Future[T]): Operation = new GetOp[T](this, block, ec)

    def put[T, E](block: RequestMeta[E] => Future[T])(implicit tag: TypeTag[E]): Operation = new PutOp[T, E](this, block, ec)

    def post[T, E](block: RequestMeta[E] => Future[T])(implicit tag: TypeTag[E]): Operation = new PostOp[T, E](this, block, ec)

    def delete[T](block: RequestMeta[Any] => Future[T]): Operation = new DeleteOp[T](this, block, ec)
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

  trait TopLevel extends Meta {
  }

  case class StatusResponse(statusCode: Int)
  case class EntityStatusResponse[T](entity: T, statusCode: Int)
}
