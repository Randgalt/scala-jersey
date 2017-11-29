package no.found.scala.jersey;

import org.glassfish.jersey.server.ContainerRequest;
import scala.Option;
import scala.concurrent.Future;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import java.lang.reflect.Method;

public class JerseyScalaInflector {
    private final Api.OpBase op;

    static final Method method;
    static {
        try {
            method = JerseyScalaInflector.class.getMethod("handle", ContainerRequest.class, AsyncResponse.class);
        } catch (NoSuchMethodException e) {
            // TODO log
            throw new RuntimeException(e);
        }
    }

    JerseyScalaInflector(Api.OpBase op) {
        this.op = op;
    }

    @SuppressWarnings("unchecked")
    public void handle(@Context ContainerRequest request, @Context AsyncResponse response) {
        MultivaluedMap<String, String> queryParams = request.getUriInfo().getQueryParameters();
        MultivaluedMap<String, String> pathParams = request.getUriInfo().getPathParameters();
        Future future;
        Option<Object> entityOption;
        if (op.hasEntity()) {
            Object entity = request.readEntity(op.entityClass());
            entityOption = Option.apply(entity);
        } else {
            entityOption = Option.empty();
        }
        Api.RequestMeta requestMeta = Api.RequestMeta$.MODULE$.apply(entityOption, queryParams, pathParams, request, response);
        future = op.apply(requestMeta);
        op.complete(future, response);
    }
}
