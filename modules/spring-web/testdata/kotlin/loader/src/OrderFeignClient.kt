import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(value = "OrderFeignClient", url = "https://market.com", path = "/v1")
interface OrderFeignClient {
    @PostMapping(value = "order")
    fun order(@RequestBody dto: Object): Object
}