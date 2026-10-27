package vn.edu.fpt.mss.engagementservice.config;

import org.springframework.amqp.core.Declarables;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vn.edu.fpt.mss.engagementservice.event.EventTypes;

@Configuration
public class RabbitTopologyConfig {

    public static final String Q_EVENTS = "engagement.events";

    @Bean
    public Declarables engagementQueues() {
        return new Declarables(RabbitConfig.queueWithDlq(Q_EVENTS, EventTypes.TRACK_CREATED, EventTypes.ALBUM_CREATED,
                EventTypes.TRACK_DELETED, EventTypes.ARTIST_DELETED, EventTypes.USER_DELETED));
    }
}
