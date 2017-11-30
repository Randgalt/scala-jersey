package no.found.scala.jersey;

import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.server.ContainerRequest;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.Context;
import java.lang.reflect.Method;

public class JerseyScalaInflector {
    private final Route route;

    static final Method method;
    static {
        try {
            method = JerseyScalaInflector.class.getMethod("handle", ContainerRequest.class, AsyncResponse.class, InjectionManager.class);
        } catch (NoSuchMethodException e) {
            // TODO log
            throw new RuntimeException(e);
        }
    }

    JerseyScalaInflector(Route route) {
        this.route = route;
    }

    @SuppressWarnings("unchecked")
    public void handle(@Context ContainerRequest request, @Context AsyncResponse response, @Context InjectionManager injectionManager) {
        route.processRequest(request, response, injectionManager);
    }
}
