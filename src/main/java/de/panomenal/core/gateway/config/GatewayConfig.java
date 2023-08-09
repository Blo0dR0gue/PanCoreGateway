package de.panomenal.core.gateway.config;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import de.panomenal.core.gateway.filter.AuthenticationPreFilter;

@Configuration
public class GatewayConfig {

        @Value("${pancore.gateway.excludedURLsNew}")
        private String urlsStrings;

        @Bean
        @Qualifier("excludedUrls")
        public List<String> excludedUrls() {
                return Arrays.stream(urlsStrings.split(",")).collect(Collectors.toList());
        }

        @Bean
        @LoadBalanced
        public WebClient.Builder loadBalancedWebClientBuilder() {
                return WebClient.builder();
        }

        @Bean
        public RouteLocator routes(RouteLocatorBuilder builder, AuthenticationPreFilter authFilter) {
                return builder.routes()
                                .route("auth-service-route", r -> r.path("/authenticationService/**")
                                                .filters(f -> f.rewritePath("/authenticationService(?<segment>/?.*)",
                                                                "/api/v1/auth$\\{segment}")
                                                                .filter(authFilter.apply(
                                                                                new AuthenticationPreFilter.Config())))
                                                .uri("lb://authenticationService"))
                                .build();
        }

}
