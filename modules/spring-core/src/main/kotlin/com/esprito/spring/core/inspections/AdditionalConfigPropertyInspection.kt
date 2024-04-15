package com.esprito.spring.core.inspections

import com.esprito.inspection.SpringBaseLocalInspectionTool
import com.esprito.spring.core.SpringCoreBundle
import com.esprito.spring.core.SpringProperties.DEPRECATED
import com.esprito.spring.core.SpringProperties.DESCRIPTION
import com.esprito.spring.core.SpringProperties.HINTS
import com.esprito.spring.core.SpringProperties.NAME
import com.esprito.spring.core.SpringProperties.PARAMETERS
import com.esprito.spring.core.SpringProperties.PROPERTIES
import com.esprito.spring.core.SpringProperties.PROVIDERS
import com.esprito.spring.core.SpringProperties.REASON
import com.esprito.spring.core.SpringProperties.TARGET
import com.esprito.spring.core.SpringProperties.VALUE
import com.esprito.spring.core.SpringProperties.VALUES
import com.esprito.spring.core.completion.properties.SpringConfigurationPropertiesSearch
import com.esprito.spring.core.inspections.quickfix.AddJsonElementQuickFix
import com.esprito.spring.core.properties.providers.ConfigurationPropertyKeyReference
import com.esprito.spring.core.properties.providers.SpringBootValueProvider
import com.esprito.spring.core.properties.references.AdditionalConfigValueProviderReference
import com.esprito.spring.core.util.PropertyUtil
import com.esprito.spring.core.util.SpringCoreUtil
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

class AdditionalConfigPropertyInspection : SpringBaseLocalInspectionTool() {

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
                        SpringCoreBundle.message("esprito.spring.inspection.metadata.config.end.with.dot"),
                        ProblemHighlightType.WEAK_WARNING,
                    )
                }
            }
        }

        private fun getProblemDeprecated(propertyName: String, jsonProperty: JsonProperty) {
            if (propertyName == DEPRECATED) {
                problems.registerProblem(
                    jsonProperty,
                    SpringCoreBundle.message("esprito.spring.inspection.metadata.config.deprecated"),
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
                    SpringCoreBundle.message("esprito.spring.inspection.metadata.config.required", NAME),
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

            val message = SpringCoreBundle.message("esprito.spring.inspection.metadata.config.duplicate", nameValue)
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
                        "esprito.spring.inspection.metadata.config.duplicate.method",
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
                            SpringCoreBundle.message("esprito.spring.inspection.metadata.config.required", NAME),
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
                            SpringCoreBundle.message("esprito.spring.inspection.metadata.config.required", VALUE),
                            AddJsonElementQuickFix(provider, listOf(VALUE))
                        )
                    }
                }
            }

        }

        private fun problemPropertyProvider(valueProperty: JsonProperty, provider: JsonObject) {
            var valueProvider: SpringBootValueProvider? = null
            val providerNameValue = valueProperty.value as? JsonStringLiteral ?: return
            for (reference in providerNameValue.references) {
                if (reference is AdditionalConfigValueProviderReference) {
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
                            "esprito.spring.inspection.metadata.config.required.parameter",
                            StringUtil.join(missingParameters, ", ")
                        ),
                        AddJsonElementQuickFix(parameters, missingParameters)
                    )
                }
            }
        }
    }

}