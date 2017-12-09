package no.found.scala.jersey

import javax.ws.rs.HttpMethod
import javax.ws.rs.container.{AsyncResponse, ContainerRequestContext}
import javax.ws.rs.core.Response

import org.glassfish.jersey.internal.inject.InjectionManager
import org.glassfish.jersey.server.ContainerRequest

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.runtime.universe.TypeTag
import scala.util.control.NonFatal

sealed trait RequestMeta[E] {
  val request: ContainerRequestContext

  val response: AsyncResponse

  val injectionManager: InjectionManager

  val entity: Option[E] = None
}

sealed trait Route {
  val method: String
  val path: String

  def processRequest(request: ContainerRequest, response: AsyncResponse, injectionManager: InjectionManager): Unit
}

trait TopLevel {
  val path: String = "/"

  def routes(): Seq[Route]
}

case class StatusResponse(statusCode: Int)

case class EntityStatusResponse[T](entity: T, statusCode: Int)

object Routes {
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

    override def processRequest(request: ContainerRequest, response: AsyncResponse, injectionManager: InjectionManager): Unit = {
      val entity = entityTag.map(tag => request.readEntity(entityClass()).asInstanceOf[E])
      val requestMeta = RequestMeta(entity, request, response, injectionManager)
      val future = block(requestMeta)
      complete(future.asInstanceOf[Future[T]], response)
    }
  }

  private object RequestMeta {
    def apply[E](entityVal: Option[E], requestArg: ContainerRequestContext, responseArg: AsyncResponse, injectionManagerArg: InjectionManager):
      RequestMeta[E] = new RequestMeta[E]
    {
      override val request: ContainerRequestContext = requestArg

      override val response: AsyncResponse = responseArg

      override val injectionManager: InjectionManager = injectionManagerArg

      override val entity: Option[E] = entityVal
    }
  }

  case class Method(verb: String)

  val get = Method(HttpMethod.GET)
  val put = Method(HttpMethod.PUT)
  val post = Method(HttpMethod.POST)
  val delete = Method(HttpMethod.DELETE)
  val head = Method(HttpMethod.HEAD)
  val patch = Method(HttpMethod.PATCH)
  val options = Method(HttpMethod.OPTIONS)

  def route[T](method: Method, path: String = "/")(block: RequestMeta[Any] => Future[T])(implicit ec: ExecutionContext): Route = {
    new RouteBase[T, Any](method.verb, path, None, block, ec)
  }

  def routeEntity[T, E](method: Method, path: String = "/")(block: RequestMeta[E] => Future[T])(implicit tag: TypeTag[E], ec: ExecutionContext): Route = {
    new RouteBase[T, E](method.verb, path, Some(tag), block, ec)
  }
}
