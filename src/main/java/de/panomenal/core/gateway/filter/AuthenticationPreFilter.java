package de.panomenal.core.gateway.filter;

import java.util.List;
import java.util.function.Predicate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import de.panomenal.core.gateway.data.ConnValidationResponse;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class AuthenticationPreFilter extends AbstractGatewayFilterFactory<AuthenticationPreFilter.Config> {

    @Autowired
    @Qualifier("excludedUrls")
    List<String> excludedUrls;

    @Autowired
    private final WebClient.Builder webClientBuilder;

    public AuthenticationPreFilter(WebClient.Builder webClientBuilder) {
        super(Config.class);
        this.webClientBuilder = webClientBuilder;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            log.info("**************************************************************************");
            log.info("URL is - " + request.getURI().getPath());
            String bearerToken = request.getHeaders().getFirst("Authenticated");
            log.info("Bearer Token: " + bearerToken);

            if (isSecured.test(request)) {
                return webClientBuilder.build().post()
                        .uri("lb://authenticationService/api/v1/auth/verify")
                        .header("Authenticated", bearerToken)
                        .retrieve().bodyToMono(ConnValidationResponse.class)
                        .map(response -> {
                            exchange.getRequest().mutate().header("username", response.getUsername());
                            exchange.getRequest().mutate().header("authorities", response.getAuthorities().stream()
                                    .reduce("", (a, b) -> a + "," + b));

                            return exchange;
                        }).flatMap(chain::filter).onErrorResume(error -> {
                            log.info("Error Happened");
                            HttpStatus errorCode = null;
                            String errorMsg = "";
                            if (error instanceof WebClientResponseException) {
                                WebClientResponseException webCLientException = (WebClientResponseException) error;
                                errorCode = (HttpStatus) webCLientException.getStatusCode();
                                errorMsg = webCLientException.getLocalizedMessage();

                            } else if (error instanceof WebClientRequestException) {
                                errorMsg = error.getLocalizedMessage();
                            } else {
                                errorCode = HttpStatus.BAD_GATEWAY;
                                errorMsg = HttpStatus.BAD_GATEWAY.getReasonPhrase();
                            }
                            // AuthorizationFilter.AUTH_FAILED_CODE
                            throw new RuntimeException(errorMsg);
                        });
            }
            return chain.filter(exchange);
        };
    }

    public Predicate<ServerHttpRequest> isSecured = request -> excludedUrls.stream()
            .noneMatch(uri -> request.getURI().getPath().contains(uri));

    @NoArgsConstructor
    public static class Config {

    }

}
