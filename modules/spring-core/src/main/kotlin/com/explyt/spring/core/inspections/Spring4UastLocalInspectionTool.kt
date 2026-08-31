/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections

import com.explyt.inspection.SpringBaseUastLocalInspectionTool
import com.explyt.spring.core.util.SpringBootUtil
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaFile
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.uast.UAnnotation

/**
 * Base class for UAST inspections that only apply to Spring Boot 4+ projects.
 *
 * The Spring Boot version is checked once per file in [isAvailableForFile] (instead of on every visited PSI element),
 * so the inspection never builds a visitor or walks PSI in projects below the required version.
 *
 * Migration inspections must gate on the **replacement** being resolvable, never on the legacy symbol: the upgrade
 * that makes them relevant is what removes the legacy artifact. The `legacy*Fqn` helpers below recognise a symbol
 * that no longer resolves, so the stale source is still reported.
 */
abstract class Spring4UastLocalInspectionTool : SpringBaseUastLocalInspectionTool() {

    override fun isAvailableForFile(file: PsiFile): Boolean {
        return super.isAvailableForFile(file) && isSupportedBootVersion(file)
    }

    /**
     * The Spring Boot version floor of this inspection, evaluated once per file.
     *
     * Defaults to Spring Boot 4, the version that performs the reported removals. An inspection whose replacement
     * already exists in an earlier version overrides this to lower the floor, so it can also help during the
     * deprecation window that precedes the removal.
     */
    protected open fun isSupportedBootVersion(file: PsiFile): Boolean = SpringBootUtil.isAtLeastSpringBoot4(file)

    protected fun isClassAvailable(file: PsiFile, fqn: String): Boolean {
        return JavaPsiFacade.getInstance(file.project).findClass(fqn, file.resolveScope) != null
    }

    protected fun isAnyClassAvailable(file: PsiFile, vararg fqns: String): Boolean {
        return fqns.any { isClassAvailable(file, it) }
    }

    /**
     * The FQN from [legacyFqns] that [annotation] denotes, or `null` when it denotes none of them.
     *
     * A resolved annotation is matched by its own qualified name. An annotation whose declaring artifact the Boot 4
     * upgrade removed does not resolve, and is matched by the name as written plus the file's imports.
     */
    protected fun legacyAnnotationFqn(annotation: UAnnotation, legacyFqns: Collection<String>): String? {
        annotation.qualifiedName?.takeIf { it in legacyFqns }?.let { return it }
        if (annotation.resolve() != null) return null

        val sourcePsi = annotation.sourcePsi ?: return null
        return legacyFqn(sourcePsi.containingFile, writtenAnnotationName(annotation), legacyFqns)
    }

    /**
     * The legacy FQN that the type at [typeSourcePsi] denotes, or `null` when the type is unrelated or already
     * migrated. [migrations] maps every legacy FQN to its replacement.
     */
    protected fun legacyTypeFqn(
        typeSourcePsi: PsiElement,
        canonicalText: String?,
        migrations: Map<String, String>
    ): String? {
        if (canonicalText != null) {
            if (canonicalText in migrations.keys) return canonicalText
            // A usage that resolves to the replacement is already migrated.
            if (canonicalText in migrations.values) return null
        }
        val writtenName = typeSourcePsi.text?.substringBefore('<')?.trim()
        return legacyFqn(typeSourcePsi.containingFile, writtenName, migrations.keys)
    }

    /** The name of [annotation] as spelled in the source, which is all that survives when it does not resolve. */
    protected fun writtenAnnotationName(annotation: UAnnotation): String? =
        when (val sourcePsi = annotation.sourcePsi) {
            is PsiAnnotation -> sourcePsi.nameReferenceElement?.text
            is KtAnnotationEntry -> sourcePsi.typeReference?.text
            else -> null
        }?.substringBefore('<')?.trim()

    private fun legacyFqn(file: PsiFile?, writtenName: String?, legacyFqns: Collection<String>): String? {
        if (writtenName == null) return null
        // A fully qualified usage carries the FQN itself and needs no import.
        legacyFqns.firstOrNull { it == writtenName }?.let { return it }
        if (file == null) return null
        return legacyFqns.firstOrNull { importsAs(file, it, writtenName) }
    }

    /** Whether [file] imports [fqn] under the name [writtenName]: explicitly, on demand, or via a Kotlin alias. */
    private fun importsAs(file: PsiFile, fqn: String, writtenName: String): Boolean {
        val packageName = fqn.substringBeforeLast('.')
        val shortName = fqn.substringAfterLast('.')
        return when (file) {
            is KtFile -> file.importDirectives.any { directive ->
                val importedFqName = directive.importedFqName?.asString()
                when {
                    directive.isAllUnder -> importedFqName == packageName && writtenName == shortName
                    importedFqName != fqn -> false
                    else -> writtenName == (directive.aliasName ?: shortName)
                }
            }

            is PsiJavaFile -> writtenName == shortName && file.importList?.importStatements?.any {
                val qualifiedName = it.qualifiedName
                if (it.isOnDemand) qualifiedName == packageName else qualifiedName == fqn
            } == true

            else -> false
        }
    }
}
