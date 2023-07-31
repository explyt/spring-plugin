package com.esprito.spring.core.properties

import com.esprito.spring.core.completion.properties.SpringConfigurationPropertiesSearch
import com.intellij.lang.properties.psi.impl.PropertyImpl
import com.intellij.lang.properties.psi.impl.PropertyKeyImpl
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext

class SpringConfigurationPropertiesReferenceContributor : PsiReferenceContributor() {

    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(PlatformPatterns.psiElement(PropertyKeyImpl::class.java), SpringConfigurationPropertiesKeyReferenceProvider())
    }
}

class SpringConfigurationPropertiesKeyReferenceProvider : PsiReferenceProvider() {

    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
        val property = PsiTreeUtil.getParentOfType(
            element,
            PropertyImpl::class.java
        ) ?: return PsiReference.EMPTY_ARRAY

        val propertyKey = property.key ?: return PsiReference.EMPTY_ARRAY
        return arrayOf(ConfigurationPropertyKeyReference(element, propertyKey))
    }
}

open class ConfigurationPropertyKeyReference(element: PsiElement, private val propertyKey: String) : PsiReferenceBase.Poly<PsiElement>(element) {

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return emptyArray()
        val project = element.project
        val foundProperty = SpringConfigurationPropertiesSearch.getInstance(project)
            .findProperty(module, propertyKey) ?: return emptyArray()
        val sourceType = foundProperty.sourceType ?: return emptyArray()
        val sourceMember = findSourceMember(project, sourceType)
        if (sourceMember != null) {
            return PsiElementResolveResult.createResults(sourceMember)
        }
        return emptyArray()
    }

    private fun findSourceMember(project: Project, sourceType: String): PsiMember? {
        val javaPsiFacade = JavaPsiFacade.getInstance(project)
        var memberName = sourceType.substringAfterLast('#', "")
        var setterName = memberName
        if (memberName.isEmpty()) {
            val splitPropsName = propertyKey.substringAfterLast('.').split(Regex("[_\\-]"))
            val firstPropName = splitPropsName.firstOrNull() ?: return null
            memberName = firstPropName + splitPropsName.subList(1, splitPropsName.size).joinToString(separator = "") {
                it.lowercase().capitalize()
            }
            setterName = "set${memberName.capitalize()}"
        }

        @Suppress("NAME_SHADOWING")
        val sourceType = sourceType.substringBeforeLast('#').replace('$', '.')
        val foundClass = javaPsiFacade.findClass(sourceType, GlobalSearchScope.allScope(project)) ?: return null
        return findMember(foundClass, memberName, setterName) ?: foundClass
    }

    private fun findMember(
        foundClass: PsiClass,
        fieldName: String,
        setterName: String
    ): PsiMember? {
        return (foundClass.findMethodsByName(setterName, true).firstOrNull()
            ?: foundClass.findFieldByName(fieldName, true))
    }

}
