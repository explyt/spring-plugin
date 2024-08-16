import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@org.springframework.stereotype.Component
public class UserHandler {
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
