package com.esprito.spring.core.properties.references

import com.esprito.spring.core.JavaCoreClasses
import com.esprito.spring.core.SpringCoreClasses
import com.esprito.spring.core.SpringProperties
import com.esprito.spring.core.SpringProperties.VALUES
import com.esprito.spring.core.completion.properties.PropertyHint
import com.esprito.spring.core.completion.properties.PropertyValueRenderer
import com.esprito.spring.core.completion.properties.ProviderHint
import com.esprito.spring.core.completion.properties.SpringConfigurationPropertiesSearch
import com.esprito.spring.core.properties.ClassReferencePropertyRenderer
import com.esprito.spring.core.service.SpringSearchService
import com.esprito.spring.core.util.PropertyUtil
import com.esprito.spring.core.util.PropertyUtil.propertyKey
import com.esprito.spring.core.util.PropertyUtil.propertyValue
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ClassInheritorsSearch
import com.intellij.xml.util.documentation.MimeTypeDictionary
import java.nio.charset.Charset
import java.text.DateFormat

class ValueHintReference(
    element: PsiElement,
    textRange: TextRange
) : PsiReferenceBase.Poly<PsiElement>(element, textRange, false) {

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        val propertyKey = element.propertyKey() ?: return emptyArray()
        val propertyValue = element.propertyValue() ?: return emptyArray()
        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return emptyArray()

        val propertyHint = PropertyUtil.getPropertyHint(module, propertyKey)
        val resolveResults = propertyHint?.providers?.flatMap {
            when (it.name) {
                SpringProperties.CLASS_REFERENCE -> getClassReference(module, propertyValue)
                SpringProperties.SPRING_BEAN_REFERENCE -> getSpringBeanReference(module, propertyValue)
                else -> emptyList()
            }
        } ?: emptyList()

        if (resolveResults.isNotEmpty()) {
            return resolveResults.map { PsiElementResolveResult(it) }.toTypedArray()
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

    private fun getSpringBeanReference(module: Module, propertyValue: String): List<PsiElement> {
        val springSearchService = SpringSearchService.getInstance(element.project)
        return springSearchService.findActiveBeanDeclarations(module, propertyValue)
    }

    private fun getClassReference(module: Module, propertyValue: String): List<PsiElement> {
        val project = module.project
        val findClass = JavaPsiFacade.getInstance(project)
            .findClass(propertyValue, GlobalSearchScope.allScope(project)) ?: return emptyList()
        return listOf(findClass)
    }

    override fun getVariants(): Array<Any> {
        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return emptyArray()
        val propertyKey = element.propertyKey() ?: return emptyArray()
        val propertyHint = PropertyUtil.getPropertyHint(module, propertyKey)

        if (isMapWithoutValue(propertyKey, propertyHint)) {
            return emptyArray()
        }

        val result = mutableListOf<Any>()
        if (propertyHint != null) {
            result.addAll(getVariantsByHint(propertyHint))
        }

        val configurationProperty =
            SpringConfigurationPropertiesSearch.getInstance(module.project).findProperty(module, propertyKey)
        configurationProperty?.type?.replace('$', '.')?.let {
            result.addAll(getVariantsByPropertyType(it))
        }

        return result.toTypedArray()
    }

    private fun getVariantsByHint(
        propertyHint: PropertyHint
    ): List<Any> {
        val result = mutableListOf<Any>()
        result.addAll(propertyHint.values
            .filter { it.value != null }
            .map { LookupElementBuilder.create(it, it.value!!).withRenderer(PropertyValueRenderer()) })

        propertyHint.providers.flatMapTo(result) { providerHint ->
            processProviderHints(providerHint)
        }
        return result
    }

    private fun getVariantsByPropertyType(propertyType: String): List<Any> {
        return when (propertyType) {
            JavaCoreClasses.LOCALE -> {
                DateFormat.getAvailableLocales().map {
                    val country = it.country
                    it.language + if (country.isNullOrBlank()) "" else "_$country"
                }.distinct().map {
                    LookupElementBuilder.create(it)
                }
            }

            JavaCoreClasses.CHARSET -> {
                Charset.availableCharsets().map {
                    LookupElementBuilder.create(it.key)
                }
            }

            SpringCoreClasses.MIME_TYPE -> {
                MimeTypeDictionary.HTML_CONTENT_TYPES.map {
                    LookupElementBuilder.create(it)
                }
            }

            JavaCoreClasses.BOOLEAN, "boolean" -> {
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
        val targetClassFqn = provider.parameters?.target ?: return emptyList()
        when (provider.name) {
            SpringProperties.CLASS_REFERENCE -> {
                return getClassReferences(targetClassFqn)
            }

            SpringProperties.HANDLE_AS -> {
                return getVariantsByPropertyType(targetClassFqn)
            }

            SpringProperties.SPRING_BEAN_REFERENCE -> {
                return getBeanReferences(targetClassFqn)
            }
        }

        return emptyList()
    }

    private fun getClassReferences(targetClassFqn: String): List<Any> {
        val resolveScope = element.resolveScope
        val targetPsiClass =
            JavaPsiFacade.getInstance(element.project).findClass(targetClassFqn, resolveScope) ?: return emptyList()
        return ClassInheritorsSearch.search(targetPsiClass, resolveScope, true).asSequence()
            .map { psiClass ->
                psiClass?.qualifiedName?.let {
                    LookupElementBuilder.create(psiClass, it)
                        .withRenderer(ClassReferencePropertyRenderer())

                }
            }.filterNotNull().toList()
    }

    private fun getBeanReferences(targetClassFqn: String): List<Any> {
        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return emptyList()
        val springSearchService = SpringSearchService.getInstance(module.project)
        val foundActiveBeans = springSearchService.getActiveBeansClasses(module)
        return foundActiveBeans.asSequence()
            .filter { targetClassFqn.isBlank() || it.psiClass.qualifiedName == targetClassFqn }
            .map {
                LookupElementBuilder.create(it.name)
                    .withIcon(AllIcons.Nodes.Class)
                    .withTailText(" (${it.psiMember.containingFile?.name})")
                    .withTypeText(it.psiClass.name)
            }.toList()
    }

    private fun isMapWithoutValue(propertyKey: String, propertyHint: PropertyHint?): Boolean {
        if (propertyHint == null) {
            return false
        }
        if (propertyHint.name.substringAfterLast(".") == VALUES
            && propertyHint.name.substringBeforeLast(".") == propertyKey
        ) {
            return true
        }
        return false
    }
}