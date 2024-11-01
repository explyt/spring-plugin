import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TestComponent {
	@Value(value = "${<PLACE_TO_INSERT>}")
	private String value;
}
