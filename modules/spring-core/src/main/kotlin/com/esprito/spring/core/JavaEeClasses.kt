package com.esprito.spring.core

import com.esprito.util.MultiVendorClass
import kotlin.reflect.KProperty

object JavaEeClasses {
    val INJECT by "inject.Inject"
    val RESOURCE by "annotation.Resource"
    val POST_CONSTRUCT by "annotation.PostConstruct"
    val PRE_DESTROY by "annotation.PreDestroy"
    val NAMED by "inject.Named"
    val TRANSACTIONAL by "transaction.Transactional"

    private operator fun String.getValue(jpaClasses: JavaEeClasses, property: KProperty<*>): MultiVendorClass {
        return MultiVendorClass(this)
    }

}