import jakarta.inject.Inject
import org.springframework.beans.BeansException
import org.springframework.beans.factory.BeanFactory
import org.springframework.beans.factory.BeanFactoryAware
import org.springframework.beans.factory.DisposableBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.ContextClosedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.io.File
import java.util.*
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy
import javax.annotation.Resource

@Configuration
@ComponentScan
open class TestConfiguration {
    @Bean
    open fun testBean(): TestBean {
        return TestBean()
    }

    @Bean
    open fun anotherBean(): AnotherBean {
        return AnotherBean()
    }

    @Bean
    open fun namedFile(): File {
        return File("namedFile.txt")
    }

    @Bean
    open fun fileName(): String {
        return "injectedFileName.txt"
    }

    class TestBean {
        @Autowired
        private val anotherBean: AnotherBean? = null

        @Resource(name = "namedFile")
        private val defaultFile: File? = null

        @Inject
        private val jakartaOutsideBean: OutsideBean? = null

        @javax.inject.Inject
        private val javaxOutsideBean: OutsideBean? = null

        @PostConstruct
        private fun javaxInit() {
            println("anotherBean: " + (anotherBean != null))
            println("defaultFile: " + (defaultFile != null))
            println("jakartaOutsideBean: " + (jakartaOutsideBean != null))
            println("javaxOutsideBean: " + (javaxOutsideBean != null))
        }
    }

    class AnotherBean
}

@Component
internal class OutsideBean @Autowired constructor(@field:Value("valueFile.txt") var namedFile: File) : DisposableBean {
    @PostConstruct
    private fun javaxInit() {
        println("On javaxInit")
        namedFile = File("javaxInit_" + namedFile.name)
    }

    @jakarta.annotation.PostConstruct
    private fun jakartaInit() {
        println("On jakartaInit")
        namedFile = File("jakartaInit_" + namedFile.name)
    }

    @PreDestroy
    private fun javaxDestroy() {
        println("On javaxDestroy")
        namedFile = File("javaxDestroy_" + namedFile.name)
    }

    @jakarta.annotation.PreDestroy
    private fun jakartaDestroy() {
        println("On jakartaDestroy")
        namedFile = File("jakartaDestroy_" + namedFile.name)
    }

    @Autowired
    private fun setNamedFileAutowired(fileName: String) {
        namedFile = File("autowired_$fileName")
    }

    @javax.inject.Inject
    private fun setNamedFile(fileName: String) {
        namedFile = File("inject_$fileName")
    }

    @Resource
    private fun setNamedFileResourceJavax(fileName: String) {
        namedFile = File("javax_resource_$fileName")
    }

    @jakarta.annotation.Resource
    private fun setNamedFileResourceJakarta(fileName: String) {
        namedFile = File("jakarta_resource_$fileName")
    }

    @Value("methodValueFile.txt")
    private fun setNamedFileValue(namedFile: File) {
        this.namedFile = namedFile
    }

    @Throws(Exception::class)
    override fun destroy() {
        println("On outsideBean destroy: $this")
        namedFile = File("destroy_" + namedFile.name)
    }

    @EventListener
    fun handleContextStart(cse: ContextClosedEvent?) {
        println("Handling context started event.")
    }

    companion object {
        @kotlin.jvm.JvmStatic
        @DynamicPropertySource
        private fun redisProperties(registry: DynamicPropertyRegistry) {
            registry.add("redis.host") { "dynamic host" }
            registry.add("redis.port") { 1234 }
        }
    }
}

/**
 * Bean PostProcessor that handles destruction of prototype beans
 */
@Component
internal class OutsideDisposableBean : BeanPostProcessor, BeanFactoryAware, DisposableBean {
    private var beanFactory: BeanFactory? = null

    val prototypeBeans: MutableList<Any> = LinkedList()

    @Throws(BeansException::class)
    override fun postProcessBeforeInitialization(bean: Any, beanName: String): Any {
        return bean
    }

    @Throws(BeansException::class)
    override fun postProcessAfterInitialization(bean: Any, beanName: String): Any {
        if (beanFactory!!.isPrototype(beanName)) {
            synchronized(prototypeBeans) {
                prototypeBeans.add(bean)
            }
        }
        return bean
    }

    @Throws(BeansException::class)
    override fun setBeanFactory(beanFactory: BeanFactory) {
        this.beanFactory = beanFactory
    }

    @Throws(Exception::class)
    override fun destroy() {
        synchronized(prototypeBeans) {
            for (bean in prototypeBeans) {
                if (bean is DisposableBean) {
                    try {
                        bean.destroy()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            prototypeBeans.clear()
        }
    }
}