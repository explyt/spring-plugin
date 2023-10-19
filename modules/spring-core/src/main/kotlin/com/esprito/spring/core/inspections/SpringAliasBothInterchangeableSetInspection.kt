package com.esprito.spring.core.inspections

import com.esprito.spring.core.SpringCoreBundle
import com.esprito.spring.core.SpringCoreClasses.ALIAS_FOR
import com.esprito.spring.core.service.AliasUtils
import com.esprito.util.EspritoPsiUtil.getHighlightRange
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.lang.jvm.annotation.JvmAnnotationAttribute
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod


class SpringAliasBothInterchangeableSetInspection : AbstractBaseJavaLocalInspectionTool() {

    override fun checkMethod(
        method: PsiMethod,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<ProblemDescriptor> {
        return check(method, manager, isOnTheFly)
    }

    override fun checkClass(
        aClass: PsiClass,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<ProblemDescriptor> {
        return check(aClass, manager, isOnTheFly)
    }

    private fun check(
        member: PsiMember,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<ProblemDescriptor> {
        val problems: MutableList<ProblemDescriptor> = mutableListOf()

        for (annotation in member.annotations) {
            val attributesByRoot = getAliasedAttributesByRoot(annotation)

            for ((rootQn, attributes) in attributesByRoot) {
                val attributeInfos = attributes.toMutableList()
                while (attributeInfos.isNotEmpty()) {
                    val (attribute, aliasedMethods) = attributeInfos.removeFirst()
                    val conflictedAttributes = attributeInfos
                        .filter { it.aliasedMethods == aliasedMethods }
                    attributeInfos.removeAll(conflictedAttributes)

                    val attributeValue = annotation.findAttributeValue(attribute.attributeName) ?: continue
                    if (conflictedAttributes.isNotEmpty()) {
                        val conflictedNames = conflictedAttributes.map { it.attribute.attributeName }
                        problems +=
                            manager.createProblemDescriptor(
                                attributeValue,
                                attributeValue.getHighlightRange(),
                                SpringCoreBundle.message(
                                    "esprito.spring.inspection.alias.interchangeable",
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

        for (attribute in annotation.attributes) {
            var aliasedName = attribute.attributeName
            var aliasedClass = annotation.resolveAnnotationType() ?: continue
            var aliasedClassQn = aliasedClass.qualifiedName ?: continue
            var aliasedMethod = aliasedClass
                .methods
                .firstOrNull {
                    it.name == aliasedName
                } ?: continue

            while (true) {
                val aliasAnnotation = AnnotationUtil.findAnnotation(aliasedMethod, ALIAS_FOR)
                if (aliasAnnotation == null) {
                    //Добрались до самой глубокой, больше ни на кого не ссылаемся
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
                    // Ссылаемся на другой метод на том же уровне
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