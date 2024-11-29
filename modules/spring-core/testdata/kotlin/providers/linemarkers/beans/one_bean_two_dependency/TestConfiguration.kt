import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component

internal class FooAOneBeanTwoDependency

internal class FooBOneBeanTwoDependency

@Configuration
internal class TestConfigurationOneBeanTwoDependency {
    @Bean
    fun foo(): FooAOneBeanTwoDependency {
        return FooAOneBeanTwoDependency()
    }
}

@Component
internal class FooAOneBeanTwoDependencyComponent1 {
    @Autowired
    var foo: FooAOneBeanTwoDependency? = null
}

@Component
internal class FooAOneBeanTwoDependencyComponent2 {
    @Autowired
    var foo: FooAOneBeanTwoDependency? = null
}

@Component
internal class FooBOneBeanTwoDependencyComponent1 {
    @Autowired
    var foo: FooBOneBeanTwoDependency? = null
}

@Component
internal class FooBOneBeanTwoDependencyComponent2 {
    @Autowired
    var foo: FooBOneBeanTwoDependency? = null
}