package component_scan.many_beans

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component

@Configuration
open class MyComponent {
    @Autowired
    var one: FooInterface? = null

    @Autowired
    var nameComponent: FooInterface? = null

    @Autowired
    var primer: BarInterface? = null
}

@Component
open class FooBeanComponent {
    @Autowired
    var fooBean1: FooBean1? = null

    @Autowired
    var fooBeanInterface: FooBeanInterface? = null

    @Autowired
    var fooBean2: FooBean2? = null
}

interface FooInterface

@Component
open class FooBean : FooInterface

@Component
open class OtherBean : FooInterface

@Component("nameComponent")
internal class ThreeBean : FooInterface

internal interface BarInterface

@Component
internal class BarImpl : BarInterface

@Component
@Primary
internal class PrimerBarImpl : BarInterface

@Configuration
open class TestConfiguration {
    @Bean
    open fun createBean1(): FooBean1 {
        return FooBean1()
    }

    @Primary
    @Bean
    open fun fooBeanPrimary(): FooBean1 {
        return FooBean1()
    }

    @Bean
    open fun createBean2(): FooBean2 {
        return FooBean2()
    }
}

interface FooBeanInterface
class FooBean1 : FooBeanInterface
class FooBean2 : FooBeanInterface