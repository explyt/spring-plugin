import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component

class FooAOneBeanOneDependency

internal class FooBOneBeanOneDependency

@Component
internal class FooCOneBeanOneDependency

@Configuration
open class TestConfiguration {
    @Bean
    open fun fooAOneBeanOneDependency(): FooAOneBeanOneDependency {
        return FooAOneBeanOneDependency()
    }
}

@Component
internal class FooComponentOneBeanOneDependency {
    @Autowired
    var fooAOneBeanOneDependency: FooAOneBeanOneDependency? = null

    @Autowired
    var fooBOneBeanOneDependency: FooBOneBeanOneDependency? = null

    @Autowired
    var fooCOneBeanOneDependency: FooCOneBeanOneDependency? = null
}