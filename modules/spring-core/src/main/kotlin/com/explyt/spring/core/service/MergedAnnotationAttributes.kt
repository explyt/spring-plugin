/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.SpringProperties
import com.explyt.spring.core.util.SpringBootUtil
import com.explyt.util.ExplytPsiUtil.resolveUAnnotationType
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiAnnotationMethod
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UAnnotation
import org.jetbrains.uast.UAnnotationMethod
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.UVariable
import org.jetbrains.uast.toUElement

class MergedAnnotationAttributes(
    private val rootFqn: String,
    private val mayComposeRoot: (PsiClass) -> Boolean,
    conventionMapping: Lazy<Boolean>,
) {
    private val conventionMapping by conventionMapping

    sealed interface Source {
        data class Declared(val annotation: PsiAnnotation, val attribute: String) : Source
        data class Default(val attribute: PsiMethod) : Source
    }

    class ConventionOverride(val useSite: Source.Declared, val root: PsiAnnotation)

    private class Level(val type: PsiClass, val annotation: PsiAnnotation)

    fun resolve(useSite: PsiAnnotation, targetAttributes: Set<String>): List<Source>? {
        val path = metaPathOf(useSite) ?: return null
        return attributeGroups(path.last().type, targetAttributes).mapNotNull { sourceOf(path, it) }
    }

    fun conventionOverrideOf(useSite: PsiAnnotation, targetAttribute: String): ConventionOverride? {
        if (!conventionMapping) return null
        val path = metaPathOf(useSite)?.takeIf { it.size > 1 } ?: return null
        val group = mirrorsOf(path.last().type, listOf(targetAttribute))
        if (aliasesAlong(path, group).first().isNotEmpty()) return null
        val conventionNames = conventionAttributes(path.first().type, group).ifEmpty { return null }
        val declared = pick(path.first(), conventionNames) as? Source.Declared ?: return null
        return ConventionOverride(declared, path.last().annotation)
    }

    private fun metaPathOf(useSite: PsiAnnotation): List<Level>? {
        val start = Level(annotationTypeOf(useSite) ?: return null, useSite)
        val visited = mutableSetOf(start.type.qualifiedName ?: return null)
        val pending = ArrayDeque(listOf(listOf(start)))
        while (pending.isNotEmpty()) {
            ProgressManager.checkCanceled()
            val path = pending.removeFirst()
            val last = path.last()
            if (last.type.qualifiedName == rootFqn) return path
            for (meta in last.type.annotations) {
                if (isLanguageAnnotation(meta)) continue
                val type = annotationTypeOf(meta) ?: continue
                val fqn = type.qualifiedName ?: continue
                if (fqn != rootFqn && !mayComposeRoot(type)) continue
                if (visited.add(fqn)) pending += path + Level(type, meta)
            }
        }
        return null
    }

    private fun attributeGroups(rootType: PsiClass, targetAttributes: Set<String>): List<Set<String>> =
        targetAttributes.fold(mutableListOf()) { groups, target ->
            if (groups.none { target in it }) groups += mirrorsOf(rootType, listOf(target))
            groups
        }

    private fun sourceOf(path: List<Level>, group: Set<String>): Source? {
        val aliases = aliasesAlong(path, group)
        aliases.first().ifNotEmpty { return pick(path.first(), it) }
        if (conventionMapping) conventionAttributes(path.first().type, group).ifNotEmpty { return pick(path.first(), it) }
        for (level in 1 until path.lastIndex) {
            aliases[level].ifNotEmpty { return pick(path[level], it) }
        }
        return pick(path.last(), group)
    }

    private fun aliasesAlong(path: List<Level>, group: Set<String>): List<Set<String>> {
        val levelOf = path.withIndex().associate { (index, level) -> level.type.qualifiedName to index }
        val aliases = List(path.size) { linkedSetOf<String>() }
        aliases.last() += group
        for (level in path.indices.reversed()) {
            val type = path[level].type
            do {
                ProgressManager.checkCanceled()
                val known = aliases[level].size
                for (attribute in type.methods) {
                    val (targetType, targetAttribute) = aliasTargetOf(type, attribute) ?: continue
                    val targetLevel = levelOf[targetType.qualifiedName]?.takeIf { it >= level } ?: continue
                    if (targetAttribute in aliases[targetLevel]) aliases[level] += attribute.name
                    if (targetLevel == level && attribute.name in aliases[level]) aliases[level] += targetAttribute
                }
            } while (aliases[level].size != known)
        }
        return aliases
    }

    private fun conventionAttributes(type: PsiClass, group: Set<String>): Set<String> {
        val conventionNames = type.methods
            .filter { it.name != SpringProperties.VALUE && it.name in group && !overridesExplicitly(type, it) }
            .map { it.name }
        return if (conventionNames.isEmpty()) emptySet() else mirrorsOf(type, conventionNames)
    }

    private fun overridesExplicitly(type: PsiClass, attribute: PsiMethod): Boolean {
        val (targetType, _) = aliasTargetOf(type, attribute) ?: return false
        return targetType.qualifiedName != type.qualifiedName
    }

    private fun mirrorsOf(type: PsiClass, names: Collection<String>): Set<String> {
        val mirrors = LinkedHashSet(names)
        do {
            ProgressManager.checkCanceled()
            val known = mirrors.size
            for (attribute in type.methods) {
                val (targetType, targetAttribute) = aliasTargetOf(type, attribute) ?: continue
                if (targetType.qualifiedName != type.qualifiedName) continue
                if (attribute.name in mirrors) mirrors += targetAttribute
                if (targetAttribute in mirrors) mirrors += attribute.name
            }
        } while (mirrors.size != known)
        return mirrors
    }

    private fun pick(level: Level, names: Set<String>): Source? {
        names.firstOrNull { level.annotation.findDeclaredAttributeValue(it) != null }
            ?.let { return Source.Declared(level.annotation, it) }
        return names.firstNotNullOfOrNull { level.type.findMethodsByName(it, false).firstOrNull() }
            ?.let { Source.Default(it) }
    }

    private inline fun Set<String>.ifNotEmpty(action: (Set<String>) -> Unit) {
        if (isNotEmpty()) action(this)
    }

    companion object {
        private const val LAST_SPRING_MAJOR_WITH_CONVENTION_MAPPING = 6
        private val LANGUAGE_ANNOTATION_PACKAGES = listOf("java.", "javax.annotation.", "kotlin.")

        fun conventionMappingApplies(module: Module?): Boolean {
            val major = module?.let { SpringBootUtil.getSpringCoreMajorVersion(it) } ?: return false
            return major <= LAST_SPRING_MAJOR_WITH_CONVENTION_MAPPING
        }

        fun moduleOf(element: PsiElement): Module? =
            ModuleUtilCore.findModuleForPsiElement(element)
                ?: element.containingFile?.originalFile?.let { ModuleUtilCore.findModuleForPsiElement(it) }

        fun isLanguageAnnotation(annotation: PsiAnnotation): Boolean {
            val annotationName = annotation.qualifiedName ?: return false
            return LANGUAGE_ANNOTATION_PACKAGES.any { annotationName.startsWith(it) }
        }

        fun annotationTypeOf(annotation: PsiAnnotation): PsiClass? {
            val type = annotation.resolveUAnnotationType()
                ?: annotation.qualifiedName?.let {
                    JavaPsiFacade.getInstance(annotation.project).findClass(it, annotation.resolveScope)
                }
            return type?.takeIf { it.isAnnotationType }
        }

        fun defaultValueOf(attribute: PsiMethod): UExpression? =
            (attribute.toUElement() as? UAnnotationMethod)?.uastDefaultValue
                ?: (attribute.navigationElement.toUElement() as? UVariable)?.uastInitializer
                ?: (attribute as? PsiAnnotationMethod)?.defaultValue?.toUElement() as? UExpression

        private fun aliasTargetOf(type: PsiClass, attribute: PsiMethod): Pair<PsiClass, String>? {
            val alias = attribute.getAnnotation(SpringCoreClasses.ALIAS_FOR) ?: return null
            val targetType = aliasedClassOf(alias) ?: type
            return targetType to (AliasUtils.getAliasedMethodName(alias) ?: attribute.name)
        }

        private fun aliasedClassOf(alias: PsiAnnotation): PsiClass? {
            val uAlias = alias.toUElement() as? UAnnotation
            val target = uAlias?.let { AliasUtils.getAliasedClass(it) } ?: AliasUtils.getAliasedClass(alias)
            return target?.takeIf { it.isAnnotationType }
        }
    }
}
