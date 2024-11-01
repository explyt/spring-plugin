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

import com.explyt.inspection.SpringBaseLocalInspectionTool
import com.explyt.spring.core.SpringCoreBundle
import com.explyt.spring.core.SpringProperties.DEPRECATED
import com.explyt.spring.core.SpringProperties.DESCRIPTION
import com.explyt.spring.core.SpringProperties.HINTS
import com.explyt.spring.core.SpringProperties.NAME
import com.explyt.spring.core.SpringProperties.PARAMETERS
import com.explyt.spring.core.SpringProperties.PROPERTIES
import com.explyt.spring.core.SpringProperties.PROVIDERS
import com.explyt.spring.core.SpringProperties.REASON
import com.explyt.spring.core.SpringProperties.TARGET
import com.explyt.spring.core.SpringProperties.VALUE
import com.explyt.spring.core.SpringProperties.VALUES
import com.explyt.spring.core.completion.properties.SpringConfigurationPropertiesSearch
import com.explyt.spring.core.inspections.quickfix.AddJsonElementQuickFix
import com.explyt.spring.core.properties.providers.ConfigurationPropertyKeyReference
import com.explyt.spring.core.properties.providers.SpringMetadataValueProvider
import com.explyt.spring.core.properties.references.SpringMetadataValueProviderReference
import com.explyt.spring.core.util.PropertyUtil
import com.explyt.spring.core.util.SpringCoreUtil
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.json.psi.*
import com.intellij.json.psi.impl.JsonRecursiveElementVisitor
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.util.Conditions
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.*
import com.intellij.psi.impl.source.resolve.reference.impl.providers.JavaClassReference
import com.intellij.util.NotNullProducer
import java.util.function.Predicate

class SpringMetadataPropertyInspection : SpringBaseLocalInspectionTool() {

    override fun checkFile(
        file: PsiFile,
        manager: InspectionManager,
        isOnTheFly: Boolean,
    ): Array<ProblemDescriptor>? {
        if (!SpringCoreUtil.isAdditionalConfigFile(file)) {
            return ProblemDescriptor.EMPTY_ARRAY
        }

        val jsonFile = file as JsonFile
        val topLevelValue = jsonFile.topLevelValue ?: return ProblemDescriptor.EMPTY_ARRAY

        val problems = ProblemsHolder(manager, file, isOnTheFly)
        val visitor = PropertyVisitor(problems)
        topLevelValue.acceptChildren(visitor)
        visitTopLevelValues(
            topLevelValue,
            Conditions.alwaysTrue(),
            visitorProducer = { NameAttributeVisitor(problems) }
        )
        visitTopLevelValues(
            topLevelValue,
            { jsonProperty: JsonProperty -> jsonProperty.name == PROPERTIES },
            visitorProducer = { NamePropertiesVisitor(problems) }
        )
        visitTopLevelValues(
            topLevelValue,
            { jsonProperty: JsonProperty -> jsonProperty.name == HINTS },
            visitorProducer = { HintsVisitor(problems) }
        )

        return problems.resultsArray
    }

    private fun visitTopLevelValues(
        topLevelValue: JsonValue,
        runVisitorCondition: Predicate<JsonProperty>,
        visitorProducer: NotNullProducer<JsonElementVisitor>
    ) {
        val jsonObject = topLevelValue as? JsonObject ?: return
        jsonObject.propertyList.forEach {
            val propertyValue = it.value
            if (propertyValue is JsonArray && runVisitorCondition.test(it)) {
                propertyValue.acceptChildren(visitorProducer.produce())
            }
        }
    }

    private class PropertyVisitor(val problems: ProblemsHolder) : JsonElementVisitor() {
        override fun visitObject(jsonObject: JsonObject) {
            jsonObject.acceptChildren(this)
        }

        override fun visitArray(jsonArray: JsonArray) {
            jsonArray.acceptChildren(this)
        }

