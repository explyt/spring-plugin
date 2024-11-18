import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import reactor.core.publisher.Mono
import java.time.LocalDateTime

@WebFluxTest
class UserRouterTest(
    @Autowired private val client: WebTestClient
) {
    @Test
    fun `getById user not found`() {
        client
            .get()
            .uri("/api/users/123")
            .exchange()
            .expectStatus()
            .isNotFound
    }

    @Test
    fun `create bad request`() {
        client
            .post()
            .uri("/api/users")
            .exchange()
            .expectStatus()
            .isBadRequest
    }
}