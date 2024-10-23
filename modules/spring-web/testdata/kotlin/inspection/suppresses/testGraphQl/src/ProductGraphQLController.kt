import com.baeldung.graphqlvsrest.entity.Order
import com.baeldung.graphqlvsrest.entity.Product
import com.baeldung.graphqlvsrest.model.ProductModel
import com.baeldung.graphqlvsrest.repository.OrderRepository
import com.baeldung.graphqlvsrest.repository.ProductRepository
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.stereotype.Controller

@Controller
class ProductGraphQLController(
    private val productRepository: ProductRepository,
    private val orderRepository: OrderRepository
) {

    @QueryMapping
    fun products(@Argument size: Int, @Argument page: Int): List<Product> {
        return productRepository.getProducts(size, page)
    }

    @QueryMapping
    fun product(@Argument id: Int): Product {
        return productRepository.getProduct(id)
    }

    @MutationMapping
    fun saveProduct(@Argument product: ProductModel?): Product {
        return productRepository.save(product)
    }

    @MutationMapping
    fun updateProduct(@Argument id: Int?, @Argument product: ProductModel?): Product {
        return productRepository.update(id, product)
    }

    @SchemaMapping(typeName = "Product", field = "orders")
    fun getOrders(product: Product): List<Order> {
        return orderRepository.getOrdersByProduct(product.getId())
    }
}
