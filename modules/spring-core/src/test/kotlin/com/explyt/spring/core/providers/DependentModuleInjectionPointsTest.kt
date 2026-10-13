/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.providers

import com.explyt.spring.core.SpringIcons
import com.explyt.spring.core.service.SpringSearchServiceFacade
import com.explyt.spring.test.ExplytMultiModuleTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.test.util.SpringGutterTestUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.uast.UClass
import org.jetbrains.uast.toUElementOfType

/**
 * A bean declared in module `lib` is injected by a component of the main module, which depends on `lib`.
 * The source bean gutter already finds that consumer through the dependent-module component search; the facade
 * search, and the abstract-component marker built on it, must find it too.
 *
 * Java only: in this heavy fixture Kotlin files carry no bean gutters, so the source-gutter control cannot hold there.
 */
class DependentModuleInjectionPointsTest : ExplytMultiModuleTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springBootAutoConfigure_3_1_1,
    )

    fun testFacadeListsTheConsumerInTheDependentModule() {
        val lib = addFixture()
        assertSourceGutterControl(lib)

        val points = facadeInjectionPoints(lib, "com.example.lib.Foo")
        assertTrue("The facade must list the dependent-module consumer like the source gutter does: $points", "foo" in points)
    }

    fun testAbstractComponentMarkerListsTheConsumerInTheDependentModule() {
        val lib = addFixture()
        assertSourceGutterControl(lib)

        val targets = abstractComponentTargets(lib)
        assertTrue("The implementation in lib must be listed: $targets", "FirstFoo" in targets)
        assertTrue("The dependent-module consumer must be listed: $targets", "abstractFoo" in targets)
    }

    private fun addFixture(): Module {
        val lib = addDependencyModule("lib")
        addFileToModule(
            lib, FOO_PATH, """
            package com.example.lib;
            import org.springframework.stereotype.Component;
            @Component public class Foo {}
            """.trimIndent()
        )
        addFileToModule(
            lib, ABSTRACT_FOO_PATH, """
            package com.example.lib;
            import org.springframework.stereotype.Component;
            @Component public abstract class AbstractFoo {}
            """.trimIndent()
        )
        addFileToModule(
            lib, "com/example/lib/FirstFoo.java", """
            package com.example.lib;
            import org.springframework.stereotype.Component;
            @Component public class FirstFoo extends AbstractFoo {}
            """.trimIndent()
        )
        addFileToModule(
            module, "com/example/lib/app/Consumer.java", """
            package com.example.lib.app;
            import com.example.lib.AbstractFoo;
            import com.example.lib.Foo;
            import org.springframework.beans.factory.annotation.Autowired;
            import org.springframework.stereotype.Component;
            @Component public class Consumer {
                @Autowired Foo foo;
                @Autowired AbstractFoo abstractFoo;
            }
            """.trimIndent()
        )
        return lib
    }

    /**
     * The fixture proves nothing unless the consumer is outside `lib`'s own beans and the source gutter, which already
     * adds the dependent-module components, does find it.
     */
    private fun assertSourceGutterControl(lib: Module) {
        val libBeans = SpringSearchServiceFacade.getInstance(project).getAllActiveBeans(lib)
            .mapNotNull { it.psiClass.qualifiedName }
        assertTrue("Foo must be an active bean of lib: $libBeans", "com.example.lib.Foo" in libBeans)
        assertFalse("The consumer must not be a bean of lib itself: $libBeans", "com.example.lib.app.Consumer" in libBeans)

        open(lib, FOO_PATH)
        val targets = SpringGutterTestUtil.getGutterTargetString(
            SpringGutterTestUtil.getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBean)
        ).flatten()
        assertTrue("Control: the source bean gutter on Foo must list the dependent-module consumer: $targets", "foo" in targets)
    }

    private fun abstractComponentTargets(lib: Module): List<String> {
        open(lib, ABSTRACT_FOO_PATH)
        val gutters = SpringGutterTestUtil.getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBeanDependencies)
        assertEquals("AbstractFoo must carry exactly one abstract-component marker", 1, gutters.size)
        return SpringGutterTestUtil.getGutterTargetString(gutters).flatten()
    }

    private fun facadeInjectionPoints(lib: Module, qualifiedName: String): List<String> {
        val psiClass = JavaPsiFacade.getInstance(project).findClass(qualifiedName, GlobalSearchScope.projectScope(project))
            ?: throw AssertionError("$qualifiedName must exist")
        val uClass = psiClass.toUElementOfType<UClass>() ?: throw AssertionError("$qualifiedName must have a UClass")
        return SpringSearchServiceFacade.getInstance(project).findInjectionPoints(uClass, null, lib)
            .map { (it as? PsiNamedElement)?.name ?: it.text }
    }

    private fun open(lib: Module, path: String) {
        val file = ModuleRootManager.getInstance(lib).sourceRoots.first().findFileByRelativePath(path)
            ?: throw AssertionError("$path must exist in lib")
        myFixture.configureFromExistingVirtualFile(file)
        myFixture.doHighlighting()
    }

    companion object {
        private const val FOO_PATH = "com/example/lib/Foo.java"
        private const val ABSTRACT_FOO_PATH = "com/example/lib/AbstractFoo.java"
    }
}
