package no.found.scala.jersey

import javax.ws.rs.container.AsyncResponse
import javax.ws.rs.core.{MultivaluedMap, Response}

import no.found.adminconsole.api.v1.util.AuthModel.AuthzMetadata

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

object Api {
  import reflect.runtime.universe.TypeTag

  sealed trait MetaData {
    def meta(): AuthzMetadata
  }

  sealed trait Parameters {
    def meta: MetaData

    def segment(name: String): Option[String] = None

    def query(name: String): Option[String] = None

    def queryAsBool(name: String): Option[Boolean] = None

    def queryAsInt(name: String): Option[Int] = None

    def queryAsLong(name: String): Option[Long] = None

    def queryAsDouble(name: String): Option[Double] = None
  }

  sealed trait Entity[E] extends Parameters {
    def entity(): Option[E] = None
  }

  sealed trait Op[T, E] {
    def method: String

    def description: Description

    def hasEntity: Boolean

    def ec: ExecutionContext

    def apply(parameters: Parameters): Future[T] = Future.failed(new UnsupportedOperationException)

    def apply(parameters: Entity[E]): Future[T] = Future.failed(new UnsupportedOperationException)

    def entityTag: TypeTag[E] = throw new UnsupportedOperationException

    def entityClass: Class[_] = entityTag.mirror.runtimeClass(entityTag.tpe.typeSymbol.asClass)

    def complete(future: Future[T], response: AsyncResponse): Unit = {
      implicit val localEc: ExecutionContext = ec
      future.map {
        case r: Response => response.resume(r)
        case status: StatusResponse => response.resume(Response.status(status.statusCode).build())
        case entity @ _ => response.resume(Response.ok(entity).build())
      }
      future.recover {
        case NonFatal(e) => response.resume(e)
      }
    }
  }

  sealed class GetOp[T](val description: Description, block: Parameters => Future[T], val ec: ExecutionContext) extends Op[T, Any] {
    def get(parameters: Parameters): Future[T] = block(parameters)

    override def apply(parameters: Parameters): Future[T] = get(parameters)

    override def method: String = "GET"

    override def hasEntity: Boolean = false
  }

  sealed class PutOp[T, E](val description: Description, block: Entity[E] => Future[T], val ec: ExecutionContext)(override implicit val entityTag: TypeTag[E]) extends Op[T, E] {
    def put(parameters: Entity[E]): Future[T] = block(parameters)

    override def apply(parameters: Entity[E]): Future[T] = put(parameters)

    override def method: String = "PUT"

    override def hasEntity: Boolean = true
  }

  sealed class PostOp[T, E](val description: Description, block: Entity[E] => Future[T], val ec: ExecutionContext)(override implicit val entityTag: TypeTag[E]) extends Op[T, E] {
    def post(parameters: Entity[E]): Future[T] = block(parameters)

    override def apply(parameters: Entity[E]): Future[T] = post(parameters)

    override def method: String = "POST"

    override def hasEntity: Boolean = true
  }

  sealed class DeleteOp[T](val description: Description, block: Parameters => Future[T], val ec: ExecutionContext) extends Op[T, Any] {
    def delete(parameters: Parameters): Future[T] = block(parameters)

    override def apply(parameters: Parameters): Future[T] = delete(parameters)

    override def method: String = "DELETE"

    override def hasEntity: Boolean = false
  }

  case class Description(
    description: String,
    path: String = "/",
    tags: String = "",
    notes: String = "",
    nickname: String = ""
  )

  object Parameters {
    def apply[E](query: MultivaluedMap[String, String], path: MultivaluedMap[String, String]): Parameters = apply(
      None, query, path
    )

    def apply[E](entityVal: Option[E], queryParams: MultivaluedMap[String, String], pathParams: MultivaluedMap[String, String]): Entity[E] = new Entity[E] {
      override def meta: MetaData = throw new UnsupportedOperationException // TODO

      override def segment(name: String): Option[String] = Option(pathParams.getFirst(name))

      override def query(name: String): Option[String] = Option(queryParams.getFirst(name))

      override def queryAsBool(name: String): Option[Boolean] = query(name).map(_.toBoolean)

      override def queryAsInt(name: String): Option[Int] = query(name).map(_.toInt)

      override def queryAsLong(name: String): Option[Long] = query(name).map(_.toLong)

      override def queryAsDouble(name: String): Option[Double] = query(name).map(_.toDouble)

      override def entity(): Option[E] = entityVal
    }
  }

  object Get {
    def apply[T](description: Description)(block: Parameters => Future[T])(implicit ec: ExecutionContext): GetOp[T] = new GetOp[T](description, block, ec)
  }

  object Put {
    def apply[T, E](description: Description)(block: Entity[E] => Future[T])(implicit tag: TypeTag[E], ec: ExecutionContext): PutOp[T, E] = new PutOp[T, E](description, block, ec)
  }

  object Post {
    def apply[T, E](description: Description)(block: Entity[E] => Future[T])(implicit tag: TypeTag[E], ec: ExecutionContext): PostOp[T, E] = new PostOp[T, E](description, block, ec)
  }

  object Delete {
    def apply[T](description: Description)(block: Parameters => Future[T])(implicit ec: ExecutionContext): DeleteOp[T] = new DeleteOp[T](description, block, ec)
  }

  trait TopLevel {
    def description: Description
  }

  case class StatusResponse(statusCode: Int)
}
