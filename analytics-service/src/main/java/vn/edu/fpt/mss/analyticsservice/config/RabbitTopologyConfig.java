package vn.edu.fpt.mss.analyticsservice.config;

import org.springframework.amqp.core.Declarables;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vn.edu.fpt.mss.analyticsservice.event.EventTypes;

@Configuration
public class RabbitTopologyConfig {

    public static final String Q_EVENTS = "analytics.events";

    @Bean
    public Declarables analyticsQueues() {
        return new Declarables(RabbitConfig.queueWithDlq(Q_EVENTS, EventTypes.USER_DELETED));
    }
}
