import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("fooFormatter")
class FooFormatter {
    public String format() {
        return "foo";
    }
}

@Component
class FooServiceField {
    @Autowired
    private FooFormatter fooFormatter;
}

@Component
class FooServiceSetter {
    private FooFormatter fooFormatter;
    @Autowired
    public void setFormatter(FooFormatter fooFormatter) {
        this.fooFormatter = fooFormatter;
    }
}

@Component
class FooServiceConstructor {
    private FooFormatter fooFormatter;
    @Autowired
    public FooServiceConstructor(FooFormatter fooFormatter) {
        this.fooFormatter = fooFormatter;
    }
}

