import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

class ProductControllerTest {
    void justForTest() {
        MockMvcRequestBuilders.get("/product/{product-id}");
    }
}