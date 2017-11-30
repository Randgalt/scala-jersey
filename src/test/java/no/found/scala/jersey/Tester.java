package no.found.scala.jersey;

import org.glassfish.hk2.utilities.binding.AbstractBinder;
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
import java.math.BigDecimal;

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
        ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.registerResources(new JerseyBuilder().buildResource(new ApiResource()));
        resourceConfig.register(JacksonMarshallingFeature.class);
        resourceConfig.register(JacksonFeature.class);
        resourceConfig.property(CommonProperties.FEATURE_AUTO_DISCOVERY_DISABLE, Boolean.TRUE);

        AbstractBinder binder = new AbstractBinder() {
            @Override
            protected void configure() {
                bind(BigDecimal.TEN).to(BigDecimal.class);
            }
        };
        resourceConfig.register(binder);

        return resourceConfig;
    }
}
