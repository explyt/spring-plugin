package component_scan.qualifier;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Qualifier;

@Configuration
public class MyComponent {
    @Qualifier("fooBean")
    @Autowired
    FooInterface testQualifier;

    @Qualifier("not")
    @Autowired
    FooInterface testQualifier;

    @Qualifier("nameQualifier")
    @Autowired
    FooInterface testQualifier;
}

public interface FooInterface {
}

@Component
public class FooBean implements FooInterface {
}

@Component
public class OtherBean implements FooInterface {
}

@Component
@Qualifier("nameQualifier")
public class AnotherBean implements FooInterface {
}