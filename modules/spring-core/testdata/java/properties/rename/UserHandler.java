import org.springframework.beans.factory.annotation.Value;

public class UserHandler {
    @Value("${server.timing_new.minutes_to_next_claim:3}")
    private Integer fooFromProperties;
}