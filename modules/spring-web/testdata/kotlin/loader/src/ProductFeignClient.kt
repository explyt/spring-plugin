import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(value = "productFeignClient")
interface ProductFeignClient {
    @PostMapping(value = "product")
    fun product(@RequestBody dto: Object): Object
}