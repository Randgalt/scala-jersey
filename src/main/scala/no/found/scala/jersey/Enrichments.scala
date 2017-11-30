package no.found.scala.jersey

import java.lang.reflect.Type
import javax.ws.rs.core.{Cookie, MultivaluedMap}

import no.found.scala.jersey.Routes.RequestMeta

object Enrichments {
  implicit class GetFirstOptional(map: MultivaluedMap[String, String]) {
    def value(key: String): Option[String] = {
      Option(map.getFirst(key))
    }
  }

  implicit class GetPathSegment(requestMeta: RequestMeta[_]) {
    def segment(id: String): Option[String] = {
      requestMeta.request.getUriInfo.getPathParameters().value(id)
    }
  }

  implicit class GetQueryParameter(requestMeta: RequestMeta[_]) {
    def query(id: String): Option[String] = {
      requestMeta.request.getUriInfo.getQueryParameters().value(id)
    }
  }

  implicit class GetHeaderParameter(requestMeta: RequestMeta[_]) {
    def header(name: String): Option[String] = {
      requestMeta.request.getHeaders.value(name)
    }
  }

  implicit class GetCookie(requestMeta: RequestMeta[_]) {
    def cookie(name: String): Option[Cookie] = {
      Option(requestMeta.request.getCookies.get(name))
    }
  }

  implicit class GetInjected(requestMeta: RequestMeta[_]) {
    def injected[T](clazz: Class[T]): T = {
      requestMeta.injectionManager.getInstance(clazz)
    }

    def injected[T](t: Type): T = {
      requestMeta.injectionManager.getInstance(t)
    }
  }
}
