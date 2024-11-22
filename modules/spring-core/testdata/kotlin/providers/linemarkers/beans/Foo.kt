import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Autowired

@Component
open class Foo

@Component
class Bar {
    @Autowired
    lateinit var foo: Foo
}