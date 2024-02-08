package component_scan.many_beans;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
class MyComponent {
    @Autowired
    FooInterface one;

    @Autowired
    FooInterface nameComponent;

    @Autowired
    BarInterface primer;
}

@Component
class FooBeanComponent {
    @Autowired FooBean1 fooBean1;
    @Autowired FooBeanInterface fooBeanInterface;
    @Autowired FooBean2 fooBean2;
}
interface FooInterface {}

@Component
class FooBean implements FooInterface {}
@Component
class OtherBean implements FooInterface {}
@Component("nameComponent")
class ThreeBean implements FooInterface {}

interface BarInterface {}
@Component
class BarImpl implements BarInterface {}
@Component
@Primary
class PrimerBarImpl implements BarInterface {}

@Configuration
class TestConfiguration {
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