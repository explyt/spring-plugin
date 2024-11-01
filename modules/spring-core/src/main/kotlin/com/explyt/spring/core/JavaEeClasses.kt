package com.explyt.spring.core

import com.explyt.util.MultiVendorClass
import kotlin.reflect.KProperty

object JavaEeClasses {
    val INJECT by "inject.Inject"
    val RESOURCE by "annotation.Resource"
    val POST_CONSTRUCT by "annotation.PostConstruct"
    val PRE_DESTROY by "annotation.PreDestroy"
    val PRIORITY by "annotation.Priority"
    val NAMED by "inject.Named"
    val QUALIFIER by "inject.Qualifier"
    val TRANSACTIONAL by "transaction.Transactional"

    private operator fun String.getValue(jpaClasses: JavaEeClasses, property: KProperty<*>): MultiVendorClass {
        return MultiVendorClass(this)
    }

}