package no.found.scala.jersey;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jaxrs.Jaxrs2TypesModule;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule;
import com.fasterxml.jackson.module.scala.DefaultScalaModule;

import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Provider;

@Provider
@Produces(MediaType.APPLICATION_JSON)
public class JacksonObjectMapperProvider extends JacksonJaxbJsonProvider {

    public static final ObjectMapper mapper = new ObjectMapper();

    static {
        mapper.registerModule(new DefaultScalaModule());
        mapper.registerModule(new JaxbAnnotationModule());
        mapper.registerModule(new Jaxrs2TypesModule());
    }

    public JacksonObjectMapperProvider() {
        super(mapper, DEFAULT_ANNOTATIONS);
    }
}
