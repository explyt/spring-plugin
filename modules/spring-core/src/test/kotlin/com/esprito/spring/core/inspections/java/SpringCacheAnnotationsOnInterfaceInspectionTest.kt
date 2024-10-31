package com.esprito.spring.core.inspections.java

import com.esprito.spring.core.inspections.SpringCacheAnnotationsOnInterfaceInspection
import com.esprito.spring.test.ExplytInspectionJavaTestCase
import com.esprito.spring.test.TestLibrary
import org.jetbrains.kotlin.test.TestMetadata

class SpringCacheAnnotationsOnInterfaceInspectionTest : ExplytInspectionJavaTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springBootAutoConfigure_3_1_1)

    @TestMetadata("cacheOnInterface")
    fun testCacheOnInterface() =
        doTest(SpringCacheAnnotationsOnInterfaceInspection())
}