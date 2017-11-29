package no.found.scala.jersey

import javax.ws.rs.core.Response.Status

object Swagger {
  case class Authorization(scheme: String, scopes: List[String] = List.empty)

  case class ResponseHeader(name: String, description: String = "", responseClass: Class[_] = Void.TYPE, responseContainer: String = "")

  trait Description {
    val path: String = "/"
    val tags: List[String] = List.empty
    val authorizations: List[Authorization] = List.empty
  }

  trait Operation extends Description {
    val notes: String = ""
    val nickname: String = ""
    val statusCode: Int = Status.OK.getStatusCode
    val responseHeaders: List[ResponseHeader] = List.empty
  }
}
