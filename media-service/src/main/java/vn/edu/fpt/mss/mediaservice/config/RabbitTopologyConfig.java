package vn.edu.fpt.mss.mediaservice.config;

import org.springframework.amqp.core.Declarables;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vn.edu.fpt.mss.mediaservice.event.EventTypes;

@Configuration
public class RabbitTopologyConfig {

    public static final String Q_EVENTS = "media.events";

    @Bean
    public Declarables mediaQueues() {
        return new Declarables(RabbitConfig.queueWithDlq(Q_EVENTS, EventTypes.TRACK_DELETED));
    }
}
