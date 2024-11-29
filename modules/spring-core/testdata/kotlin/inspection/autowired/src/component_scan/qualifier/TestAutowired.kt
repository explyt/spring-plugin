package component_scan.qualifier

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component

@Configuration
open class MyComponent {
    @Qualifier("fooBean")
    @Autowired
    var testQualifier1: FooInterface? = null

    @Qualifier("not")
    @Autowired
    var testQualifier2: FooInterface? = null

    @Qualifier("nameQualifier")
    @Autowired
    var testQualifier3: FooInterface? = null
}

interface FooInterface

@Component
open class FooBean : FooInterface

@Component
open class OtherBean : FooInterface

@Component
@Qualifier("nameQualifier")
open class AnotherBean : FooInterface