package no.found.scala.jersey

import javax.ws.rs.core.Response.Status

import no.found.scala.jersey.Api.{Description, Entity}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ApiResource extends Api.TopLevel {
  override def description = Description(description = "test", path = "/hey")

  val opGetModel = Api.Get(Description(
    description = "test",
    nickname = "this-is-a-test-get",
    path = "{id}"
  )) { params =>
    Future.successful(Model("me", 42, params.segment("id")))
  }

  val opPutModel = Api.Put(Description(
    description = "test",
    nickname = "this-is-a-test-put",
  )) { params: Entity[Model] =>
    Future.successful(Api.StatusResponse(Status.ACCEPTED.getStatusCode))
  }

  val opPostModel = Api.Post(Description(
    description = "test",
    nickname = "this-is-a-test-post",
  )) { params: Entity[Model] =>
    Future.successful(params.entity().map(_.name).getOrElse("dunno"))
  }

  val opDeleteModel = Api.Delete(Description(
    description = "test",
    nickname = "this-is-a-test-delete",
  )) { params =>
    Future.successful(Api.StatusResponse(Status.ACCEPTED.getStatusCode))
  }
}
