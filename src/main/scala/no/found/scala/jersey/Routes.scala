package no.found.scala.jersey

import javax.ws.rs.{ForbiddenException, HttpMethod}
import javax.ws.rs.container.{AsyncResponse, ContainerRequestContext}
import javax.ws.rs.core.Response

import org.glassfish.jersey.internal.inject.InjectionManager
import org.glassfish.jersey.server.ContainerRequest
import org.glassfish.jersey.server.internal.LocalizationMessages

import scala.collection.Seq
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.runtime.universe.TypeTag
import scala.util.control.NonFatal

sealed trait RequestMeta[E] {
  val request: ContainerRequestContext

  val response: AsyncResponse

  val injectionManager: InjectionManager

  val entity: Option[E] = None
}

sealed class TopLevelRole
class DenyAll extends TopLevelRole
class PermitAll extends TopLevelRole

sealed trait Route {
  val method: String
  val path: String
  val roles: Option[Seq[String]]

  def processRequest(request: ContainerRequest, response: AsyncResponse, injectionManager: InjectionManager): Unit

  def checkRoles(topLevelRole: Option[TopLevelRole], request: ContainerRequest): Unit

  def withRoles(roles: Seq[String]): Route
}

trait TopLevel {
  val path: String = "/"
  val role: Option[TopLevelRole] = None

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
    implicit val ec: ExecutionContext,
    override val roles: Option[Seq[String]] = None
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

    override def withRoles(roles: Seq[String]): Route = {
      new RouteBase[T, E](method, path, entityTag, block, ec, Some(roles))
    }

    override def checkRoles(topLevelRole: Option[TopLevelRole], request: ContainerRequest): Unit = {
      // see RolesAllowedDynamicFeature.RolesAllowedRequestFilter
      val localRoles = topLevelRole match {
        case Some(_: DenyAll) => Some(Seq.empty)
        case Some(_: PermitAll) => None
        case _ => roles
      }

      if ( localRoles.nonEmpty ) {
        if ( request.getSecurityContext.getUserPrincipal == null ) {
          throw new ForbiddenException(LocalizationMessages.USER_NOT_AUTHORIZED)
        }

        if ( !localRoles.exists(roleNames => roleNames.exists(role => request.getSecurityContext.isUserInRole(role))) ) {
          throw new ForbiddenException(LocalizationMessages.USER_NOT_AUTHORIZED)
        }
      }
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
