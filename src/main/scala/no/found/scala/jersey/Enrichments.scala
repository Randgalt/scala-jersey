package no.found.scala.jersey

import java.lang.reflect.Type
import java.security.Principal
import javax.ws.rs.core.{Cookie, MultivaluedMap}

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

  implicit class SecurityContextOptionals(requestMeta: RequestMeta[_]) {
    def userPrincipal: Option[Principal] = {
      Option(requestMeta.request.getSecurityContext).map(_.getUserPrincipal)
    }

    def isUserInRole(role: String): Boolean = {
      (requestMeta.request.getSecurityContext != null) && requestMeta.request.getSecurityContext.isUserInRole(role)
    }

    def isSecure: Boolean = {
      (requestMeta.request.getSecurityContext != null) && requestMeta.request.getSecurityContext.isSecure
    }

    def authenticationScheme: Option[String] = {
      Option(requestMeta.request.getSecurityContext).map(_.getAuthenticationScheme)
    }
  }
}
