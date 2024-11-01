package autowired

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
class TestInjectBean {
    @Autowired
    ApplicationContext context;
}