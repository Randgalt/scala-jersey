package no.found.scala.jersey

import javax.ws.rs.core.Response.Status

import no.found.scala.jersey.Api._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class ApiResource extends TopLevel {
  override val description = "Test"

  override val path = "/hey"

  private val metaGet = Op(
    description = "test",
    nickname = "this-is-a-test-get",
    path = "{id}"
  )
  val opGet = metaGet get { requestMeta =>
    Future.successful(Model("me", 42, requestMeta.segment("id")))
  }

  private val metaPut = Op(
    description = "test",
    nickname = "this-is-a-test-put"
  )
  val opPut = metaPut put { _: RequestMeta[Model] =>
    Future.successful(StatusResponse(Status.ACCEPTED.getStatusCode))
  }

  private val metaPost = Op(
    description = "test",
    nickname = "this-is-a-test-post"
  )
  val opPost = metaPost post { requestMeta: RequestMeta[Model] =>
    Future.successful(requestMeta.entity.map(_.name).getOrElse("dunno"))
  }

  private val metaDelete = Op(
    description = "test",
    nickname = "this-is-a-test-delete"
  )
  val opDelete = metaDelete delete { _ =>
    Future.successful(StatusResponse(Status.ACCEPTED.getStatusCode))
  }
}
