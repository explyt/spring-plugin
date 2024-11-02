/*
 * Copyright © 2024 Explyt Ltd
 *
 * All rights reserved.
 *
 * This code and software are the property of Explyt Ltd and are protected by copyright and other intellectual property laws.
 *
 * You may use this code under the terms of the Explyt Source License Version 1.0 ("License"), if you accept its terms and conditions.
 *
 * By installing, downloading, accessing, using, or distributing this code, you agree to the terms and conditions of the License.
 * If you do not agree to such terms and conditions, you must cease using this code and immediately delete all copies of it.
 *
 * You may obtain a copy of the License at: https://github.com/explyt/spring-plugin/blob/main/EXPLYT-SOURCE-LICENSE.md
 *
 * Unauthorized use of this code constitutes a violation of intellectual property rights and may result in legal action.
 */

package com.explyt.spring.core.inspections

import com.explyt.inspection.SpringBaseUastLocalInspectionTool
import com.explyt.spring.core.SpringCoreBundle
import com.explyt.spring.core.SpringCoreClasses.ALIAS_FOR
import com.explyt.spring.core.service.AliasUtils
import com.explyt.util.ExplytPsiUtil.getHighlightRange
import com.explyt.util.ExplytPsiUtil.toSourcePsi
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.lang.jvm.annotation.JvmAnnotationAttribute
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import org.jetbrains.uast.*


class SpringAliasBothInterchangeableSetInspection : SpringBaseUastLocalInspectionTool() {

    override fun checkMethod(
        method: UMethod,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<ProblemDescriptor> {
        return check(method, manager, isOnTheFly)
    }

    override fun checkClass(
        aClass: UClass,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<ProblemDescriptor> {
        return check(aClass, manager, isOnTheFly)
    }

    private fun check(
        member: UDeclaration,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<ProblemDescriptor> {
        val problems: MutableList<ProblemDescriptor> = mutableListOf()

        for (uAnnotation in member.uAnnotations) {
            val annotation = uAnnotation.javaPsi ?: continue
            val attributesByRoot = getAliasedAttributesByRoot(annotation)

            for ((rootQn, attributes) in attributesByRoot) {
                val attributeInfos = attributes.toMutableList()
                while (attributeInfos.isNotEmpty()) {
                    val (attribute, aliasedMethods) = attributeInfos.removeFirst()
                    val conflictedAttributes = attributeInfos
                        .filter { it.aliasedMethods == aliasedMethods }
                    attributeInfos.removeAll(conflictedAttributes)

                    val attributeValue = annotation.findAttributeValue(attribute.attributeName)
                        .toSourcePsi() ?: continue

                    if (conflictedAttributes.isNotEmpty()) {
                        val conflictedNames = conflictedAttributes.map { it.attribute.attributeName }
                        problems +=
                            manager.createProblemDescriptor(
                                attributeValue,
                                attributeValue.getHighlightRange(),
                                SpringCoreBundle.message(
                                    "explyt.spring.inspection.alias.interchangeable",
                                    rootQn,
                                    attribute.attributeName,
                                    conflictedNames.toString()
                                ),
                                ProblemHighlightType.GENERIC_ERROR,
                                isOnTheFly
                            )
                    }
                }
            }
        }

        return problems.toTypedArray()
    }

    private fun getAliasedAttributesByRoot(annotation: PsiAnnotation): Map<String, List<AttributeInfo>> {
        val attributesByRoot = mutableMapOf<String, MutableList<AttributeInfo>>()

        val uAnnotation = annotation.toUElement() ?: return attributesByRoot
        var aliasedClass = uAnnotation.tryResolve() as? PsiClass ?: return attributesByRoot
        var aliasedClassQn = aliasedClass.qualifiedName ?: return attributesByRoot

        for (attribute in annotation.attributes) {
            var aliasedName = attribute.attributeName
            var aliasedMethod = aliasedClass
                .methods
                .firstOrNull {
                    it.name == aliasedName
                } ?: continue

            while (true) {
                val aliasAnnotation = AnnotationUtil.findAnnotation(aliasedMethod, ALIAS_FOR)
                if (aliasAnnotation == null) {
                    // deepest part, we don’t refer to anyone else
                    attributesByRoot.addValue(
                        aliasedClassQn,
                        AttributeInfo(attribute, setOf(aliasedName))
                    )
                    break
                }
                val topAliasedClass = AliasUtils.getAliasedClass(aliasAnnotation)
                val nextAliasedMethodName = AliasUtils.getAliasedMethodName(aliasAnnotation)

                if (topAliasedClass == null
                    || topAliasedClass.qualifiedName == aliasedClassQn
                    || nextAliasedMethodName == null
                ) {
                    // referring to another method at the same level
                    attributesByRoot.addValue(
                        aliasedClassQn,
                        AttributeInfo(
                            attribute, setOfNotNull(
                                aliasedName,
                                nextAliasedMethodName
                            )
                        )
                    )
                    break

                }

                aliasedClass = topAliasedClass
                aliasedClassQn = aliasedClass.qualifiedName ?: break
                aliasedName = nextAliasedMethodName
                val nextAliasedMethod = aliasedClass
                    .methods
                    .firstOrNull { it.name == aliasedName }

                if (nextAliasedMethod == null) {
                    attributesByRoot.addValue(
                        aliasedClassQn,
                        AttributeInfo(attribute, setOf(aliasedName))
                    )
                    break
                }
                aliasedMethod = nextAliasedMethod
            }
        }
        return attributesByRoot
    }

    private fun <T> MutableMap<String, MutableList<T>>.addValue(key: String, value: T): List<T> {
        val list = getOrPut(key) { mutableListOf() }
        list.add(value)
        return list
    }

    private data class AttributeInfo(
        val attribute: JvmAnnotationAttribute,
        val aliasedMethods: Set<String>
    )

}