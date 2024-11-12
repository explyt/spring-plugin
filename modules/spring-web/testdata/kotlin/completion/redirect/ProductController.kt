import com.baeldung.graphqlvsrest.entity.Product
import com.baeldung.graphqlvsrest.model.ProductModel
import com.baeldung.graphqlvsrest.repository.ProductRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Pageable
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("product")
class ProductController {
    @Autowired
    lateinit var productRepository: ProductRepository

    @GetMapping("/get/it")
    fun getProducts(pageable: Pageable): List<Product> {
        return productRepository.getProducts(pageable.getPageSize(), pageable.getPageNumber())
    }

    @GetMapping("/{product-id}")
    fun getProduct(@PathVariable("product-id") productId: Int?): Product {
        return productRepository.getProduct(productId)
    }

    @PutMapping("/{product-id}")
    fun update(@PathVariable("product-id") productId: Int?, @RequestBody productModel: ProductModel?): Product {
        return productRepository.update(productId, productModel)
    }
}