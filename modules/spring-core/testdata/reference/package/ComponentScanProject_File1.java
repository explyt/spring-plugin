package pack1.pack2.pack3;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@ComponentScan(basePackages = {"pack1.pack2.pack3<caret>"})
@Configuration
public class TestConfiguration {

}
