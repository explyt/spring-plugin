package component_scan.no_bean

import component_scan.constructor.BarBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Service
import java.util.*

@Service
open class MyComponent {
    @Autowired
    var notOptional: Optional<NotImplInterface>? = null

    @Autowired
    var notList: List<NotImplInterface>? = null

    @Autowired
    var notCollection: Collection<NotImplInterface>? = null

    @Autowired
    var notSet: Set<NotImplInterface>? = null

    @Autowired
    var noArray: Array<BarBean>
    var other: OtherBean? = null

    @Autowired
    fun setOther(value: OtherBean?) {
        other = value
    }

    @Autowired
    var beanA: MyBean? = null
}

open class MyBean

open interface NotImplInterface

open class OtherBean {
    @Bean
    fun myBean(): MyBean {
        return MyBean()
    }
}

open class FooBean
open class FooBeanClass {
    @Autowired
    var bean: FooBean? = null
}

@Configuration
open class ConfigurationBean {
    @Bean
    open fun createBean(): ClassBeanExist {
        return ClassBeanExist()
    }
}

open class ClassNotBeanExist {
    @Autowired
    var injectBean: InjectBean? = null
}

open class ClassBeanExist {
    @Autowired
    var injectBean: InjectBean? = null
}

open interface InjectBean

@Service
open class IBComponent : InjectBean
