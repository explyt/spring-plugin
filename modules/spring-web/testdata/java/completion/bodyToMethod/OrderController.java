import com.not.a.real.package.OrderRepository;
import com.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

@RestController
public class OrderController {

    @Autowired
    OrderRepository orderRepository;

    @GetMapping("orderMono")
    public Mono<Order> orderMono() {
        return orderRepository.getOrder();
    }

    @GetMapping("orderFlux")
    public Flux<Order> orderFlux() {
        return orderRepository.getOrders();
    }

    @GetMapping("orderListExtendsUUID")
    public Mono<List<? extends UUID>> orderListExtendsUUID() {
        return orderRepository.getOrders();
    }

    @GetMapping("orderGeneric")
    public <T> Mono<T> shouldNotShow() {
        return orderRepository.getOrders();
    }

    @PostMapping("orderGeneric")
    public <T> Flux<T> orderGeneric() {
        return orderRepository.getOrders();
    }

}