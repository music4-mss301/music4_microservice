package vn.edu.fpt.mss.analyticsservice.config;

import vn.edu.fpt.mss.analyticsservice.security.GatewayAuthFilter;
import vn.edu.fpt.mss.analyticsservice.security.SecurityProperties;
import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Mọi Feign client gọi /internal/** của service khác đều kèm X-Internal-Token. */
@Configuration
public class FeignInternalConfig {

    @Bean
    public RequestInterceptor internalTokenInterceptor(SecurityProperties props) {
        return template -> template.header(GatewayAuthFilter.HEADER_INTERNAL_TOKEN, props.internalToken());
    }
}
