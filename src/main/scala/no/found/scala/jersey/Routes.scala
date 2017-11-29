package no.found.scala.jersey

import javax.ws.rs.container.{AsyncResponse, ContainerRequestContext}
import javax.ws.rs.core.Response

import no.found.adminconsole.api.v1.util.AuthModel.AuthzMetadata
import org.glassfish.jersey.server.ContainerRequest

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

object Routes {
  import reflect.runtime.universe.TypeTag

  sealed trait RequestMeta[E] {
    val auth: AuthzMetadata

    val requestContext: ContainerRequestContext

    val response: AsyncResponse

    val entity: Option[E] = None
  }

  sealed trait Route {
    val method: String
    val path: String

    def processRequest(request: ContainerRequest, response: AsyncResponse): Unit
  }

  private class RouteBase[T, E](
    override val method: String,
    override val path: String,
    val entityTag: Option[TypeTag[E]],
    block: RequestMeta[E] => Future[T],
    implicit val ec: ExecutionContext
  ) extends Route {
    private def entityClass(): Class[_] = entityTag.map(tag => tag.mirror.runtimeClass(tag.tpe.typeSymbol.asClass)).getOrElse(throw new UnsupportedOperationException)

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

    override def processRequest(request: ContainerRequest, response: AsyncResponse): Unit = {
      val entity = entityTag.map(tag => request.readEntity(entityClass()).asInstanceOf[E])
      val requestMeta = RequestMeta(entity, request, response)
      val future = block(requestMeta)
      complete(future.asInstanceOf[Future[T]], response)
    }
  }

  private object RequestMeta {
    def apply[E](entityVal: Option[E], requestArg: ContainerRequestContext, responseArg: AsyncResponse): RequestMeta[E] = new RequestMeta[E]
    {
      override val auth: AuthzMetadata = null  // TODO

      override val requestContext: ContainerRequestContext = requestArg

      override val response: AsyncResponse = responseArg

      override val entity: Option[E] = entityVal
    }
  }

  def get[T](block: RequestMeta[Any] => Future[T])(implicit ec: ExecutionContext): Route = {
    get("/")(block)
  }

  def get[T](path: String)(block: RequestMeta[Any] => Future[T])(implicit ec: ExecutionContext): Route = {
    new RouteBase[T, Any]("GET", path, None, block, ec)
  }

  def put[T, E](block: RequestMeta[E] => Future[T])(implicit tag: TypeTag[E], ec: ExecutionContext): Route = {
    put("/")(block)
  }

  def put[T, E](path: String)(block: RequestMeta[E] => Future[T])(implicit tag: TypeTag[E], ec: ExecutionContext): Route = {
    new RouteBase[T, E]("PUT", path, Some(tag), block, ec)
  }

  def post[T, E](block: RequestMeta[E] => Future[T])(implicit tag: TypeTag[E], ec: ExecutionContext): Route = {
    post("/")(block)
  }

  def post[T, E](path: String)(block: RequestMeta[E] => Future[T])(implicit tag: TypeTag[E], ec: ExecutionContext): Route = {
    new RouteBase[T, E]("POST", path, Some(tag), block, ec)
  }

  def delete[T](block: RequestMeta[Any] => Future[T])(implicit ec: ExecutionContext): Route = {
    delete("/")(block)
  }

  def delete[T](path: String)(block: RequestMeta[Any] => Future[T])(implicit ec: ExecutionContext): Route = {
    new RouteBase[T, Any]("DELETE", path, None, block, ec)
  }

  def verb[T](method: String, path: String = "/")(block: RequestMeta[Any] => Future[T])(implicit ec: ExecutionContext): Route = {
    new RouteBase[T, Any](method, path, None, block, ec)
  }

  def verbEntity[T, E](method: String, path: String = "/")(block: RequestMeta[E] => Future[T])(implicit tag: TypeTag[E], ec: ExecutionContext): Route = {
    new RouteBase[T, E](method, path, Some(tag), block, ec)
  }

  trait TopLevel {
    val path: String = "/"

    def routes(): Seq[Route]
  }

  case class StatusResponse(statusCode: Int)
  case class EntityStatusResponse[T](entity: T, statusCode: Int)
}
