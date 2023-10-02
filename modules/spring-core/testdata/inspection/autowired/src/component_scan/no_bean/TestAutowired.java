package component_scan.no_bean;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
class MyComponent {
    @Autowired
    Optional<NotImplInterface> notOptional;

    @Autowired
    List<NotImplInterface> notList;

    @Autowired
    Collection<NotImplInterface> notCollection;

    @Autowired
    Set<NotImplInterface> notSet;

    @Autowired
    BarBean[] noArray;
    OtherBean other;

    @Autowired
    void setOther(OtherBean value) {
        other = value;
    }

    @Autowired
    MyBean beanA;
}

class MyBean {
}

interface NotImplInterface {
}

class OtherBean {
    @Bean
    public MyBean myBean() {
        return new MyBean();
    }
}

class FooBean {}
class FooBeanClass {
    @Autowired
    FooBean bean;
}