/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service

import com.explyt.spring.test.ExplytJavaLightTestCase
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil

/**
 * `AnnotatedElementsSearch` executors assert their preconditions instead of returning empty, and each signals a
 * violation differently — the Java executor throws `IllegalArgumentException("FQN is null for …")`, the Groovy
 * one a message-less `AssertionError`. Both reached users through the cached component-annotation set, so the
 * filter that keeps them out is pinned here.
 */
class SearchableAnnotationTypeTest : ExplytJavaLightTestCase() {

    private fun service() = SpringSearchService.getInstance(project)

    private fun classesOf(file: PsiFile): List<PsiClass> =
        PsiTreeUtil.findChildrenOfType(file, PsiClass::class.java).toList()

    fun testAnnotationTypeWithAQualifiedNameIsSearchable() {
        val file = myFixture.addFileToProject("Marker.java", "package com.demo; public @interface Marker {}")
        val marker = classesOf(file).single()

        assertTrue("a resolvable annotation type must reach the search", service().isSearchableAnnotationType(marker))
    }

    fun testPlainClassIsNotSearchable() {
        val file = myFixture.addFileToProject("NotAnAnnotation.java", "package com.demo; public class NotAnAnnotation {}")
        val psiClass = classesOf(file).single()

        // The Java executor throws "Annotation type should be passed to annotated members search" for this one.
        assertFalse(service().isSearchableAnnotationType(psiClass))
    }

    fun testAnnotationWithoutAQualifiedNameIsNotSearchable() {
        // A local class has no qualified name, which is exactly the shape the executors reject.
        val file = myFixture.addFileToProject(
            "WithLocal.java",
            "package com.demo; public class WithLocal { void m() { @interface Local {} } }"
        )
        val local = classesOf(file).first { it.name == "Local" }

        assertNull("the fixture must produce a class with no qualified name", local.qualifiedName)
        assertTrue("the fixture must still be an annotation type", local.isAnnotationType)
        assertFalse(service().isSearchableAnnotationType(local))
    }

    fun testInvalidatedAnnotationIsNotSearchable() {
        val file = myFixture.addFileToProject("Stale.java", "package com.demo; public @interface Stale {}")
        val stale = classesOf(file).single()
        assertTrue("precondition: the annotation starts out searchable", service().isSearchableAnnotationType(stale))

        WriteCommandAction.runWriteCommandAction(project) { file.virtualFile.delete(this) }

        assertFalse("the deleted annotation must be invalidated", stale.isValid)
        assertFalse(service().isSearchableAnnotationType(stale))
    }
}
