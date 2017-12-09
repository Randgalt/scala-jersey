package no.found.scala.jersey

import java.math.BigDecimal
import javax.ws.rs.core.Response.Status

import no.found.scala.jersey.Enrichments.{GetInjected, GetPathSegment}
import no.found.scala.jersey.Routes._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ApiResource extends TopLevel {
  override val path = "/hey"

  private val opGet = route(get, "/{id}") { requestMeta =>
    val bd = requestMeta.injected(classOf[BigDecimal])
    Future.successful(Model("me", bd.intValue(), requestMeta.segment("id")))
  }

  private val opPut = route(put) { _ =>
    Future.successful(StatusResponse(Status.ACCEPTED.getStatusCode))
  }

  private val opPost = routeEntity(post) { requestMeta: RequestMeta[Model] =>
    Future.successful(requestMeta.entity.map(_.name).getOrElse("dunno"))
  }

  private val opDelete = route(delete) { _ =>
    Future.successful(StatusResponse(Status.ACCEPTED.getStatusCode))
  }

  override def routes() = Seq(opGet, opPut, opPost, opDelete)
}
