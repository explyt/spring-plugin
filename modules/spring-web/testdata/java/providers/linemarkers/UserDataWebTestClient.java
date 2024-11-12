import com.example.demo.rest.WebClients;
import com.example.demo.rest.dto.Employee;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles({"test"})
@AutoConfigureMockMvc
public class UserDataWebTestClient {
    @Autowired
    private UserRouter userRouter;

    @Test
    void getUser() {
        WebTestClient client = WebTestClient
                .bindToRouterFunction(userRouter.getFunction())
                .build();

        User user = new User();
        client.get()
                .uri("/users/{userId}", 1)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(User.class)
                .isEqualTo(user);
    }

    @Test
    void deleteUser() {
        WebTestClient client = WebTestClient
                .bindToRouterFunction(userRouter.getFunction())
                .build();

        User user = new User();
        client.delete()
                .uri("/users/{userId}", 1)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(User.class)
                .isEqualTo(user);
    }
}

class User {}
