package com.esprito.spring.core.completion.properties

import com.esprito.module.ExternalSystemModule
import com.esprito.spring.core.SpringCoreClasses
import com.esprito.util.runReadNonBlocking
import com.intellij.openapi.module.Module
import com.intellij.psi.*
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import com.intellij.psi.util.PropertyUtil
import org.jetbrains.uast.UAnnotated
import org.jetbrains.uast.evaluateString
import org.jetbrains.uast.toUElementOfType
import java.util.*

class ProjectConfigurationPropertiesLoader : ConfigurationPropertiesLoader {

    override fun loadProperties(module: Module): List<ConfigurationProperty> = runReadNonBlocking {
        val project = module.project
        val librariesSearchScope = ExternalSystemModule.of(module).librariesSearchScope
        val configurationPropertiesClass = JavaPsiFacade.getInstance(project)
            .findClass(SpringCoreClasses.CONFIGURATION_PROPERTIES, librariesSearchScope)
            ?: return@runReadNonBlocking emptyList()

        val annotatedElements = AnnotatedElementsSearch.searchElements(
            configurationPropertiesClass,
            module.moduleProductionSourceScope,
            PsiClass::class.java, PsiMethod::class.java
        )

        val result = mutableListOf<ConfigurationProperty>()

        for (annotatedElement in annotatedElements) {
            val prefix = extractConfigurationPropertyPrefix(annotatedElement) ?: continue
            val configurationPropertiesType = when (annotatedElement) {
                is PsiClass -> annotatedElement
                is PsiMethod -> (annotatedElement.returnType as? PsiClassType)?.resolve()
                else -> null
            } ?: continue

            collectConfigurationProperty(
                configurationPropertiesType,
                configurationPropertiesType,
                prefix,
                result
            )
        }

        result
    }


    private fun extractConfigurationPropertyPrefix(annotatedElement: PsiModifierListOwner): String? {
        val configurationPropsAnn = annotatedElement.toUElementOfType<UAnnotated>()
            ?.uAnnotations
            ?.find { it.qualifiedName == SpringCoreClasses.CONFIGURATION_PROPERTIES } ?: return null

        return (configurationPropsAnn.findDeclaredAttributeValue("value")
            ?: configurationPropsAnn.findAttributeValue("prefix"))?.evaluateString()
    }

    private fun collectConfigurationProperty(
        ownerConfigurationClass: PsiClass,
        targetClass: PsiClass,
        prefix: String,
        result: MutableList<ConfigurationProperty>
    ) {
        val finalFields = targetClass.allFields.filter {
            !it.hasModifierProperty(PsiModifier.STATIC) && it.hasModifierProperty(PsiModifier.FINAL)
        }.map {
            FieldPropertyWrapper(it)
        }

        val setterMethods = targetClass.allMethods.filter {
            !it.hasModifierProperty(PsiModifier.STATIC)
                    && !it.hasModifierProperty(PsiModifier.PRIVATE)
                    && it.parameterList.parametersCount == 1
                    && isPrefixedJavaIdentifier(it.name, "set")
        }.map {
            MethodPropertyWrapper(it)
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
                        ownerConfigurationClass,
                        propertyTypeClass,
                        "$prefix.$propertyName",
                        result
                    )
                }
            }

            result.add(ConfigurationProperty("$prefix.$propertyName", propertyWrapper.type, null, null))
        }
    }

    private fun isPrefixedJavaIdentifier(name: String, prefix: String): Boolean {
        return name.startsWith(prefix) && name.length > prefix.length
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

    abstract val psiType: PsiType

    private fun isUnderscoreRequired(before: Char, current: Char, after: Char): Boolean {
        return Character.isLowerCase(before) && Character.isUpperCase(current) && Character.isLowerCase(after)
    }

    override fun toString(): String {
        return name ?: ""
    }
}

private class FieldPropertyWrapper(psiField: PsiField) : PropertyWrapper<PsiField>(psiField) {

    override val psiType: PsiType = psiMember.type
}

private class MethodPropertyWrapper(psiMethod: PsiMethod) : PropertyWrapper<PsiMethod>(psiMethod) {

    override val psiType: PsiType = psiMember.parameterList.parameters[0].type
}