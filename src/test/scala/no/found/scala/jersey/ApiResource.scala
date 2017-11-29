package no.found.scala.jersey

import javax.ws.rs.core.Response.Status

import no.found.scala.jersey.Enrichments.GetPathSegmentFromRequestMeta
import no.found.scala.jersey.Routes._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ApiResource extends TopLevel {
  override val path = "/hey"

  val opGet: Route = get("/{id}") { requestMeta =>
    Future.successful(Model("me", 42, requestMeta.segment("id")))
  }

  val opPut: Route = put { _: RequestMeta[Model] =>
    Future.successful(StatusResponse(Status.ACCEPTED.getStatusCode))
  }

  val opPost: Route = post { requestMeta: RequestMeta[Model] =>
    Future.successful(requestMeta.entity.map(_.name).getOrElse("dunno"))
  }

  val opDelete: Route = delete { _ =>
    Future.successful(StatusResponse(Status.ACCEPTED.getStatusCode))
  }

  override def routes() = Seq(opGet, opPut, opPost, opDelete)
}
