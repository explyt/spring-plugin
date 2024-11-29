import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component("fooFormatter")
internal class FooFormatter {
    fun format(): String {
        return "foo"
    }
}

@Component
internal class FooServiceField {
    @Autowired
    private val fooFormatter: FooFormatter? = null
}

@Component
internal class FooServiceSetter {
    private var fooFormatter: FooFormatter? = null

    @Autowired
    fun setFormatter(fooFormatter: FooFormatter?) {
        this.fooFormatter = fooFormatter
    }
}

@Component
internal class FooServiceConstructor @Autowired constructor(private val fooFormatter: FooFormatter)


