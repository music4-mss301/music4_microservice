package vn.edu.fpt.mss.searchadminservice.config;

import org.springframework.amqp.core.Declarables;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitTopologyConfig {

    public static final String Q_EVENTS = "search.content.events";

    /** Nhận mọi thay đổi của content để đồng bộ ES. */
    @Bean
    public Declarables searchQueues() {
        return new Declarables(RabbitConfig.queueWithDlq(Q_EVENTS,
                "content.track.*", "content.album.*", "content.artist.*", "content.category.*"));
    }
}
