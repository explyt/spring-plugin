import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

@Configuration
@ComponentScan
public class TestConfiguration {

    @Bean TestBean testBean() {
        return new TestBean();
    }

    @Bean AnotherBean anotherBean() {
        return new AnotherBean();
    }

    @Bean File namedFile() {
        return new File("namedFile.txt");
    }

    @Bean String fileName() {
        return "injectedFileName.txt";
    }

    public static class TestBean {
        @Autowired
        private AnotherBean anotherBean;

        @javax.annotation.Resource(name="namedFile")
        private File defaultFile;

        @jakarta.inject.Inject
        private OutsideBean jakartaOutsideBean;

        @javax.inject.Inject
        private OutsideBean javaxOutsideBean;

        @javax.annotation.PostConstruct
        private void javaxInit() {
            System.out.println("anotherBean: " + (anotherBean != null));
            System.out.println("defaultFile: " + (defaultFile != null));
            System.out.println("jakartaOutsideBean: " + (jakartaOutsideBean != null));
            System.out.println("javaxOutsideBean: " + (javaxOutsideBean != null));
        }

    }

    public static class AnotherBean {
    }

}

@Component
class OutsideBean implements DisposableBean {

    @DynamicPropertySource
    private static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("redis.host", () -> "dynamic host");
        registry.add("redis.port", () -> 1234);
    }

    @Value("valueFile.txt")
    File namedFile;

    @Autowired
    OutsideBean(File namedFile) {
        this.namedFile = namedFile;
    }

    @javax.annotation.PostConstruct
    private void javaxInit() {
        System.out.println("On javaxInit");
        namedFile = new File("javaxInit_" + namedFile.getName());
    }

    @jakarta.annotation.PostConstruct
    private void jakartaInit() {
        System.out.println("On jakartaInit");
        namedFile = new File("jakartaInit_" + namedFile.getName());
    }

    @javax.annotation.PreDestroy
    private void javaxDestroy() {
        System.out.println("On javaxDestroy");
        namedFile = new File("javaxDestroy_" + namedFile.getName());
    }

    @jakarta.annotation.PreDestroy
    private void jakartaDestroy() {
        System.out.println("On jakartaDestroy");
        namedFile = new File("jakartaDestroy_" + namedFile.getName());
    }

    @Autowired
    private void setNamedFileAutowired(String fileName) {
        namedFile = new File("autowired_" + fileName);
    }

    @javax.inject.Inject
    private void setNamedFile(String fileName) {
        namedFile = new File("inject_" + fileName);
    }

    @javax.annotation.Resource
    private void setNamedFileResourceJavax(String fileName) {
        namedFile = new File("javax_resource_" + fileName);
    }

    @jakarta.annotation.Resource
    private void setNamedFileResourceJakarta(String fileName) {
        namedFile = new File("jakarta_resource_" + fileName);
    }

    @Value("methodValueFile.txt")
    private void setNamedFileValue(File namedFile) {
        this.namedFile = namedFile;
    }

    @Override
    public void destroy() throws Exception {
        System.out.println("On outsideBean destroy: " + this);
        namedFile = new File("destroy_" + namedFile.getName());
    }

    @EventListener
    public void handleContextStart(ContextClosedEvent cse) {
        System.out.println("Handling context started event.");
    }
}

/**
 * Bean PostProcessor that handles destruction of prototype beans
 */
@Component
class OutsideDisposableBean implements BeanPostProcessor, BeanFactoryAware, DisposableBean {

    private BeanFactory beanFactory;

    final List<Object> prototypeBeans = new LinkedList<>();

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (beanFactory.isPrototype(beanName)) {
            synchronized (prototypeBeans) {
                prototypeBeans.add(bean);
            }
        }
        return bean;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Override
    public void destroy() throws Exception {
        synchronized (prototypeBeans) {
            for (Object bean : prototypeBeans) {
                if (bean instanceof DisposableBean) {
                    DisposableBean disposable = (DisposableBean)bean;
                    try {
                        disposable.destroy();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            prototypeBeans.clear();
        }
    }
}
