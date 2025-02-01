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

package com.explyt.spring.core.completion

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.SpringIcons
import com.explyt.spring.core.SpringProperties.SPRING_JPA_PROPERTIES
import com.explyt.spring.core.completion.insertHandler.YamlKeyConfigurationPropertyInsertHandler
import com.explyt.spring.core.completion.properties.ConfigurationProperty
import com.explyt.spring.core.completion.properties.DeprecationInfo
import com.explyt.spring.core.completion.properties.DeprecationInfoLevel
import com.explyt.spring.core.completion.properties.YamlPropertySource
import com.explyt.spring.core.completion.renderer.JpaRendererPropertyRenderer
import com.explyt.spring.core.util.SpringCoreUtil
import com.explyt.util.ExplytPsiUtil.isFinal
import com.explyt.util.ExplytPsiUtil.isStatic
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.completion.CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.lang.properties.IProperty
import com.intellij.lang.properties.PropertiesLanguage
import com.intellij.lang.properties.psi.PropertiesFile
import com.intellij.lang.properties.psi.impl.PropertyImpl
import com.intellij.lang.properties.psi.impl.PropertyKeyImpl
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.ProcessingContext
import org.jetbrains.yaml.YAMLLanguage
import org.jetbrains.yaml.YAMLUtil
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YAMLPsiElement
import org.jetbrains.yaml.psi.YAMLScalar


class JpaPropertiesCompletionContributor : CompletionContributor() {
    init {
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement().withLanguage(YAMLLanguage.INSTANCE),
            JpaPropertiesCompletionProvider()
        )
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement(PropertyKeyImpl::class.java).withLanguage(PropertiesLanguage.INSTANCE),
            JpaPropertiesCompletionProvider()
        )


    }

    class JpaPropertiesCompletionProvider : CompletionProvider<CompletionParameters>() {
        override fun addCompletions(
            parameters: CompletionParameters,
            context: ProcessingContext,
            result: CompletionResultSet
        ) {
            if (!SpringCoreUtil.isConfigurationPropertyFile(parameters.originalFile)) return

            val propertyPosition = parameters.position
            val psiElement = propertyPosition.parent ?: return

            if (!isSpringJpaProperties(psiElement)) return

            getHibernateKeys(parameters, psiElement, result)
            result.stopHere()
        }

        private fun isSpringJpaProperties(psiElement: PsiElement): Boolean {
            return when (psiElement) {
                is YAMLPsiElement -> {
                    val result = YAMLUtil.getConfigFullName(psiElement).startsWith(SPRING_JPA_PROPERTIES)
                    if (psiElement is YAMLScalar) {
                        val scalarText = psiElement.parent.parent.text
                            .substringBefore(DUMMY_IDENTIFIER_TRIMMED)
                            .substringAfterLast("\n")
                            .trim()
                        if (scalarText.contains(":")) return false
                    }
                    result
                }

                is PropertyImpl -> {
                    psiElement.text.startsWith(SPRING_JPA_PROPERTIES)
                }

                else -> false
            }
        }

        private fun getHibernateKeys(
            parameters: CompletionParameters,
            psiElement: PsiElement,
            result: CompletionResultSet
        ) {
            val facade = JavaPsiFacade.getInstance(parameters.position.project)
            val hibernateSettings = facade.findClass(
                SpringCoreClasses.HIBERNATE_CFG_AVAILABLE_SETTING,
                GlobalSearchScope.allScope(parameters.position.project)
            ) ?: return

            val keys = getExistingKeys(psiElement)
            val startKey = if (psiElement is YAMLPsiElement) {
                YAMLUtil.getConfigFullName(psiElement).substringBefore(DUMMY_IDENTIFIER_TRIMMED)
                    .substringAfter("$SPRING_JPA_PROPERTIES.", "")
            } else if (psiElement is IProperty) {
                psiElement.key?.substringBeforeLast(".") ?: ""
            } else ""

            hibernateSettings.fields.asSequence().forEach { field ->
                if (field.isStatic && field.isFinal && field.type.equalsToText("java.lang.String")) {
                    val value = (field.initializer as? PsiLiteralExpression)?.value
                    if (value != null) {
                        val name =
                            if (psiElement.language is PropertiesLanguage) "$SPRING_JPA_PROPERTIES.$value"
                            else {
                                value.toString()
                            }
                        if (isViewProperty(psiElement, keys, startKey, name)) {
                            val property = jpaConfigurationProperty(value.toString(), field.isDeprecated)

                            val propertyName =
                                if (startKey.isNotEmpty() && psiElement is YAMLPsiElement) name.substringAfter("$startKey.") else name
                            result.addElement(
                                LookupElementBuilder.create(property, propertyName)
                                    .withInsertHandler(YamlKeyConfigurationPropertyInsertHandler())
                                    .withRenderer(JpaRendererPropertyRenderer("Hibernate", SpringIcons.Hibernate))
                            )
                        }
                    }
                }
            }
        }

        private fun isViewProperty(element: PsiElement, keys: Set<String>, startKey: String, name: String): Boolean {
            val propertyName = if (element is YAMLPsiElement) {
                "$SPRING_JPA_PROPERTIES.$name"
            } else name
            return propertyName !in keys && (startKey.isEmpty() || name.startsWith(startKey))
        }

        private fun jpaConfigurationProperty(
            name: String,
            deprecated: Boolean
        ): ConfigurationProperty {
            return ConfigurationProperty(
                name = "$SPRING_JPA_PROPERTIES.$name",
                propertyType = null,
                type = null,
                sourceType = null,
                description = null,
                defaultValue = null,
                deprecation =
                    if (deprecated)
                        DeprecationInfo(DeprecationInfoLevel.WARNING, null, null)
                    else null
            )
        }

        private fun getExistingKeys(element: PsiElement): Set<String> {
            val psiFile = element.containingFile
            val keys = mutableSetOf<String>()
            when (psiFile) {
                is PropertiesFile -> {
                    keys.addAll(psiFile.properties.mapNotNull { it.name })
                }

                is YAMLFile -> {
                    keys.addAll(YamlPropertySource(psiFile).properties.map { it.key })
                }
            }
            return keys

        }
    }
}