        override fun visitProperty(jsonProperty: JsonProperty) {
            val value = jsonProperty.value ?: return
            val propertyName = jsonProperty.name

            getProblemUnresolved(value, propertyName)
            getProblemWithDot(value, propertyName)
            getProblemDeprecated(propertyName, jsonProperty)

            value.accept(this)
        }

        private fun getProblemUnresolved(value: JsonValue, propertyName: String) {
            for (reference in value.references) {
                if (reference.isSoft) continue

                val unresolved =
                    if (reference is PsiPolyVariantReference) reference.multiResolve(false).isEmpty()
                    else reference.resolve() == null
                if (unresolved) {
                    problems.registerProblem(
                        reference,
                        ProblemsHolder.unresolvedReferenceMessage(reference),
                        unresolvedProblemHighlightType(
                            propertyName,
                            reference
                        )
                    )
                }
            }
        }

        private fun getProblemWithDot(value: JsonValue, propertyName: String) {
            if (value is JsonStringLiteral && (propertyName == DESCRIPTION || propertyName == REASON)) {
                val text = value.value
                if (!StringUtil.endsWithChar(text, '.')) {
                    problems.registerProblem(
                        value,
                        SpringCoreBundle.message("explyt.spring.inspection.metadata.config.end.with.dot"),
                        ProblemHighlightType.WEAK_WARNING,
                    )
                }
            }
        }

        private fun getProblemDeprecated(propertyName: String, jsonProperty: JsonProperty) {
            if (propertyName == DEPRECATED) {
                problems.registerProblem(
                    jsonProperty,
                    SpringCoreBundle.message("explyt.spring.inspection.metadata.config.deprecated"),
                    ProblemHighlightType.LIKE_DEPRECATED,
                )
            }
        }

        private fun unresolvedProblemHighlightType(
            propertyName: String,
            reference: PsiReference
        ): ProblemHighlightType {
            return if (reference is ConfigurationPropertyKeyReference)
                ProblemHighlightType.WEAK_WARNING
            else if (reference !is JavaClassReference) {
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING
            } else if (propertyName == TARGET) {
                ProblemHighlightType.WEAK_WARNING
            } else {
                ProblemHighlightType.LIKE_UNKNOWN_SYMBOL
            }
        }

    }

    private class NameAttributeVisitor(private var problems: ProblemsHolder) : JsonElementVisitor() {
        private val existingNames = HashMap<String, JsonValue>()

        override fun visitObject(jsonObject: JsonObject) {
            if (jsonObject.findProperty(NAME) == null) {
                problems.registerProblem(
                    jsonObject,
                    SpringCoreBundle.message("explyt.spring.inspection.metadata.config.required", NAME),
                    AddJsonElementQuickFix(jsonObject, listOf(NAME))
                )
            }
            jsonObject.acceptChildren(this)
        }

        override fun visitProperty(jsonProperty: JsonProperty) {
            if (jsonProperty.name == NAME) {
                val jsonValue = jsonProperty.value ?: return
                val value = jsonValue as? JsonStringLiteral ?: return

                getProblemDuplicate(value)
            }
        }

        private fun getProblemDuplicate(value: JsonStringLiteral) {
            val nameValue = value.value
            if (!existingNames.containsKey(nameValue)) {
                existingNames[nameValue] = value
                return
            }

            val existingNameValue = existingNames[nameValue] ?: return

            val message = SpringCoreBundle.message("explyt.spring.inspection.metadata.config.duplicate", nameValue)
            problems.registerProblem(value, ElementManipulators.getValueTextRange(value), message)
            problems.registerProblem(
                existingNameValue,
                ElementManipulators.getValueTextRange(existingNameValue),
                message
            )
        }
    }

    private class NamePropertiesVisitor(private var problems: ProblemsHolder) : JsonElementVisitor() {
        override fun visitObject(jsonObject: JsonObject) {
            jsonObject.acceptChildren(this)
        }

