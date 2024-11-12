import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("product")
public class ProductController {
    
    @Autowired
    ProductRepository productRepository;
    
    @GetMapping("/{product-id}")
    public Product getProduct(@PathVariable("product-id") Integer productId) {
        return productRepository.getProduct(productId);
    }
    
}
