import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
open class TestComponent {
    @Value(value = "\${<PLACE_TO_INSERT>}")
    private val value: String = ""
}
