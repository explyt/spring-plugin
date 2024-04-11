package component_scan.constructor

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service

@Service
open class MyComponent {
    @Autowired
    constructor(bean: BarBean?)

    @Autowired
    constructor()

    @Autowired
    constructor(foobean: FooBean?, bean: BarBean?)

    @Autowired(required = false)
    constructor(foobean: FooBean?, bean: BarBean?, str: String?)
}

@Component
open class FooBean

@Service
open class MyFactory {
    constructor(str: String?)
    constructor(count: Int)
}

@Component
class ConstructorProperties(
    var param: String? = null
)