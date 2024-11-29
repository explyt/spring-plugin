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

package com.explyt.spring.core.completion.properties

import com.explyt.module.ExternalSystemModule
import com.explyt.spring.core.JavaCoreClasses
import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.SpringProperties.ADDITIONAL_CONFIGURATION_METADATA_FILE_NAME
import com.explyt.spring.core.SpringProperties.HINTS
import com.explyt.spring.core.SpringProperties.PROPERTIES
import com.explyt.spring.core.completion.properties.utils.ProjectConfigurationPropertiesUtil
import com.explyt.spring.core.service.SpringSearchService
import com.explyt.spring.core.util.PropertyUtil
import com.explyt.spring.core.util.SpringCoreUtil.isMimeTypeClass
import com.explyt.util.ExplytPsiUtil.isFinal
import com.explyt.util.ExplytPsiUtil.isMap
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.explyt.util.ExplytPsiUtil.isNonStatic
import com.explyt.util.ExplytPsiUtil.resolvedPsiClass
import com.explyt.util.ExplytPsiUtil.returnPsiClass
import com.explyt.util.ExplytPsiUtil.returnPsiType
import com.explyt.util.runReadNonBlocking
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.json.psi.JsonFile
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.javadoc.PsiDocToken
import com.intellij.psi.util.childrenOfType
import java.util.*


class ProjectConfigurationPropertiesLoader(project: Project) : AbstractSpringMetadataConfigurationPropertiesLoader(project) {

    override fun loadProperties(module: Module): List<ConfigurationProperty> = runReadNonBlocking {
        val projectProperties = loadPropertiesFromConfiguration(module)
        val properties = mutableListOf<ConfigurationProperty>()
        properties += projectProperties.asSequence().map { it.value }
        properties += loadPropertiesFromMetadata(module).asSequence()
            .filter { it.key !in projectProperties.keys }
            .map { it.value }
            .toList()

        return@runReadNonBlocking properties.toList()
    }

    override fun loadPropertyHints(module: Module): List<PropertyHint> {
        return findMetadataFiles(module).flatMap {
            collectPropertyHints(it.text, it.virtualFile.path)
        }
    }

    override fun loadPropertyMetadataElements(module: Module): List<ElementHint> {
        return findMetadataFiles(module)
            .filterIsInstance<JsonFile>()
            .flatMap { collectElementMetadataName(it, PROPERTIES) }
    }

    override fun loadMetadataElements(module: Module): List<ElementHint> {
        return findMetadataFiles(module)
            .filterIsInstance<JsonFile>()
            .flatMap { collectElementMetadataName(it, HINTS) }
    }

    private fun loadPropertiesFromConfiguration(module: Module): HashMap<String, ConfigurationProperty> = runReadNonBlocking {
        val result = hashMapOf<String, ConfigurationProperty>()

        val annotatedElements = ProjectConfigurationPropertiesUtil.getAnnotatedElements(module)

        for (annotatedElement in annotatedElements) {
            val prefix = ProjectConfigurationPropertiesUtil
                .extractConfigurationPropertyPrefix(module, annotatedElement) ?: continue
            val configurationPropertiesType = when (annotatedElement) {
                is PsiClass -> annotatedElement
                is PsiMethod -> annotatedElement.returnPsiClass
                else -> null
            } ?: continue

            collectConfigurationProperty(
                module,
                configurationPropertiesType,
                configurationPropertiesType,
                prefix,
                result
            )
        }

        result
    }

    private fun loadPropertiesFromMetadata(module: Module): HashMap<String, ConfigurationProperty> {
        val result = hashMapOf<String, ConfigurationProperty>()
        findMetadataFiles(module).forEach {
            collectConfigurationProperties(module.project, it.text, it.virtualFile.path, result)
        }
        return result
    }

