/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service.beans

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.JavaEeClasses
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifierListOwner
import com.intellij.psi.PsiParameter
import com.intellij.psi.PsiVariable
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UDeclaration
import org.jetbrains.uast.UField
import org.jetbrains.uast.UParameter
import org.jetbrains.uast.UVariable
import org.jetbrains.uast.toUElement
import org.jetbrains.uast.toUElementOfType

/**
 * Finds the injection point a caller points at by line and column.
 *
 * The whole line is scanned rather than the first character of it, because a declaration rarely starts at the
 * line start and a Kotlin primary constructor can hold several parameters on one line. When more than one
 * declaration sits on the line and no column narrows it down, the caller is asked instead of being served the
 * first one - answering about the wrong parameter is indistinguishable from answering about the right one.
 */
class SpringInjectionPointResolver(private val project: Project) {

    /** Must run under a read action. */
    fun resolve(file: PsiFile, line: Int, column: Int?): SpringInjectionPoint {
        val document = PsiDocumentManager.getInstance(project).getDocument(file)
            ?: throw invalidArgument("The file has no document.")
        if (line < 1 || line > document.lineCount) {
            throw invalidArgument("Line $line is outside the file (1..${document.lineCount}).")
        }
        val lineStart = document.getLineStartOffset(line - 1)
        val lineEnd = document.getLineEndOffset(line - 1)
        if (column != null && (column < 1 || lineStart + column - 1 > lineEnd)) {
            throw invalidArgument("Column $column is outside line $line.")
        }

        val onLine = declarationsOn(file, lineStart, lineEnd)
        if (onLine.isEmpty()) throw unsupported("No injectable declaration on line $line.")

        val variable = pick(onLine, column?.let { lineStart + it - 1 }, line, document.getLineStartOffset(line - 1))
        return describe(variable)
    }

    private fun declarationsOn(file: PsiFile, lineStart: Int, lineEnd: Int): List<PsiVariable> {
        val found = LinkedHashMap<PsiVariable, Unit>()
        var offset = lineStart
        while (offset <= lineEnd) {
            val leaf = file.findElementAt(offset)
            if (leaf == null) {
                offset++
                continue
            }
            uVariableAround(leaf)?.let { found[it] = Unit }
            offset = maxOf(offset + 1, leaf.textRange.endOffset)
        }
        return found.keys.filter { it.textRange != null && it.textRange.startOffset <= lineEnd && it.textRange.endOffset >= lineStart }
    }

    /**
     * Kotlin declarations have no [PsiVariable] in their own tree, so the UAST view is what turns a `KtParameter`
     * into the parameter Spring sees. The Java path resolves through the same call.
     */
    private fun uVariableAround(leaf: PsiElement): PsiVariable? {
        var element: PsiElement? = leaf
        while (element != null && element !is PsiFile) {
            val uVariable = element.toUElementOfType<UVariable>()
            if (uVariable is UParameter || uVariable is UField) {
                (uVariable.javaPsi as? PsiVariable)?.let { return it }
            }
            element = element.parent
        }
        return null
    }

    private fun pick(candidates: List<PsiVariable>, offset: Int?, line: Int, lineStart: Int): PsiVariable {
        if (offset != null) {
            val atColumn = candidates.filter { it.textRange.containsOffset(offset) }
            if (atColumn.size == 1) return atColumn.single()
            if (atColumn.isEmpty() && candidates.size == 1) return candidates.single()
            if (atColumn.isEmpty()) throw injectionPointRequired(candidates, line, lineStart)
            return atColumn.first()
        }
        if (candidates.size == 1) return candidates.single()
        throw injectionPointRequired(candidates, line, lineStart)
    }

    private fun describe(variable: PsiVariable): SpringInjectionPoint {
        if (!isSupportedPlace(variable)) {
            throw unsupported("${variable.name ?: "This declaration"} is not a Spring injection point.")
        }
        val facts = InjectionCapabilityPolicy.inspect(variable)
        return SpringInjectionPoint(
            name = variable.name.orEmpty(),
            declaredType = variable.type,
            beanType = InjectionCapabilityPolicy.beanType(variable.type),
            variable = variable,
            facts = facts
        )
    }

