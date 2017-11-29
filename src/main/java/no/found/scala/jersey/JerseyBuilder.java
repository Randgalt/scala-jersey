package no.found.scala.jersey;

import no.found.scala.jersey.Routes.TopLevel;
import org.glassfish.jersey.server.model.Resource;
import scala.collection.JavaConverters;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.MediaType;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

public class JerseyBuilder {
    public static Resource buildResource(TopLevel topLevel) {
        Resource.Builder resourceBuilder = Resource.builder(topLevel.path());
        getRoutes(topLevel).forEach(op -> resourceBuilder
                .addChildResource(op.path())
                .addMethod()
                .httpMethod(op.method())
                .suspended(AsyncResponse.NO_TIMEOUT, TimeUnit.MILLISECONDS)
                .managedAsync()
                .consumes(MediaType.APPLICATION_JSON_TYPE)
                .produces(MediaType.APPLICATION_JSON_TYPE)
                .handledBy(new JerseyScalaInflector(op), JerseyScalaInflector.method));
        return resourceBuilder.build();
    }
/*

    public static Swagger buildSwagger(List<TopLevel> topLevels) {
        Swagger swagger = new Swagger();
        topLevels.forEach(topLevel -> {
            getOps(topLevel).forEach(op -> {
                Operation operation = new Operation();
                operation.addConsumes(MediaType.APPLICATION_JSON);
                operation.addProduces(MediaType.APPLICATION_JSON);
                Path path = new Path();
                path.set(op.method().toLowerCase(), operation);
                swagger.path(op.op().nickname(), path);
            });
        });
        return swagger;
    }
*/

    private static Collection<Routes.Route> getRoutes(TopLevel topLevel) {
        return JavaConverters.asJavaCollectionConverter(topLevel.routes()).asJavaCollection();
    }

    private JerseyBuilder() {
    }
}
