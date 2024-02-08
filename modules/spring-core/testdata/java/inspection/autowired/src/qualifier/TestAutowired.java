package component_scan.qualifier;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Qualifier;

@Configuration
class MyComponent {
    @Qualifier("fooBean")
    @Autowired
    FooInterface testQualifier1;
    
    @Qualifier("not")
    @Autowired
    FooInterface testQualifier2;
    
    @Qualifier("nameQualifier")
    @Autowired
    FooInterface testQualifier3;
}

interface FooInterface {
}

@Component
class FooBean implements FooInterface {
}

@Component
class OtherBean implements FooInterface {
}

@Component
@Qualifier("nameQualifier")
class AnotherBean implements FooInterface {
}