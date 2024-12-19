import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@FeignClient
@RequestMapping("product")
public class ProductClient {

    @Autowired
    ProductRepository productRepository;

    @GetMapping("/{product-id}")
    public Product getProduct(@PathVariable("product-id") Integer productId) {
        return productRepository.getProduct(productId);
    }

}