    /**
     * Only the places Spring actually injects into are answered. A class with several constructors and no
     * `@Autowired` is reported as unsupported rather than guessed at: Spring picks one by rules this model does
     * not evaluate, and naming the wrong one would read as a fact.
     */
    private fun isSupportedPlace(variable: PsiVariable): Boolean = when (variable) {
        is PsiField -> variable.hasInjectionAnnotation()
        is PsiParameter -> {
            val method = PsiTreeUtil.getParentOfType(variable, PsiMethod::class.java)
            method != null && method.acceptsInjectedParameters()
        }

        else -> false
    }

    private fun PsiMethod.acceptsInjectedParameters(): Boolean {
        if (hasInjectionAnnotation() || annotatedWith(SpringCoreClasses.BEAN)) return true
        if (!isConstructor) return false
        val owner = containingClass ?: return false
        if (!owner.isComponentLike()) return false
        if (owner.constructors.any { it != this && it.hasInjectionAnnotation() }) return false
        return owner.sourceConstructorCount() == 1
    }

    /**
     * Kotlin publishes one light constructor per defaulted-parameter overload, all sharing a single
     * `KtPrimaryConstructor`. Counting the light ones would read a class with a default value as having several
     * constructors and refuse to answer about it, so the declarations behind them are what is counted.
     */
    private fun PsiClass.sourceConstructorCount(): Int =
        constructors.mapTo(HashSet()) { it.navigationElement }.size

    private fun PsiClass.isComponentLike(): Boolean {
        val uClass = toUElementOfType<UClass>() ?: return false
        return uClass.uAnnotations.any { annotation ->
            val fqn = annotation.qualifiedName ?: return@any false
            fqn in COMPONENT_ANNOTATIONS || isMetaComponent(fqn, this)
        }
    }

    /**
     * A stereotype may be meta-annotated - `@RestController` is a `@Controller` - so one level of indirection is
     * followed before a class is rejected. The annotation is looked up in the owner's own classpath.
     */
    private fun isMetaComponent(fqn: String, owner: PsiClass): Boolean {
        val module = ModuleUtilCore.findModuleForPsiElement(owner) ?: return false
        val scope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module, false)
        val annotationClass = JavaPsiFacade.getInstance(project).findClass(fqn, scope) ?: return false
        return annotationClass.annotations.any { it.qualifiedName in COMPONENT_ANNOTATIONS }
    }

    private fun PsiModifierListOwner.hasInjectionAnnotation(): Boolean =
        annotatedWith(SpringCoreClasses.AUTOWIRED) || JavaEeClasses.INJECT.allFqns.any { annotatedWith(it) }

    private fun PsiModifierListOwner.annotatedWith(fqn: String): Boolean =
        modifierList?.findAnnotation(fqn) != null ||
                (toUElement() as? UDeclaration)?.uAnnotations?.any { it.qualifiedName == fqn } == true

    private fun invalidArgument(message: String) =
        BeanQueryException(BeanQueryProblem(INVALID_ARGUMENT, message))

    private fun unsupported(message: String) =
        BeanQueryException(BeanQueryProblem(UNSUPPORTED_INJECTION_POINT, message))

    private fun injectionPointRequired(candidates: List<PsiVariable>, line: Int, lineStart: Int) =
        BeanQueryException(
            BeanQueryProblem(
                INJECTION_POINT_REQUIRED,
                "More than one declaration on line $line; name the column.",
                candidates.map {
                    mapOf(
                        "name" to it.name.orEmpty(),
                        "line" to line.toString(),
                        "column" to (it.textRange.startOffset - lineStart + 1).toString()
                    )
                }
            )
        )

    companion object {
        const val INVALID_ARGUMENT = "INVALID_ARGUMENT"
        const val UNSUPPORTED_INJECTION_POINT = "UNSUPPORTED_INJECTION_POINT"
        const val INJECTION_POINT_REQUIRED = "INJECTION_POINT_REQUIRED"

        private val COMPONENT_ANNOTATIONS = setOf(
            SpringCoreClasses.COMPONENT,
            SpringCoreClasses.SERVICE,
            SpringCoreClasses.CONTROLLER,
            SpringCoreClasses.REPOSITORY,
            SpringCoreClasses.CONFIGURATION
        )
    }
}
