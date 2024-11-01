import org.springframework.test.web.servlet.request.MockMvcRequestBuilders

class ProductControllerTest {
    fun justForTest() {
        MockMvcRequestBuilders.get("/product/{product-id}")
    }
}