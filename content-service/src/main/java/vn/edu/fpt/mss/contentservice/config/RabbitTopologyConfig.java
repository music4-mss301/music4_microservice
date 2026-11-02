package vn.edu.fpt.mss.contentservice.config;

import org.springframework.amqp.core.Declarables;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vn.edu.fpt.mss.contentservice.event.EventTypes;

@Configuration
public class RabbitTopologyConfig {

    public static final String Q_TRACK_PLAYED = "content.analytics.track.played";

    @Bean
    public Declarables contentQueues() {
        return new Declarables(RabbitConfig.queueWithDlq(Q_TRACK_PLAYED, EventTypes.TRACK_PLAYED));
    }
}
