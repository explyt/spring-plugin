import com.baeldung.graphqlvsrest.entity.Order
import com.baeldung.graphqlvsrest.repository.OrderRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class OrderController {
    @Autowired
    lateinit var orderRepository: OrderRepository

    @GetMapping("order")
    fun getOrders(@RequestParam("product-id") productId: Int?): List<Order> {
        return orderRepository?.getOrdersByProduct(productId)
    }
}