package no.found.scala.jersey

import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.core.MultivaluedMap

import no.found.scala.jersey.Routes.RequestMeta

object Enrichments {
  implicit class GetFirstOptional(map: MultivaluedMap[String, String]) {
    def value(key: String): Option[String] = {
      Option(map.getFirst(key))
    }
  }

  implicit class GetPathSegment(context: ContainerRequestContext) {
    def segment(id: String): Option[String] = {
      context.getUriInfo.getPathParameters().value(id)
    }
  }

  implicit class GetPathSegmentFromRequestMeta(requestMeta: RequestMeta[_]) {
    def segment(id: String): Option[String] = {
      requestMeta.requestContext.segment(id)
    }
  }

  implicit class GetQueryParameter(context: ContainerRequestContext) {
    def query(id: String): Option[String] = {
      context.getUriInfo.getQueryParameters().value(id)
    }
  }

  implicit class GetQueryParameterFromRequestMeta(requestMeta: RequestMeta[_]) {
    def query(id: String): Option[String] = {
      requestMeta.requestContext.query(id)
    }
  }
}
