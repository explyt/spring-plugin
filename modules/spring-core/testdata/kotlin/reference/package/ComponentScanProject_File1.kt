package pack1.pack2.pack3

import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration

@Configuration
@ComponentScan(basePackages = ["pack1.pack2.pack3<caret>"])
open class TestConfiguration