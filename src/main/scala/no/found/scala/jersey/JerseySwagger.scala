package no.found.scala.jersey

import javax.ws.rs.core.Response.Status

object JerseySwagger {
  case class Authorization(scheme: String, scopes: List[String] = List.empty)

  case class ResponseHeader(name: String, description: String = "", responseClass: Class[_] = Void.TYPE, responseContainer: String = "")

  trait Details {
    val tags: List[String] = List.empty
    val notes: String = ""
    val nickname: String = ""
    val sudoRequired: Boolean = false
    val statusCode: Int = Status.OK.getStatusCode
    val authorizations: List[Authorization] = List.empty
    val responseHeaders: List[ResponseHeader] = List.empty
  }
}
