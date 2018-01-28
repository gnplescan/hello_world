package hello_world.resources;

import com.codahale.metrics.annotation.Timed;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.GetResponse;
import hello_world.api.MessageStatus;
import hello_world.api.Saying;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

@Path("/hello-world")
@Produces(MediaType.APPLICATION_JSON)
public class HelloWorldResource {
    private final static String QUEUE_NAME = "hello";
    private final String template;
    private final String defaultName;
    private final AtomicLong counter;

    public HelloWorldResource(String template, String defaultName) {
        this.template = template;
        this.defaultName = defaultName;
        this.counter = new AtomicLong();
    }

    @GET
    @Timed
    public Saying sayHello(@QueryParam("name") Optional<String> name) {
        final String value = String.format(template, name.orElse(defaultName));
        return new Saying(counter.incrementAndGet(), value);
    }

    @POST
    @Path("/send")
    @Produces("application/json")
    public MessageStatus send(@FormParam("message") Optional<String> message) throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        channel.queueDeclare(QUEUE_NAME, false, false, false, null);
        final String value = message.orElse("Hello World!");
        channel.basicPublish("", QUEUE_NAME, null, value.getBytes());
        channel.close();
        connection.close();
        return new MessageStatus(counter.incrementAndGet(), "Sent '" + value + "'");
    }

    @GET
    @Path("/recv")
    @Produces("application/json")
    public MessageStatus recv() throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        channel.queueDeclare(QUEUE_NAME, false, false, false, null);
        GetResponse response = channel.basicGet(QUEUE_NAME, false);
        if (response == null) {
            return new MessageStatus(counter.incrementAndGet(), "No message received");
        } else {
            String value = new String(response.getBody(), "UTF-8");
            long deliveryTag = response.getEnvelope().getDeliveryTag();
            channel.basicAck(deliveryTag, false);
            return new MessageStatus(counter.incrementAndGet(), "Received '" + value + "'");
        }
    }
}