        override fun visitProperty(jsonProperty: JsonProperty) {
            if (jsonProperty.name == NAME) {
                val jsonValue = jsonProperty.value ?: return
                val value = jsonValue as? JsonStringLiteral ?: return

                getProblemExistInSetMethod(value)
            }
        }

        private fun getProblemExistInSetMethod(value: JsonStringLiteral) {
            val module = ModuleUtilCore.findModuleForPsiElement(value) ?: return
            val project = value.project

            val nameValue = value.value
            val foundProperty = SpringConfigurationPropertiesSearch.getInstance(project)
                .findProperty(module, nameValue) ?: return
            val sourceType = foundProperty.sourceType ?: return
            val sourceMember = PropertyUtil.findSourceMember(nameValue, sourceType, project)

            if (sourceMember != null && sourceMember is PsiMethod) {
                val psiClass = sourceMember.containingClass
                val className =
                    if (psiClass != null && psiClass.name != null) psiClass.name!!
                    else "NONE"
                problems.registerProblem(
                    value,
                    SpringCoreBundle.message(
                        "explyt.spring.inspection.metadata.config.duplicate.method",
                        sourceMember.name,
                        className
                    ),
                    ProblemHighlightType.WEAK_WARNING
                )
            }
        }
    }
    private class HintsVisitor(private var problems: ProblemsHolder) : JsonRecursiveElementVisitor() {
        override fun visitProperty(jsonProperty: JsonProperty) {
            val providersValue = jsonProperty.value ?: return
            val providersArray = providersValue as? JsonArray ?: return
            if (jsonProperty.name == PROVIDERS) {
                for (value in providersArray.valueList) {
                    val provider = value as? JsonObject ?: continue
                    val valueProperty = provider.findProperty(NAME)

                    if (valueProperty == null) {
                        problems.registerProblem(
                            provider,
                            SpringCoreBundle.message("explyt.spring.inspection.metadata.config.required", NAME),
                            AddJsonElementQuickFix(provider, listOf(NAME))
                        )
                    } else {
                        problemPropertyProvider(valueProperty, provider)
                    }
                }
            }

            if (jsonProperty.name == VALUES) {
                for (value in providersArray.valueList) {
                    val provider = value as? JsonObject ?: continue
                    val valueProperty = provider.findProperty(VALUE)
                    if (valueProperty == null) {
                        problems.registerProblem(
                            provider,
                            SpringCoreBundle.message("explyt.spring.inspection.metadata.config.required", VALUE),
                            AddJsonElementQuickFix(provider, listOf(VALUE))
                        )
                    }
                }
            }

        }

        private fun problemPropertyProvider(valueProperty: JsonProperty, provider: JsonObject) {
            var valueProvider: SpringMetadataValueProvider? = null
            val providerNameValue = valueProperty.value as? JsonStringLiteral ?: return
            for (reference in providerNameValue.references) {
                if (reference is SpringMetadataValueProviderReference) {
                    valueProvider = reference.getValueProvider()
                    break
                }
            }

            if (valueProvider != null && valueProvider.isRequiredParameters()) {
                val parameters = provider.findProperty(PARAMETERS) ?: return
                val parametersValue = parameters.value as? JsonObject ?: return

                val missingParameters = mutableListOf<String>()
                valueProvider.parameters.asSequence()
                    .filter { it.required && (parametersValue.findProperty(it.name) == null) }
                    .mapTo(missingParameters) { it.name }

                if (missingParameters.isNotEmpty()) {
                    problems.registerProblem(
                        providerNameValue,
                        ElementManipulators.getValueTextRange(providerNameValue),
                        SpringCoreBundle.message(
                            "explyt.spring.inspection.metadata.config.required.parameter",
                            StringUtil.join(missingParameters, ", ")
                        ),
                        AddJsonElementQuickFix(parameters, missingParameters)
                    )
                }
            }
        }
    }

}