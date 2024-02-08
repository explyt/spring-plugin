package pack1.pack2_2.pack3_2

import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration

@Configuration
@ComponentScan(basePackages = ["pack1.*.pack3_2<caret>"])
open class TestConfiguration