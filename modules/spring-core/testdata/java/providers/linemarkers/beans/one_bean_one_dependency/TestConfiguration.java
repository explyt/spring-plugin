import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

class FooAOneBeanOneDependency {}

class FooBOneBeanOneDependency {}

@Component
class FooCOneBeanOneDependency {}

@Configuration
public class TestConfiguration {
    @Bean
    FooAOneBeanOneDependency fooAOneBeanOneDependency() { return new FooAOneBeanOneDependency(); }
}

@Component
class FooComponentOneBeanOneDependency {
    @Autowired
    FooAOneBeanOneDependency fooAOneBeanOneDependency;

    @Autowired
    FooBOneBeanOneDependency fooBOneBeanOneDependency;

    @Autowired
    FooCOneBeanOneDependency fooCOneBeanOneDependency;
}
