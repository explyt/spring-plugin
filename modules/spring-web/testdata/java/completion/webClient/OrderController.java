import com.baeldung.graphqlvsrest.entity.Order;
import com.baeldung.graphqlvsrest.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class OrderController {

    @Autowired
    OrderRepository orderRepository;

    @GetMapping("order")
    public List<Order> getOrders(@RequestParam("product-id") Integer productId){
        return orderRepository.getOrdersByProduct(productId);
    }
}
