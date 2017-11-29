package no.found.scala.jersey

import javax.ws.rs.core.Response.Status

import no.found.scala.jersey.Api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ApiResource extends TopLevel {
  override def meta = Meta(description = "test", path = "/hey")

  val opGet = Meta(
    description = "test",
    nickname = "this-is-a-test-get",
    path = "{id}"
  ) get { requestMeta =>
    Future.successful(Model("me", 42, requestMeta.segment("id")))
  }

  val opPut = Meta(
    description = "test",
    nickname = "this-is-a-test-put"
  ) put { _: RequestMeta[Model] =>
    Future.successful(StatusResponse(Status.ACCEPTED.getStatusCode))
  }

  val opPost = Meta(
    description = "test",
    nickname = "this-is-a-test-post"
  ) post { requestMeta: RequestMeta[Model] =>
    Future.successful(requestMeta.entity().map(_.name).getOrElse("dunno"))
  }

  val opDelete = Meta(
    description = "test",
    nickname = "this-is-a-test-delete"
  ) delete { _ =>
    Future.successful(StatusResponse(Status.ACCEPTED.getStatusCode))
  }
}
