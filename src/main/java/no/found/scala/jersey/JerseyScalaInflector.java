package no.found.scala.jersey;

import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.server.ContainerRequest;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.Context;
import java.lang.reflect.Method;

public class JerseyScalaInflector {
    private final TopLevel topLevel;
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

    JerseyScalaInflector(TopLevel topLevel, Route route) {
        this.topLevel = topLevel;
        this.route = route;
    }

    @SuppressWarnings("unchecked")
    public void handle(@Context ContainerRequest request, @Context AsyncResponse response, @Context InjectionManager injectionManager) {
        route.checkRoles(topLevel.role(), request);

        route.processRequest(request, response, injectionManager);
    }
}
