import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

@Component
public class Foo {

}

@Component
class Bar {
    @Autowired
    Foo foo;
}