package no.found.scala.jersey;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.models.Swagger;
import org.glassfish.jersey.CommonProperties;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.Assert;
import org.junit.Test;
import scala.Option;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import java.util.Collections;

public class Tester extends JerseyTest {
    @Test
    public void postTest() {
        Model model = new Model("orderId: 453", 10, Option.empty());
        String response = target("/hey").request().post(Entity.json(model), String.class);
        Assert.assertTrue("orderId: 453".equals(response));
    }

    @Test
    public void getTest() {
        Model response = target("/hey/101").request().get(Model.class);
        Assert.assertTrue(Option.apply("101").equals(response.id()));
    }

    @Override
    protected void configureClient(ClientConfig config) {
        config.register(JacksonMarshallingFeature.class);
        config.property(CommonProperties.FEATURE_AUTO_DISCOVERY_DISABLE, Boolean.TRUE);
    }

    @Override
    protected Application configure() {
        Swagger swagger = JerseyBuilder.buildSwagger(Collections.singletonList(new ApiResource()));
        try {
            System.out.println(JacksonObjectMapperProvider.mapper.writeValueAsString(swagger));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.registerResources(JerseyBuilder.buildResource(new ApiResource()));
        resourceConfig.register(JacksonMarshallingFeature.class);
        resourceConfig.register(JacksonFeature.class);
        resourceConfig.property(CommonProperties.FEATURE_AUTO_DISCOVERY_DISABLE, Boolean.TRUE);
        return resourceConfig;
    }
}
