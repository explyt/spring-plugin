package component_scan.constructor;

import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

@Service
class MyComponent {
    
    @Autowired
    public MyComponent(BarBean bean) {
    }

    @Autowired
    public MyComponent() {
    }

    @Autowired
    public MyComponent(FooBean foobean, BarBean bean) {
    }

    @Autowired(required = false)
    public MyComponent(FooBean foobean, BarBean bean, String str) {
    }
}


@Component
class FooBean {
}

@Service
class MyFactory {
    public MyFactory(String str) {
    }
    public MyFactory(int count) {
    }
}