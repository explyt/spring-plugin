import com.not.a.real.package.OrderRepository
import com.Order
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

//import java.util.List
import java.util.UUID

import kotlinx.coroutines.flow.Flow
import reactor.core.publisher.Mono
import reactor.core.publisher.Flux

@RestController
class OrderController {

    @Autowired
    lateinit var orderRepository: OrderRepository

    @GetMapping("orderMono")
    fun orderMono(): Mono<Order> {
        return orderRepository.getOrder()
    }

    @PostMapping("implicitMono")
    fun findOneKt(): Order {
        return Order()
    }

    @GetMapping("orderFlux")
    fun orderFlux(): Flux<Order> {
        return orderRepository.getOrders()
    }


    @GetMapping("orderFlow")
    fun orderFlux(): Flow<Order> {
        return orderRepository.getOrders()
    }


    @GetMapping("orderListExtendsUUID")
    fun orderListExtendsUUID(): Mono<List<out UUID>> {
        return orderRepository.getOrders()
    }

    @GetMapping("orderGeneric")
    fun <T> shouldNotShow(): Mono<T> {
        return orderRepository.getOrders()
    }

    @PostMapping("orderGeneric")
    fun <T> orderGeneric(): Flux<T> {
        return orderRepository.getOrders()
    }

}