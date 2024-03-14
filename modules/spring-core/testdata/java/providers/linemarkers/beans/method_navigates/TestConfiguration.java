import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

class Foo {}

@Configuration
public class TestBeanByName {
    @Bean({"namedBeanFoo"}) Foo fooUnusedName() { return new Foo(); }
    @Bean Foo foo() { return new Foo(); }
    @Bean Foo fooOther() {return new Foo(); }
}


@Component
class FooComponent1 {
    @Autowired Foo namedBeanFoo; /** Target {@link TestBeanByName#fooUnusedName()} by bean name*/
}

@Component
class FooComponent2 {
    @Autowired Foo foo; /** Target {@link TestBeanByName#foo} by name */
}