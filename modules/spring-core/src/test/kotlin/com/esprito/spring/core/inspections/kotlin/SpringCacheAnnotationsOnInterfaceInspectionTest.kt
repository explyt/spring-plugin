package com.explyt.spring.core.inspections.kotlin

import com.explyt.spring.core.inspections.SpringCacheAnnotationsOnInterfaceInspection
import com.explyt.spring.test.ExplytInspectionKotlinTestCase
import com.explyt.spring.test.TestLibrary
import org.jetbrains.kotlin.test.TestMetadata

class SpringCacheAnnotationsOnInterfaceInspectionTest : ExplytInspectionKotlinTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springBootAutoConfigure_3_1_1)

    @TestMetadata("cacheOnInterface")
    fun testCacheOnInterface() =
        doTest(SpringCacheAnnotationsOnInterfaceInspection())
}
