package no.found.scala.jersey;

import org.glassfish.jersey.server.ContainerRequest;
import scala.Option;
import scala.concurrent.Future;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import java.lang.reflect.Method;

public class JerseyScalaInflector {
    private final Api.Op op;

    static final Method method;
    static {
        try {
            method = JerseyScalaInflector.class.getMethod("handle", ContainerRequest.class, AsyncResponse.class);
        } catch (NoSuchMethodException e) {
            // TODO log
            throw new RuntimeException(e);
        }
    }

    JerseyScalaInflector(Api.Op op) {
        this.op = op;
    }

    @SuppressWarnings("unchecked")
    public void handle(@Context ContainerRequest request, @Context AsyncResponse response) {
        MultivaluedMap<String, String> queryParams = request.getUriInfo().getQueryParameters();
        MultivaluedMap<String, String> pathParams = request.getUriInfo().getPathParameters();
        Future future;
        if (op.hasEntity()) {
            Class aClass = op.entityClass();
            Object entity = request.readEntity(aClass);
            Api.Entity parameters = Api.Parameters$.MODULE$.apply(Option.apply(entity), queryParams, pathParams);
            future = op.apply(parameters);
        } else {
            Api.Parameters parameters = Api.Parameters$.MODULE$.apply(queryParams, pathParams);
            future = op.apply(parameters);
        }
        op.complete(future, response);
    }
}
