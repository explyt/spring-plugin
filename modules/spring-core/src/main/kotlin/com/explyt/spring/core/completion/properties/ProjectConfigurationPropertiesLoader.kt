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
import com.explyt.spring.core.SpringProperties.ADDITIONAL_CONFIGURATION_METADATA_FILE_NAME
import com.explyt.spring.core.SpringProperties.HINTS
import com.explyt.spring.core.SpringProperties.PROPERTIES
import com.explyt.spring.core.completion.properties.utils.ProjectConfigurationPropertiesUtil
import com.explyt.spring.core.util.PropertyUtil
import com.explyt.spring.core.util.RenameUtil
import com.explyt.util.ExplytPsiUtil.returnPsiClass
import com.explyt.util.runReadNonBlocking
import com.intellij.json.psi.JsonFile
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.javadoc.PsiDocToken
import com.intellij.psi.util.childrenOfType


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

    override fun findMetadataValueElement(module: Module, propertyName: String, propertyValue: String): ElementHint? {
        return findMetadataFiles(module)
            .filterIsInstance<JsonFile>()
            .map { collectElementMetadataHintsValue(it, propertyName, propertyValue) }
            .firstOrNull()
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

            PropertyUtil.collectConfigurationProperty(
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


    private fun findMetadataFiles(module: Module): List<PsiFile> {
        val sourceMeta = ExternalSystemModule.of(module).sourceMetaInfDirectory ?: return emptyList()
        return sourceMeta.files.asSequence()
            .filterNotNull()
            .filter { ADDITIONAL_CONFIGURATION_METADATA_FILE_NAME.equals(it.name, true) }.toList()
    }
}

abstract class PropertyWrapper<T : PsiMember>(val psiMember: T) {
    open val name: String?
        get() {
            val propertyName = com.intellij.psi.util.PropertyUtil.getPropertyName(psiMember) ?: return null
            return RenameUtil.convertSetterToPKebabCase(propertyName)
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
            val comment = getComment(psiMember)
            if (comment != null) return comment

            val filedName = psiMember.name?.substringAfter("set")?.replaceFirstChar { it.lowercase() } ?: return null
            val containingClass = (psiMember as? PsiMethod)?.containingClass ?: return null
            val psiField = containingClass.findFieldByName(filedName, false) ?: return null
            return getComment(psiField)
        }

    abstract val psiType: PsiType

    abstract val default: Any?

    abstract val deprecation:  DeprecationInfo?

    override fun toString(): String {
        return name ?: ""
    }

    private fun getComment(member: PsiMember): String? {
        val docToken = (member as? PsiJavaDocumentedElement)?.docComment?.childrenOfType<PsiDocToken>() ?: return null
        return docToken.asSequence()
            .filter { it.tokenType == JavaDocTokenType.DOC_COMMENT_DATA }
            .map { it.text }.firstOrNull()
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

class MethodPropertyWrapper(psiMethod: PsiMethod, deprecationInfo: DeprecationInfo? = null) :
    PropertyWrapper<PsiMethod>(psiMethod) {

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
