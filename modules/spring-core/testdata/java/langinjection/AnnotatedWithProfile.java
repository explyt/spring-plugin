import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("someProfile<caret>Name")
public class AnnotatedWithProfile {
}
