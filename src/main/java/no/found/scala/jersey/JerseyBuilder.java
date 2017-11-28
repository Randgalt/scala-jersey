package no.found.scala.jersey;

import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.Swagger;
import no.found.scala.jersey.Api.TopLevel;
import org.glassfish.jersey.server.model.Resource;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.MediaType;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class JerseyBuilder {
    public static Resource buildResource(TopLevel topLevel) {
        Api.Description topDescription = topLevel.description();
        Resource.Builder resourceBuilder = Resource.builder(topDescription.path());
        getOps(topLevel).forEach(op -> resourceBuilder
                .addChildResource(op.description().path())
                .addMethod()
                .httpMethod(op.method())
                .suspended(AsyncResponse.NO_TIMEOUT, TimeUnit.MILLISECONDS)
                .managedAsync()
                .consumes(MediaType.APPLICATION_JSON_TYPE)
                .produces(MediaType.APPLICATION_JSON_TYPE)
                .handledBy(new JerseyScalaInflector(op), JerseyScalaInflector.method));
        return resourceBuilder.build();
    }

    public static Swagger buildSwagger(List<TopLevel> topLevels) {
        Swagger swagger = new Swagger();
        topLevels.forEach(topLevel -> {
            getOps(topLevel).forEach(op -> {
                Operation operation = new Operation();
                operation.addConsumes(MediaType.APPLICATION_JSON);
                operation.addProduces(MediaType.APPLICATION_JSON);
                operation.setOperationId(topLevel.description().nickname());
                Path path = new Path();
                path.set(op.method().toLowerCase(), operation);
                swagger.path(op.description().nickname(), path);
            });
        });
        return swagger;
    }

    private interface MethodOp {
        Method method();

        Api.Op op();
    }

    private static List<Api.Op> getOps(TopLevel topLevel) {
        List<Api.Op> methods = new ArrayList<>();
        for ( Method method : topLevel.getClass().getMethods() ) {
            try {
                if ( Api.Op.class.isAssignableFrom(method.getReturnType()) ) {
                    Api.Op op = (Api.Op)method.invoke(topLevel);
                    methods.add(op);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return methods;
    }

    private JerseyBuilder() {
    }
}
