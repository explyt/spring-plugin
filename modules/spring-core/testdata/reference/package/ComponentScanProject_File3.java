package pack1.pack2_2.pack3;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@ComponentScan({"pack1.*.pa<caret>"})
@Configuration
public class TestConfiguration {

}
