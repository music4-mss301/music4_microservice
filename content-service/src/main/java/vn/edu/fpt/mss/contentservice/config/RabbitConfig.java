package vn.edu.fpt.mss.contentservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarable;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "music4.events";
    public static final String DLX = "music4.dlx";

    @Bean
    public TopicExchange eventsExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DLX, true, false);
    }

    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    /** Queue durable + DLQ (<queue>.dlq), bind queue vào exchange theo các routing key. */
    public static List<Declarable> queueWithDlq(String queueName, String... routingKeys) {
        List<Declarable> list = new ArrayList<>();
        Queue queue = QueueBuilder.durable(queueName)
                .withArgument("x-dead-letter-exchange", DLX)
                .withArgument("x-dead-letter-routing-key", queueName + ".dlq")
                .build();
        Queue dlq = QueueBuilder.durable(queueName + ".dlq").build();
        list.add(queue);
        list.add(dlq);
        list.add(new Binding(dlq.getName(), Binding.DestinationType.QUEUE, DLX, queueName + ".dlq", null));
        TopicExchange exchange = new TopicExchange(EXCHANGE, true, false);
        for (String key : routingKeys) {
            list.add(BindingBuilder.bind(queue).to(exchange).with(key));
        }
        return list;
    }
}
