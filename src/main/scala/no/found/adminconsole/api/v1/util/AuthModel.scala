package no.found.adminconsole.api.v1.util

object AuthModel {
  sealed trait AuthzMetadata {
    type self <: AuthzMetadata
    val request: Option[Any]
    /** The HTTP request corresponding to the operation, if one exists */
    def withRequest(request: Any): self
    /** Provides information on the authenticated user */
    def userService: Any
    /** List of client IPs that originated the request */
    def remoteAddresses: Option[List[Any]] = None
  }
}