    private fun collectConfigurationProperty(
        module: Module,
        ownerConfigurationClass: PsiClass,
        targetClass: PsiClass,
        prefix: String,
        result: MutableMap<String, ConfigurationProperty>
    ) {
        val nestedFields = getNestedPropertyWrappers(targetClass)
        val finalFields = getFieldPropertyWrappers(targetClass)
        val setterMethods = getMethodPropertyWrappers(module, targetClass, nestedFields)

        for (it in nestedFields) {
            val propertyTypeClass = it.psiType.resolvedPsiClass
            if (propertyTypeClass != null) {
                collectConfigurationProperty(
                    module,
                    ownerConfigurationClass,
                    propertyTypeClass,
                    "$prefix.${it.name}",
                    result
                )
            }
        }

        val propertyWrappers = finalFields + setterMethods
        for (propertyWrapper in propertyWrappers) {
            val propertyName = propertyWrapper.name
            val psiType = propertyWrapper.psiType

            if (psiType is PsiClassType) {
                val propertyTypeClass = psiType.resolve()
                val javaFile = propertyTypeClass?.containingFile as? PsiJavaFile ?: continue

                if (javaFile.packageName != JavaCoreClasses.PACKAGE_JAVA_LANG &&
                    javaFile.packageName != JavaCoreClasses.PACKAGE_KOTLIN
                ) {
                    collectConfigurationProperty(
                        module,
                        ownerConfigurationClass,
                        propertyTypeClass,
                        "$prefix.$propertyName",
                        result
                    )
                }
            }

            val name = "$prefix.$propertyName"
            result[name] = ConfigurationProperty(
                name,
                ConfigurationPropertiesLoader.getPropertyType(propertyWrapper.psiType),
                propertyWrapper.type,
                propertyWrapper.sourceType,
                propertyWrapper.description,
                propertyWrapper.default,
                propertyWrapper.deprecation
            )
        }
    }

    private fun getMethodPropertyWrappers(
        module: Module,
        targetClass: PsiClass,
        nestedFields: List<FieldPropertyWrapper>
    ): List<MethodPropertyWrapper> {
        val result = mutableListOf<MethodPropertyWrapper>()
        val setterMethods = PropertyUtil.getSetterMethods(targetClass, nestedFields)
        val getterMethods = PropertyUtil.getGetterMethods(targetClass, nestedFields)
        for (method in setterMethods) {
            val setterIdentifier = getIdentifierFromSetterMethod(method) ?: continue
            var methodPropertyWrapper: MethodPropertyWrapper? = null
            for (it in getterMethods) {
                val getterIdentifier = getIdentifierFromGetterMethod(it)
                if (getterIdentifier!=null && getterIdentifier.text == setterIdentifier.text) {
                    val deprecation = getDeprecationInfo(module, it)
                    methodPropertyWrapper = MethodPropertyWrapper(method, deprecation)
                }
            }
            if (methodPropertyWrapper == null) {
                methodPropertyWrapper = MethodPropertyWrapper(method, null)
            }
            result += methodPropertyWrapper
        }
        return result
    }

    private fun getNestedPropertyWrappers(targetClass: PsiClass): List<FieldPropertyWrapper> {
        return targetClass.allFields
            .filter { it.isMetaAnnotatedBy(SpringCoreClasses.NESTED_CONFIGURATION_PROPERTIES) }
            .map { FieldPropertyWrapper(it) }
    }

    private fun getFieldPropertyWrappers(targetClass: PsiClass): List<FieldPropertyWrapper> {
        val fields = if (targetClass.isEnum) {
            emptyList()
        } else if (targetClass.isMimeTypeClass()) {
            targetClass.allFields.filter { it.returnPsiType?.isMap == true }
        } else {
            targetClass.allFields.filter {
                it.isNonStatic
                        && it.isFinal
                        && !it.isMetaAnnotatedBy(SpringCoreClasses.NESTED_CONFIGURATION_PROPERTIES)
            }
        }
        return fields.map { FieldPropertyWrapper(it) }
    }

    private fun findMetadataFiles(module: Module): List<PsiFile> {
        val sourceMeta = ExternalSystemModule.of(module).sourceMetaInfDirectory ?: return emptyList()
        return sourceMeta.files.asSequence()
            .filterNotNull()
            .filter { ADDITIONAL_CONFIGURATION_METADATA_FILE_NAME.equals(it.name, true) }.toList()
    }

    private fun getIdentifierFromSetterMethod(psiMethod: PsiMethod): PsiIdentifier? {
        val codeBlock = psiMethod.childrenOfType<PsiCodeBlock>()
        if (codeBlock.isEmpty()) {
            return psiMethod.identifyingElement as? PsiIdentifier
        }
        val expressions = codeBlock[0].childrenOfType<PsiExpressionStatement>()
        val assignmentExpressions = expressions.asSequence()
            .map { it.childrenOfType<PsiAssignmentExpression>() }
            .filter { it.isNotEmpty() }
            .toList()
        return assignmentExpressions
            .asSequence()
            .flatMap { it.asSequence() }
            .filter { it.lastChild != null }
            .filter {
                it.lastChild.text == psiMethod.parameterList.parameters[0].lastChild?.text
                        || it.lastChild.text == psiMethod.parameterList.parameters[0].name
            }
            .map { it.firstChild }
            .filterIsInstance<PsiReferenceExpression>()
            .map { it.childrenOfType<PsiIdentifier>().firstOrNull() }
            .firstOrNull { it != null }
    }

