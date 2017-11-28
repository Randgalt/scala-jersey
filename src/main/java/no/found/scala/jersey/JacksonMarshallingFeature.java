package no.found.scala.jersey;

import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

public class JacksonMarshallingFeature implements Feature {
    @Override
    public boolean configure(FeatureContext context) {
        context.register(JacksonObjectMapperProvider.class, MessageBodyReader.class, MessageBodyWriter.class);
        return true;
    }
}
