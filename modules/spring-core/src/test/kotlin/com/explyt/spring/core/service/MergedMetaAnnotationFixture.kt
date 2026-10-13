/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service

import com.intellij.openapi.module.Module
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertTrue
import org.jetbrains.uast.UClass

import org.jetbrains.uast.toUElement

object MergedMetaAnnotationFixture {
    const val TARGET = "merged.Target"

    fun addJavaCases(fixture: CodeInsightTestFixture) {
        fixture.addFileToProject(
            "merged/Cases.java",
            """
            package merged;
            import org.springframework.core.annotation.AliasFor;
            import java.lang.annotation.Retention;
            import static java.lang.annotation.RetentionPolicy.RUNTIME;

            @Retention(RUNTIME) @interface Target {
                @AliasFor("mode") String value() default "T";
                @AliasFor("value") String mode() default "T";
                String[] names() default {"T"};
            }

            @Retention(RUNTIME) @Target @interface C1 { @AliasFor(annotation = Target.class, attribute = "mode") String m() default "A"; }
            @C1 class Case1 {}
            @C1(m = "B") class Case2 {}

            @Retention(RUNTIME) @Target @interface C3 { String mode() default "A"; }
            @C3 class Case3a {}
            @C3(mode = "B") class Case3b {}
            @Retention(RUNTIME) @Target(mode = "META") @interface C3m { String mode() default "A"; }
            @C3m class Case3c {}

            @Retention(RUNTIME) @C1 @interface O4a {}
            @O4a class Case4a {}
            @Retention(RUNTIME) @C1 @interface O4b { @AliasFor(annotation = C1.class, attribute = "m") String o() default "O"; }
            @O4b class Case4b {}
            @O4b(o = "X") class Case4c {}

            @Retention(RUNTIME) @Target @interface PA { @AliasFor(annotation = Target.class, attribute = "mode") String m() default "PA"; }
            @Retention(RUNTIME) @Target @interface PB { @AliasFor(annotation = Target.class, attribute = "mode") String m() default "PB"; }
            @Retention(RUNTIME) @PA @PB @interface O5 { @AliasFor(annotation = PA.class, attribute = "m") String o() default "O"; }
            @O5 class Case5a {}
            @Retention(RUNTIME) @PB @PA @interface O5r { @AliasFor(annotation = PA.class, attribute = "m") String o() default "O"; }
            @O5r class Case5c {}
            @Retention(RUNTIME) @PA @PB @interface O5n {}
            @O5n class Case5e {}

            @Retention(RUNTIME) @C1(m = "FIX") @interface O6 { @AliasFor(annotation = C1.class, attribute = "m") String o() default "O"; }
            @O6 class Case6a {}

            @Retention(RUNTIME) @Target @interface C7 { @AliasFor(annotation = Target.class, attribute = "mode") String value() default "A"; }
            @C7("B") class Case7a {}
            @C7 class Case7b {}
            @Retention(RUNTIME) @Target @interface C7v { String value() default "A"; }
            @C7v("B") class Case7e {}

            @Retention(RUNTIME) @Target(names = {"META"}) @interface C8 { String[] names() default {"A"}; }
            @C8 class Case8a {}
            @C8(names = {"B", "C"}) class Case8b {}
            @Retention(RUNTIME) @Target(names = {"META"}) @interface C8a { @AliasFor(annotation = Target.class, attribute = "names") String[] n() default {"A"}; }
            @C8a class Case8c {}

            @Retention(RUNTIME) @Target(names = {"META"}) @interface E1 { @AliasFor(annotation = Target.class, attribute = "names") String[] n() default {}; }
            @E1 class Case9a {}
            @Retention(RUNTIME) @Target(mode = "META") @interface E2 { @AliasFor(annotation = Target.class, attribute = "mode") String m() default ""; }
            @E2 class Case9b {}
            @Retention(RUNTIME) @Target(mode = "META") @interface E3 { String mode() default ""; }
            @E3 class Case9c {}
            """.trimIndent()
        )
    }

    fun addKotlinCases(fixture: CodeInsightTestFixture) {
        fixture.addFileToProject(
            "merged/Cases.kt",
            """
            package merged
            import org.springframework.core.annotation.AliasFor

            annotation class Target(
                @get:AliasFor("mode") val value: String = "T",
                @get:AliasFor("value") val mode: String = "T",
                val names: Array<String> = ["T"],
            )

            @Target
            annotation class C1(@get:AliasFor(annotation = Target::class, attribute = "mode") val m: String = "A")
            @C1 class Case1
            @C1(m = "B") class Case2

            @Target(names = ["META"])
            annotation class C8a(@get:AliasFor(annotation = Target::class, attribute = "names") val n: Array<String> = ["A"])
            @C8a class Case8c
            """.trimIndent()
        )
    }

    fun mergedValues(
        fixture: CodeInsightTestFixture,
        module: Module,
        useSite: String,
        composed: String,
        attribute: String,
    ): List<String> {
        val project = fixture.project
        val useSiteClass = JavaPsiFacade.getInstance(project)
            .findClass("merged.$useSite", GlobalSearchScope.projectScope(project))
        assertNotNull("Use site merged.$useSite is in the fixture", useSiteClass)
        val annotation = (useSiteClass!!.toUElement() as UClass).uAnnotations
            .singleOrNull { it.qualifiedName == "merged.$composed" }
        assertNotNull("merged.$composed is on merged.$useSite", annotation)
        assertEquals("merged.$composed resolves", "merged.$composed", annotation!!.resolve()?.qualifiedName)

        val holder = MetaAnnotationsHolder.of(module, TARGET)
        assertEquals(TARGET, holder.getRootClassQualified())
        assertTrue("merged.$composed is meta-annotated with $TARGET", holder.contains(annotation))

        return holder.getAnnotationMemberValues(annotation, setOf(attribute))
            .map { it.evaluate() as? String ?: "<not a constant: ${it.sourcePsi?.text}>" }
    }
}
