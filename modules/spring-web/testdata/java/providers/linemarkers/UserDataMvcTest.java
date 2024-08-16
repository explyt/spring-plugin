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
public class UserDataMvcTest {
    @Autowired
    private MockMvc mvc;

    @Test
    void getUser() throws Exception {
        ResultActions resultActions = mvc.perform(get("/users/{userId}", 1)
                .contentType(MediaType.APPLICATION_JSON));

        resultActions.andExpect(status().isOk());
    }

    @Test
    void deleteUser() throws Exception {
        ResultActions resultActions = mvc.perform(delete("/users/{userId}", 1)
                .contentType(MediaType.APPLICATION_JSON));

        resultActions.andExpect(status().isOk());
    }

    @Test
    void getCustomers() throws Exception {
        ResultActions resultActions = mvc.perform(get("/users/customers")
                .contentType(MediaType.APPLICATION_JSON));

        String contentAsString = resultActions.andReturn().getResponse().getContentAsString();

        resultActions.andExpect(status().isOk());
    }

}