    private fun getIdentifierFromGetterMethod(psiMethod: PsiMethod): PsiIdentifier? {
        val codeBlock = psiMethod.childrenOfType<PsiCodeBlock>()
        if (codeBlock.isEmpty()) {
            return psiMethod.identifyingElement as? PsiIdentifier
        }
        val expressions = codeBlock.flatMap { it.childrenOfType<PsiReturnStatement>() }

        return expressions.lastOrNull()
            ?.childrenOfType<PsiReferenceExpression>()?.lastOrNull()
            ?.childrenOfType<PsiIdentifier>()?.lastOrNull()
    }

    private fun getDeprecationInfo(module: Module, method: PsiMethod?): DeprecationInfo? {
        if (method == null || !method.isDeprecated) {
            return null
        }
        val metaHolder = SpringSearchService.getInstance(module.project)
            .getMetaAnnotations(module, SpringCoreClasses.DEPRECATED_CONFIGURATION_PROPERTIES)
        val reasonAnnotation = metaHolder.getAnnotationMemberValues(method, setOf("reason")).firstOrNull()
        val replacementAnnotation = metaHolder.getAnnotationMemberValues(method, setOf("replacement")).firstOrNull()
        val reason = if (reasonAnnotation != null) AnnotationUtil.getStringAttributeValue(reasonAnnotation) else null
        val replacement =
            if (replacementAnnotation != null) AnnotationUtil.getStringAttributeValue(replacementAnnotation) else null
        return DeprecationInfo(DeprecationInfoLevel.WARNING, replacement, reason)
    }
}

abstract class PropertyWrapper<T : PsiMember>(val psiMember: T) {
    open val name: String?
        get() {
            val propertyName = com.intellij.psi.util.PropertyUtil.getPropertyName(psiMember) ?: return null
            val builder = StringBuilder(propertyName)

            var i = 1
            while (i < builder.length - 1) {
                if (isUnderscoreRequired(builder[i - 1], builder[i], builder[i + 1])) {
                    builder.insert(i++, '-')
                }
                i++
            }

            return builder.toString().lowercase(Locale.getDefault())
        }

    open val type: String
        get() {
            return psiType.canonicalText
        }

    open val sourceType: String?
        get() {
            val containingClass = psiMember.containingClass ?: return null
            return "${containingClass.qualifiedName}#${psiMember.name}"
        }

    open val description: String?
        get() {
            val docToken = (psiMember as? PsiJavaDocumentedElement)?.docComment?.childrenOfType<PsiDocToken>() ?: return null
            return docToken.asSequence()
                .filter { it.tokenType == JavaDocTokenType.DOC_COMMENT_DATA }
                .map { it.text }.firstOrNull()
        }

    abstract val psiType: PsiType

    abstract val default: Any?

    abstract val deprecation:  DeprecationInfo?

    private fun isUnderscoreRequired(before: Char, current: Char, after: Char): Boolean {
        return Character.isLowerCase(before) && Character.isUpperCase(current) && Character.isLowerCase(after)
    }

    override fun toString(): String {
        return name ?: ""
    }
}

class FieldPropertyWrapper(psiField: PsiField, deprecationInfo: DeprecationInfo? = null) :
    PropertyWrapper<PsiField>(psiField) {

    override val psiType: PsiType = psiMember.type

    override val default: Any?
        get() {
            val expression = psiMember.childrenOfType<PsiLiteralExpression>()
            if (expression.isEmpty()) {
                return null
            }
            return expression[0].text
        }

    override val deprecation: DeprecationInfo? = deprecationInfo
}

private class MethodPropertyWrapper(psiMethod: PsiMethod, deprecationInfo: DeprecationInfo? = null) : PropertyWrapper<PsiMethod>(psiMethod) {

    override val psiType: PsiType = psiMember.parameterList.parameters[0].type

    override val default: Any?
        get() = null

    override val deprecation: DeprecationInfo? = deprecationInfo
}

data class DeprecationInfo (
    val level: DeprecationInfoLevel?,
    val replacement: String? = null,
    val reason: String? = null,
)

enum class DeprecationInfoLevel(val value: String) {
    WARNING("warning"),
    ERROR("error"),
    HIDDEN("hidden")
}
