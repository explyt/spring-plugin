import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.*;

import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.web.reactive.function.server.RequestPredicates.*;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
public class WebClients {

    private static final RequestPredicate ACCEPT_JSON = accept(APPLICATION_JSON);

    @Autowired
    private UserHandler userHandler;

    @Bean
    public RouterFunction<ServerResponse> oneRoute() {
        return route()
                .GET("/users", ACCEPT_JSON, userHandler::getUser)
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> differentFunction() {
        return route()
                .GET("/users/all", ACCEPT_JSON, userHandler::getUser)
                .GET("/users/customers", ACCEPT_JSON, userHandler::getUserCustomers)
                .POST("/users/customers", ACCEPT_JSON, userHandler::getUserCustomers)
                .DELETE("/users/{userId}", ACCEPT_JSON, userHandler::deleteUser)
                .build();
    }
}

@Component
class UserHandler {
    public Mono<ServerResponse> getUser(ServerRequest request) {
        return Mono.empty();
    }
    public Mono<ServerResponse> getUserCustomers(ServerRequest request) {
        return Mono.empty();
    }
    public Mono<ServerResponse> deleteUser(ServerRequest request) {
        return Mono.empty();
    }
}
