package com.esprito.spring.core.properties.dataRetriever

import ai.grazie.utils.capitalize
import com.esprito.spring.core.SpringCoreClasses
import com.esprito.spring.core.completion.properties.DefinedConfigurationPropertiesSearch
import com.esprito.spring.core.completion.properties.SpringConfigurationPropertiesSearch
import com.esprito.spring.core.service.SpringSearchService
import com.esprito.spring.core.tracker.ModificationTrackerManager
import com.esprito.spring.core.util.PropertyUtil
import com.esprito.util.EspritoPsiUtil.isMetaAnnotatedBy
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.openapi.module.Module
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.apache.logging.log4j.util.Strings
import org.jetbrains.kotlin.j2k.getContainingConstructor
import org.jetbrains.uast.*

abstract class ConfigurationPropertyDataRetriever {

    abstract fun getContainingClass(): PsiClass?


    abstract fun getMemberName(): String?

    abstract fun getNameElementPsi(): PsiElement?

    fun getRelatedProperties(prefix: String, name: String, module: Module): List<PsiElement> {
        val propertyFqn = prefix + name
        return DefinedConfigurationPropertiesSearch.getInstance(module.project)
            .getAllProperties(module).asSequence()
            .filter { PropertyUtil.isSameProperty(it.key, propertyFqn) }
            .mapNotNull { it.psiElement }.toList()
    }

    fun getMetadataName(prefix: String, name: String, module: Module): List<PsiElement> {
        val propertyFqn = prefix + name
        val hints = SpringConfigurationPropertiesSearch.getInstance(module.project)
            .getElementNameHints(module)

        return hints
            .asSequence()
            .filter { PropertyUtil.isSameProperty(it.name, propertyFqn) }
            .mapNotNull { it.jsonProperty.value }
            .toList()
    }

    companion object {

        fun getPrefixValue(psiClass: PsiClass, module: Module): String {
            return CachedValuesManager.getManager(module.project)
                .getCachedValue(psiClass) {
                    CachedValueProvider.Result(
                        getPrefixFromUsage(psiClass, module),
                        ModificationTrackerManager.getInstance(module.project).getUastModelAndLibraryTracker()
                    )
                }
        }

        private fun getPrefixFromUsage(psiClass: PsiClass, module: Module): String {
            if (psiClass.isMetaAnnotatedBy(SpringCoreClasses.CONFIGURATION_PROPERTIES)) {
                return getPrefixFromAnnotation(psiClass, module)
            }
            val searchScope = psiClass.useScope as? GlobalSearchScope ?: return Strings.EMPTY
            val references = ReferencesSearch.search(psiClass, searchScope)
                .mapNotNull { it.element }

            val configurationOnBean = references.asSequence()
                .mapNotNull {
                    listOfNotNull(
                        it.getUastParentOfType<UMethod>(),
                        it.getUastParentOfType<UField>()
                    )
                        .firstOrNull()
                }
                .filter { it.isMetaAnnotatedBy(SpringCoreClasses.CONFIGURATION_PROPERTIES) }
                .filter { it.isMetaAnnotatedBy(SpringCoreClasses.BEAN) }
                .firstOrNull()

            if (configurationOnBean != null) {
                return getPrefixFromAnnotation(configurationOnBean, module)
            }

            val psiElements = references.asSequence()
                .mapNotNull { it.getUastParentOfTypes(arrayOf(UField::class.java, UParameter::class.java)) }
                .mapNotNullTo(mutableSetOf()) { it.javaPsi as? PsiVariable }
            for (element in psiElements) {
                val topClass = when (element) {
                    is PsiParameter -> element.getContainingConstructor()?.containingClass
                    is PsiField -> element.containingClass
                    else -> null
                }
                if (topClass == null) continue
                if (topClass.qualifiedName == psiClass.qualifiedName) continue

                val prefixValue = getPrefixFromUsage(topClass, module)
                if (prefixValue.isNotBlank()) {
                    return PropertyUtil.prefixValue("$prefixValue${element.name}")
                }
            }

            return ""
        }

        private fun getPrefixFromAnnotation(psiMember: PsiMember, module: Module): String {
            val annotationsHolder = SpringSearchService.getInstance(module.project)
                .getMetaAnnotations(module, SpringCoreClasses.CONFIGURATION_PROPERTIES)

            val prefix = annotationsHolder.getAnnotationMemberValues(psiMember, setOf("prefix", "value"))
                .asSequence()
                .mapNotNull { AnnotationUtil.getStringAttributeValue(it) }
                .firstOrNull()
            return PropertyUtil.prefixValue(prefix)
        }

    }

    protected fun toPascalFormat(memberName: String?): String {
        return when {
            memberName == null -> ""
            memberName.startsWith("set") -> memberName.substring(3)
            memberName.startsWith("get") -> memberName.substring(3)
            else -> memberName.capitalize()
        }
    }

}