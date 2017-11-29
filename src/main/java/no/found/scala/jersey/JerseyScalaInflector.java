package no.found.scala.jersey;

import org.glassfish.jersey.server.ContainerRequest;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.Context;
import java.lang.reflect.Method;

public class JerseyScalaInflector {
    private final Routes.Route route;

    static final Method method;
    static {
        try {
            method = JerseyScalaInflector.class.getMethod("handle", ContainerRequest.class, AsyncResponse.class);
        } catch (NoSuchMethodException e) {
            // TODO log
            throw new RuntimeException(e);
        }
    }

    JerseyScalaInflector(Routes.Route route) {
        this.route = route;
    }

    @SuppressWarnings("unchecked")
    public void handle(@Context ContainerRequest request, @Context AsyncResponse response) {
        route.processRequest(request, response);
    }
}
