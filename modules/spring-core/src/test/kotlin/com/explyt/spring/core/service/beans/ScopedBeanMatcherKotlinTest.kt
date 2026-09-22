/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service.beans

import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiType
import com.intellij.psi.search.GlobalSearchScope

/**
 * Kotlin declarations reach the matcher as light PSI, and their subtyping has to answer the same way Java's does.
 *
 * A Kotlin `object` and a companion `@Bean` are ordinary records by the time they arrive here; what is worth
 * pinning is that their light classes still resolve through the type hierarchy, because a matcher that silently
 * treats an unresolvable Kotlin type as "not compatible" would report such a bean as absent rather than unproven.
 */
class ScopedBeanMatcherKotlinTest : ExplytKotlinLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7)

    fun testKotlinObjectAndCompanionBeanMatchTheirInterface() {
        myFixture.configureByText(
            "Clocks.kt",
            """
            package com.explyt.demo

            interface Clock

            object SystemClock : Clock

            class ClockConfig {
                companion object {
                    fun testClock(): Clock = SystemClock
                }
            }
            """.trimIndent()
        )
        assertNotNull("Fixture must declare the interface", findClass("com.explyt.demo.Clock"))
        assertNotNull("Fixture must declare the object", findClass("com.explyt.demo.SystemClock"))

        val snapshot = snapshotOf(
            record("bean-object", "systemClock", setOf("systemClock"), "com.explyt.demo.SystemClock", typeOf("com.explyt.demo.SystemClock"), BeanKind.COMPONENT),
            record("bean-companion", "testClock", setOf("testClock"), "com.explyt.demo.Clock", typeOf("com.explyt.demo.Clock"), BeanKind.BEAN_METHOD)
        )

        val result = ScopedBeanMatcher(project).lookup(snapshot, BeanLookupSelector("com.explyt.demo.Clock", null))

        assertEquals(BeanOutcome.MULTIPLE, result.outcome)
        assertEquals(MatchCompleteness.COMPLETE, result.match.completeness)
        assertEquals(setOf("bean-object", "bean-companion"), result.match.records.map { it.id }.toSet())
    }

    private fun findClass(fqn: String) =
        JavaPsiFacade.getInstance(project).findClass(fqn, GlobalSearchScope.allScope(project))

    private fun typeOf(fqn: String): PsiType {
        val psiClass = findClass(fqn) ?: error("No PSI for $fqn")
        return JavaPsiFacade.getElementFactory(project).createType(psiClass)
    }

    private fun record(
        id: String,
        name: String,
        knownNames: Set<String>,
        typeName: String?,
        declaredType: PsiType?,
        kind: BeanKind
    ) = ScopedBeanRecord(
        id = id,
        name = name,
        knownNames = knownNames,
        typeName = typeName,
        kind = kind,
        declaration = null,
        declaredType = declaredType,
        declarationModule = null,
        primary = null,
        priority = null,
        details = BeanDetailsEvidence(aliases = knownNames.toList()),
        limitations = emptySet()
    )

    private fun snapshotOf(vararg records: ScopedBeanRecord) = ScopedBeanSnapshot(
        application = BeanApplicationIdentity("com.explyt.demo.App", "demo.main", "app-source"),
        selection = BeanContextSelection(BeanModelSource.NATIVE_SNAPSHOT, null, emptySet()),
        modelStamp = "stamp",
        records = records.toList(),
        limitations = emptySet()
    )
}
