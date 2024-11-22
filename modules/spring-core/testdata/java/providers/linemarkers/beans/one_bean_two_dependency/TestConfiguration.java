import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component;

class FooAOneBeanTwoDependency{}

class FooBOneBeanTwoDependency{}

@Configuration
class TestConfigurationOneBeanTwoDependency {
    @Bean
    FooAOneBeanTwoDependency foo() { return new FooAOneBeanTwoDependency(); }
}

@Component
class FooAOneBeanTwoDependencyComponent1 {
    @Autowired FooAOneBeanTwoDependency foo;
}

@Component
class FooAOneBeanTwoDependencyComponent2 {
    @Autowired FooAOneBeanTwoDependency foo;
}

@Component
class FooBOneBeanTwoDependencyComponent1 {
    @Autowired FooBOneBeanTwoDependency foo;
}

@Component
class FooBOneBeanTwoDependencyComponent2 {
    @Autowired FooBOneBeanTwoDependency foo;
}