import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource

@Configuration
@PropertySource("classpath:configuration-dir/abc.properties")
open class MyConfig{
}