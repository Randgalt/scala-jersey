package no.found.scala.jersey;

import no.found.scala.jersey.Routes.TopLevel;
import org.glassfish.jersey.server.model.Resource;
import scala.collection.JavaConverters;

import javax.ws.rs.core.MediaType;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class JerseyBuilder {
    private final ContentTypeMapper consumesMapper;
    private final ContentTypeMapper producesMapper;
    private final long defaultSuspendedTimeoutMs;

    public static final ContentTypeMapper jsonMapper = (topLevel, route) -> Collections.singleton(MediaType.APPLICATION_JSON_TYPE);

    @FunctionalInterface
    public interface ContentTypeMapper {
        Collection<MediaType> types(TopLevel topLevel, Routes.Route route);
    }

    public JerseyBuilder() {
        this(jsonMapper, jsonMapper, Optional.empty());
    }

    public JerseyBuilder(ContentTypeMapper consumesMapper, ContentTypeMapper producesMapper, Optional<Duration> defaultSuspendedTimeout) {
        this.consumesMapper = Objects.requireNonNull(consumesMapper, "consumesMapper cannot be null");
        this.producesMapper = Objects.requireNonNull(producesMapper, "producesMapper cannot be null");
        this.defaultSuspendedTimeoutMs = defaultSuspendedTimeout.map(Duration::toMillis).orElse(0L);
    }

    public Resource buildResource(TopLevel topLevel) {
        Resource.Builder resourceBuilder = Resource.builder(topLevel.path());
        getRoutes(topLevel).forEach(route -> resourceBuilder
                .addChildResource(route.path())
                .addMethod()
                .httpMethod(route.method())
                .suspended(defaultSuspendedTimeoutMs, TimeUnit.MILLISECONDS)
                .managedAsync()
                .consumes(consumesMapper.types(topLevel, route))
                .produces(producesMapper.types(topLevel, route))
                .handledBy(new JerseyScalaInflector(route), JerseyScalaInflector.method));
        return resourceBuilder.build();
    }

    private static Collection<Routes.Route> getRoutes(TopLevel topLevel) {
        return JavaConverters.asJavaCollectionConverter(topLevel.routes()).asJavaCollection();
    }
}
