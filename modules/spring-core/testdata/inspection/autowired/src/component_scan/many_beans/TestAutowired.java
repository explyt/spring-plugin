package component_scan.many_beans;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class MyComponent {
    @Autowired
    FooInterface one;

    @Autowired
    FooInterface nameComponent;

    @Autowired
    BarInterface primer;

    @Autowired
    Foo13A foo13A;

    @Autowired Foo13B foo13B;
}

@Component
class FooBeanComponent {
    @Autowired FooBean1 fooBean1;
    @Autowired FooBeanInterface fooBeanInterface;
    @Autowired FooBean2 fooBean2;
}
public interface FooInterface {}

@Component
public class FooBean implements FooInterface {}
@Component
public class OtherBean implements FooInterface {}
@Component("nameComponent")
public class ThreeBean implements FooInterface {}

public interface BarInterface {}
@Component
public class BarImpl implements BarInterface {}
@Component
@Primary
public class PrimerBarImpl implements BarInterface {}

@Configuration
public class TestConfiguration {
    @Bean FooBean1 createBean1() {
        return new FooBean1();
    }

    @Primary
    @Bean FooBean1 fooBeanPrimary() {
        return new FooBean1();
    }

    @Bean FooBean2 createBean2() {
        return new FooBean2();
    }
}
interface FooBeanInterface {}
class FooBean1 implements FooBeanInterface {}
class FooBean2 implements FooBeanInterface {}