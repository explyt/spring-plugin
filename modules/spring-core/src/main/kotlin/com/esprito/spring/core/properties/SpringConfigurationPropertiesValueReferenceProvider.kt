package com.esprito.spring.core.properties

import com.esprito.spring.core.completion.properties.PropertyHint
import com.esprito.spring.core.completion.properties.PropertyValueRenderer
import com.esprito.spring.core.completion.properties.ProviderHint
import com.esprito.spring.core.completion.properties.SpringConfigurationPropertiesSearch
import com.esprito.spring.core.service.SpringSearchService
import com.esprito.spring.core.util.SpringCoreUtil
import com.intellij.codeInsight.completion.JavaPsiClassReferenceElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.lang.properties.psi.impl.PropertyImpl
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ClassInheritorsSearch
import com.intellij.psi.util.parentOfType
import com.intellij.util.ProcessingContext
import com.intellij.xml.util.documentation.MimeTypeDictionary
import java.nio.charset.Charset
import java.text.DateFormat

class SpringConfigurationPropertiesValueReferenceProvider : PsiReferenceProvider() {

    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
        val propertyKey = element.parentOfType<PropertyImpl>()?.key ?: return emptyArray()
        if (!SpringCoreUtil.isConfigurationPropertyFile(element.containingFile)) {
            return emptyArray()
        }
        return arrayOf(ValueHintReference(element, propertyKey))
    }
}

class ValueHintReference(
    element: PsiElement,
    private val propertyKey: String
) : PsiReferenceBase.Poly<PsiElement>(element) {

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        val propertyValue = element.text ?: return emptyArray()
        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return emptyArray()

        val beanReference = getSpringBeanReference(module, propertyValue)
        if (beanReference.isNotEmpty()) {
            return beanReference.map { PsiElementResolveResult(it) }.toTypedArray()
        }

        val javaPsiFacade = JavaPsiFacade.getInstance(module.project)
        val configurationProperty = SpringConfigurationPropertiesSearch.getInstance(module.project)
            .findProperty(module, propertyKey) ?: return emptyArray()
        val propertyType = configurationProperty.type?.replace('$', '.') ?: return emptyArray()
        val propertyTypeClass = javaPsiFacade.findClass(propertyType, GlobalSearchScope.allScope(module.project))
        if (propertyTypeClass?.isEnum == true) {
            val field = propertyTypeClass.findFieldByName(propertyValue, true) ?: return emptyArray()
            return arrayOf(PsiElementResolveResult(field))
        }
        return emptyArray()
    }

    private fun getSpringBeanReference(module: Module, propertyValue: String): Collection<PsiElement> {
        val springSearchService = SpringSearchService.getInstance(element.project)
        return springSearchService.findActiveBeanDeclarations(module, propertyValue)
    }

    override fun getVariants(): Array<Any> {
        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return emptyArray()

        val springConfigurationPropertiesSearch = SpringConfigurationPropertiesSearch.getInstance(module.project)

        val propertyHint = getPropertyHint(springConfigurationPropertiesSearch, module)

        val result = mutableListOf<Any>()
        if (propertyHint != null) {
            result.addAll(getVariantsByHint(propertyHint))
        }

        val configurationProperty = springConfigurationPropertiesSearch.findProperty(module, propertyKey)
        configurationProperty?.type?.replace('$', '.')?.let {
            result.addAll(getVariantsByPropertyType(it))
        }

        return result.toTypedArray()
    }

    private fun getPropertyHint(
        springConfigurationPropertiesSearch: SpringConfigurationPropertiesSearch,
        module: Module
    ): PropertyHint? {
        val propertyHint = springConfigurationPropertiesSearch
            .getAllHints(module).find { hint ->
                val hintName = hint.name
                if (hintName == propertyKey) {
                    return@find true
                }
                val valuesIdx = hintName.lastIndexOf(".values")
                if (valuesIdx == -1) {
                    return@find false
                }
                propertyKey.startsWith(hintName.substring(0, valuesIdx))
            }
        return propertyHint
    }

    private fun getVariantsByHint(
        propertyHint: PropertyHint
    ): List<Any> {
        val result = mutableListOf<Any>()
        result.addAll(propertyHint.values.map {
            LookupElementBuilder.create(it, it.value).withRenderer(PropertyValueRenderer())
        })

        propertyHint.providers.flatMapTo(result) { providerHint ->
            processProviderHints(providerHint)
        }
        return result
    }

    private fun getVariantsByPropertyType(propertyType: String): List<Any> {
        return when (propertyType) {
            "java.util.Locale" -> {
                DateFormat.getAvailableLocales().map {
                    val country = it.country
                    it.language + if (country.isNullOrBlank()) "" else "_$country"
                }.distinct().map {
                    LookupElementBuilder.create(it)
                }
            }

            "java.nio.charset.Charset" -> {
                Charset.availableCharsets().map {
                    LookupElementBuilder.create(it.key)
                }
            }

            "org.springframework.util.MimeType" -> {
                MimeTypeDictionary.HTML_CONTENT_TYPES.map {
                    LookupElementBuilder.create(it)
                }
            }

            "java.lang.Boolean", "boolean" -> {
                listOf("true", "false").map {
                    LookupElementBuilder.create(it)
                }
            }

            else -> {
                val project = element.project
                val propertyTypeClass = JavaPsiFacade.getInstance(project)
                    .findClass(propertyType, GlobalSearchScope.allScope(project))
                if (propertyTypeClass?.isEnum == true) {
                    return propertyTypeClass.fields.map {
                        LookupElementBuilder.create(it)
                    }
                }
                emptyList()
            }
        }
    }

    private fun processProviderHints(provider: ProviderHint): List<Any> {
        val providerName = provider.name
        when (providerName) {
            "class-reference" -> {
                val targetClassFqn = provider.parameters?.target ?: return emptyList()
                val resolveScope = element.resolveScope
                val targetPsiClass = JavaPsiFacade.getInstance(element.project)
                    .findClass(targetClassFqn, resolveScope) ?: return emptyList()
                return ClassInheritorsSearch.search(targetPsiClass, resolveScope, true).map {
                    JavaPsiClassReferenceElement(it)
                }
            }
            "handle-as" -> {
                val targetClassFqn = provider.parameters?.target ?: return emptyList()
                return getVariantsByPropertyType(targetClassFqn)
            }

            "spring-bean-reference" -> {
                return getBeanReferences(provider.parameters?.target)
            }
        }

        return emptyList()
    }

    private fun getBeanReferences(targetClassFqn: String?): List<Any> {
        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return emptyList()
        val springSearchService = SpringSearchService.getInstance(module.project)
        val foundActiveBeans = springSearchService.getActiveBeansClasses(module)
        return foundActiveBeans.asSequence()
            .filter { targetClassFqn.isNullOrBlank() || it.psiClass.qualifiedName == targetClassFqn }
            .map {
                LookupElementBuilder.create(it.name)
                    .withIcon(AllIcons.Nodes.Class)
                    .withTailText(" (${it.psiMember.containingFile?.name})")
                    .withTypeText(it.psiClass.name)
            }.toList()
    }

}