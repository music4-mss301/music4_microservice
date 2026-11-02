package vn.edu.fpt.mss.contentservice.config;

import vn.edu.fpt.mss.contentservice.exception.ApiError;
import vn.edu.fpt.mss.contentservice.security.GatewayAuthFilter;
import vn.edu.fpt.mss.contentservice.security.SecurityProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final GatewayAuthFilter gatewayAuthFilter;
    private final SecurityProperties props;
    private final ObjectMapper objectMapper;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) ->
                                write(res, HttpStatus.UNAUTHORIZED, "Bạn cần đăng nhập", req.getRequestURI()))
                        .accessDeniedHandler((req, res, ex) ->
                                write(res, HttpStatus.FORBIDDEN, "Bạn không có quyền truy cập", req.getRequestURI())))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers("/actuator/**", "/v3/api-docs/**", "/swagger-ui/**",
                            "/swagger-ui.html", "/error").permitAll();
                    auth.requestMatchers("/internal/**").hasRole("INTERNAL");
                    props.publicGetPaths().forEach(p -> auth.requestMatchers(HttpMethod.GET, p).permitAll());
                    props.publicPaths().forEach(p -> auth.requestMatchers(p).permitAll());
                    auth.anyRequest().authenticated();
                })
                .addFilterBefore(gatewayAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /** Filter chỉ chạy trong Security chain, không tự đăng ký thêm lần nữa vào servlet chain. */
    @Bean
    public FilterRegistrationBean<GatewayAuthFilter> gatewayAuthFilterRegistration(GatewayAuthFilter filter) {
        FilterRegistrationBean<GatewayAuthFilter> reg = new FilterRegistrationBean<>(filter);
        reg.setEnabled(false);
        return reg;
    }

    private void write(HttpServletResponse res, HttpStatus status, String msg, String path) throws IOException {
        res.setStatus(status.value());
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(res.getWriter(), ApiError.of(status, msg, path));
    }
}
