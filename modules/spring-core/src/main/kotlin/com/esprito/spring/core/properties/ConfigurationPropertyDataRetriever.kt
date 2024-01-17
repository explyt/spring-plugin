package com.esprito.spring.core.properties

import ai.grazie.utils.capitalize
import com.esprito.spring.core.SpringCoreClasses
import com.esprito.spring.core.completion.properties.DefinedConfigurationPropertiesSearch
import com.esprito.spring.core.service.SpringSearchService
import com.esprito.spring.core.tracker.ModificationTrackerManager
import com.esprito.spring.core.util.PropertyUtil
import com.esprito.util.EspritoPsiUtil.isMetaAnnotatedBy
import com.esprito.util.EspritoPsiUtil.isNonAbstract
import com.esprito.util.EspritoPsiUtil.isSetter
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.openapi.module.Module
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.parentOfType
import org.apache.logging.log4j.util.Strings

class ConfigurationPropertyDataRetriever(val psiMethod: PsiMethod) {

    fun getContainingClass(): PsiClass? {
        val psiClass = psiMethod.containingClass

        return if (psiClass != null && !psiClass.isInterface && psiClass.isNonAbstract) {
            psiClass
        } else {
            null
        }
    }

    fun getMemberName(): String? {
        return if (isSetter(psiMethod)) {
            toPascalFormat(psiMethod.name)
        } else {
            return null
        }
    }

    fun getRelatedProperties(prefix: String, name: String, module: Module): List<PsiElement> {
        return DefinedConfigurationPropertiesSearch.getInstance(module.project)
            .getAllProperties(module).asSequence()
            .filter { it.key.startsWith(prefix) }
            .filter { propertyNameToPascalFormat(it.key, prefix) == name }
            .mapNotNull { it.psiElement }.toList()
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
                .mapNotNull { it.parentOfType<PsiMethod>() }
                .filter { it.isMetaAnnotatedBy(SpringCoreClasses.CONFIGURATION_PROPERTIES) }
                .filter { it.isMetaAnnotatedBy(SpringCoreClasses.BEAN) }
                .firstOrNull()
            if (configurationOnBean != null) {
                return getPrefixFromAnnotation(configurationOnBean, module)
            }

            val psiFields = references.asSequence()
                .mapNotNull { it.parentOfType<PsiTypeElement>() }
                .mapNotNullTo(mutableSetOf()) { it.parentOfType<PsiField>() }
            for (psiField in psiFields) {
                val topClass = psiField.containingClass ?: continue
                val prefixValue = getPrefixFromUsage(topClass, module)
                if (prefixValue.isNotBlank()) {
                    return PropertyUtil.prefixValue("$prefixValue${psiField.name}")
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

    private fun propertyNameToPascalFormat(key: String, prefixValue: String): String {
        val name = key.replace(prefixValue, "")
        return PropertyUtil.propertyNameToPascalCase(name)
    }

    private fun toPascalFormat(memberName: String?): String {
        return when {
            memberName == null -> ""
            memberName.startsWith("set") -> memberName.substring(3)
            memberName.startsWith("get") -> memberName.substring(3)
            else -> memberName.capitalize()
        }
    }

}