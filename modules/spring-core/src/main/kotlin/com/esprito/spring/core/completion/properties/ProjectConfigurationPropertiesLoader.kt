package com.esprito.spring.core.completion.properties

import com.esprito.module.ExternalSystemModule
import com.esprito.spring.core.SpringCoreClasses
import com.esprito.spring.core.SpringProperties.ADDITIONAL_CONFIGURATION_METADATA_FILE_NAME
import com.esprito.spring.core.service.SpringSearchService
import com.esprito.util.EspritoPsiUtil.isMetaAnnotatedBy
import com.esprito.util.EspritoPsiUtil.resolvedPsiClass
import com.esprito.util.EspritoPsiUtil.returnPsiClass
import com.esprito.util.runReadNonBlocking
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.javadoc.PsiDocToken
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import com.intellij.psi.util.PropertyUtil
import com.intellij.psi.util.childrenOfType
import com.intellij.util.Query
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

    private fun loadPropertiesFromConfiguration(module: Module): HashMap<String, ConfigurationProperty> = runReadNonBlocking {
        val result = hashMapOf<String, ConfigurationProperty>()

        val annotatedElements = getAnnotatedElements(module) ?: return@runReadNonBlocking result

        for (annotatedElement in annotatedElements) {
            val prefix = extractConfigurationPropertyPrefix(module, annotatedElement) ?: continue
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
            collectConfigurationProperties(it.text, it.virtualFile.path, result)
        }
        return result
    }

    private fun getAnnotatedElements(module: Module): Query<out PsiModifierListOwner>? {
        val librariesSearchScope = ExternalSystemModule.of(module).librariesSearchScope
        val configurationPropertiesClass = JavaPsiFacade.getInstance(module.project)
            .findClass(SpringCoreClasses.CONFIGURATION_PROPERTIES, librariesSearchScope)
            ?: return null

        return AnnotatedElementsSearch.searchElements(
            configurationPropertiesClass,
            module.moduleWithDependenciesScope,
            PsiClass::class.java, PsiMethod::class.java
        )
    }

    private fun extractConfigurationPropertyPrefix(module: Module, annotatedElement: PsiModifierListOwner): String? {
        if (annotatedElement !is PsiMember) {
            return null
        }
        val metaHolder = SpringSearchService.getInstance(module.project)
            .getMetaAnnotations(module, SpringCoreClasses.CONFIGURATION_PROPERTIES)
        val prefix = metaHolder.getAnnotationMemberValues(annotatedElement, setOf("value", "prefix")).firstOrNull() ?: return null
        return AnnotationUtil.getStringAttributeValue(prefix)
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
                if (propertyTypeClass != null
                    && ownerConfigurationClass.containsClass(propertyTypeClass)
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
        val setterMethods = getSetterMethods(targetClass, nestedFields)
        val getterMethods = getGetterMethods(targetClass, nestedFields)
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

    private fun getGetterMethods(
        targetClass: PsiClass,
        nestedFields: List<FieldPropertyWrapper>
    ) : List<PsiMethod> {
        return targetClass.allMethods.filter {
            !it.hasModifierProperty(PsiModifier.STATIC)
                    && !it.hasModifierProperty(PsiModifier.PRIVATE)
                    && it.parameterList.parametersCount == 0
                    && (isPrefixedJavaIdentifier(it.name, "get") || isPrefixedJavaIdentifier(it.name, "is"))
                    && it.returnType !in nestedFields.map { field -> field.psiType }
        }.filterNotNull()
    }

    private fun getSetterMethods(
        targetClass: PsiClass,
        nestedFields: List<FieldPropertyWrapper>
    ) : List<PsiMethod> {
        return targetClass.allMethods.filter {
            !it.hasModifierProperty(PsiModifier.STATIC)
                    && !it.hasModifierProperty(PsiModifier.PRIVATE)
                    && it.parameterList.parametersCount == 1
                    && isPrefixedJavaIdentifier(it.name, "set")
                    && it.parameterList.parameters[0].type !in nestedFields.map { field -> field.psiType }
                    && it.returnPsiClass == null
        }.filterNotNull()
    }

    private fun getNestedPropertyWrappers(targetClass: PsiClass): List<FieldPropertyWrapper> {
        return targetClass.allFields
            .filter { it.isMetaAnnotatedBy(SpringCoreClasses.NESTED_CONFIGURATION_PROPERTIES) }
            .map { FieldPropertyWrapper(it) }
    }

    private fun getFieldPropertyWrappers(targetClass: PsiClass): List<FieldPropertyWrapper> {
        return targetClass.allFields.filter {
            !it.hasModifierProperty(PsiModifier.STATIC)
                    && it.hasModifierProperty(PsiModifier.FINAL)
                    && !it.isMetaAnnotatedBy(SpringCoreClasses.NESTED_CONFIGURATION_PROPERTIES)
        }.map { FieldPropertyWrapper(it) }
    }

    private fun isPrefixedJavaIdentifier(name: String, prefix: String): Boolean {
        return name.startsWith(prefix) && name.length > prefix.length
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
            return null
        }
        val expressions = codeBlock[0].childrenOfType<PsiExpressionStatement>()
        val assignmentExpressions = expressions.asSequence()
            .map { it.childrenOfType<PsiAssignmentExpression>() }
            .filter { it.isNotEmpty() }
            .toList()
        for(assignmentExpression in assignmentExpressions) {
            for(it in assignmentExpression) {
                if (it.lastChild.text == psiMethod.parameterList.parameters[0].lastChild.text) {
                    val referenceExpression = it.firstChild
                    if (referenceExpression is PsiReferenceExpression) {
                        val identifier = referenceExpression.childrenOfType<PsiIdentifier>().firstOrNull()
                        if (identifier != null) {
                            return identifier
                        }
                    }
                }
            }
        }
        return null
    }

    private fun getIdentifierFromGetterMethod(psiMethod: PsiMethod): PsiIdentifier? {
        val codeBlock = psiMethod.childrenOfType<PsiCodeBlock>()
        if (codeBlock.isEmpty()) {
            return null
        }
        val expressions = codeBlock.flatMap { it.childrenOfType<PsiReturnStatement>() }

        return expressions.last()
            .childrenOfType<PsiReferenceExpression>().last()
            .childrenOfType<PsiIdentifier>().last()
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

private fun PsiClass.containsClass(nestedClass: PsiClass): Boolean {
    return innerClasses.any { it == nestedClass || it.containsClass(nestedClass) }
}

private abstract class PropertyWrapper<T : PsiMember>(val psiMember: T) {
    open val name: String?
        get() {
            val propertyName = PropertyUtil.getPropertyName(psiMember) ?: return null
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

private class FieldPropertyWrapper(psiField: PsiField, deprecationInfo: DeprecationInfo? = null) : PropertyWrapper<PsiField>(psiField) {

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
