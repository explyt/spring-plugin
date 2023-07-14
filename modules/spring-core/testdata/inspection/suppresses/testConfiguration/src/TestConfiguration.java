import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TestConfiguration {

    @Bean
    public TestBean testBean() {
        return new TestBean();
    }

    private static class TestBean {

    }
}
