/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service.conditional

abstract class CompiledBeanConditionTestCase : CompiledConditionTestCase() {

    protected abstract fun addProjectFactoryOf(libraryClass: String)

    fun testCompiledOnBeanClassLiteralMatchedByProjectBeanIsActive() {
        val method = compiledBeanMethod("@ConditionalOnBean(Foo.class)", CONDITIONAL_ON_BEAN, FOO_SOURCE)
        addFooFactory()

        assertEquals(ConditionVerdict.Active, verdictOf(method))
    }

    fun testCompiledOnMissingBeanClassLiteralMatchedByProjectBeanIsInactive() {
        val method = compiledBeanMethod("@ConditionalOnMissingBean(Foo.class)", CONDITIONAL_ON_MISSING_BEAN, FOO_SOURCE)
        addFooFactory()

        assertInactiveWith(verdictOf(method), CONDITIONAL_ON_MISSING_BEAN, "of type $FOO exists")
    }

    private fun addFooFactory() {
        compiledClass(FOO)
        addProjectFactoryOf(FOO)
        val beans = activeBeans()
        assertTrue("Precondition: the project bean of type $FOO must be active, got $beans", FOO_FACTORY in beans)
    }

    companion object {
        const val FOO = "$LIBRARY_PACKAGE.Foo"
        const val FOO_FACTORY = "com.app.FooConfiguration#foo"
        val FOO_SOURCE = mapOf("Foo" to "package $LIBRARY_PACKAGE;\npublic class Foo {}\n")
    }
}
