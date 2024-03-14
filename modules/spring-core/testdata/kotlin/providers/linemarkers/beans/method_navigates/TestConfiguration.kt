import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component

class Foo

@Configuration
open class TestBeanByName {
    @Bean("namedBeanFoo")
    open fun fooUnusedName(): Foo {
        return Foo()
    }

    @Bean
    open fun foo(): Foo {
        return Foo()
    }

    @Bean
    open fun fooOther(): Foo {
        return Foo()
    }
}


@Component
internal class FooComponent1 {
    @Autowired
    var namedBeanFoo: Foo? = null
    /** Target {@link TestBeanByName.fooUnusedName} by bean name */
}

@Component
internal class FooComponent2 {
    @Autowired
    var foo: Foo? = null
    /** Target {@link TestBeanByName.foo} by name  */
}