package com.esprito.spring.core.properties

import com.esprito.spring.core.completion.properties.PropertyHint
import com.esprito.spring.core.completion.properties.SpringConfigurationPropertiesSearch
import com.esprito.spring.core.util.PropertyUtil
import com.esprito.spring.core.util.SpringCoreUtil
import com.intellij.lang.properties.psi.impl.PropertyImpl
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.impl.FakePsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext

class SpringConfigurationPropertiesKeyReferenceProvider : PsiReferenceProvider() {

    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
        val property = PsiTreeUtil.getParentOfType(
            element,
            PropertyImpl::class.java
        ) ?: return PsiReference.EMPTY_ARRAY

        val propertyKey = property.key ?: return PsiReference.EMPTY_ARRAY

        if (!SpringCoreUtil.isConfigurationPropertyFile(property.containingFile)) {
            return emptyArray()
        }

        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return PsiReference.EMPTY_ARRAY
        val allHints = SpringConfigurationPropertiesSearch.getInstance(element.project)
            .getAllHints(module)

        val keyHint: PropertyHint = allHints.find { hint ->
            val hintName = hint.name
            val keysIdx = hintName.lastIndexOf(".keys")
            if (keysIdx == -1) {
                return@find false
            }
            propertyKey.startsWith(hintName.substring(0, keysIdx))
        } ?: return arrayOf(ConfigurationPropertyKeyReference(element, propertyKey))

        val prefix = keyHint.name.substringBefore(".keys")
        return if (propertyKey.startsWith("$prefix.")
            && keyHint.providers.any { it.name == "logger-name" }
        ) {
            val prefixLength = prefix.length

            val result = mutableListOf<PsiReference>(
                ConfigurationPropertyKeyReference(
                    element,
                    prefix,
                    TextRange.from(0, prefixLength)
                )
            )

            val javaClassReferenceSet = PropertiesJavaClassReferenceSet(
                propertyKey.substringAfter("$prefix."),
                element,
                prefixLength + 1
            )

            result.addAll(javaClassReferenceSet.references)

            result.toTypedArray()
        } else {
            arrayOf(ConfigurationPropertyKeyReference(element, propertyKey))
        }
    }
}

open class ConfigurationPropertyKeyReference(
    element: PsiElement,
    private val propertyKey: String,
    textRange: TextRange? = null
) : PsiReferenceBase.Poly<PsiElement>(element, textRange, false) {

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return emptyArray()
        val project = element.project
        val foundProperty = SpringConfigurationPropertiesSearch.getInstance(project)
            .findProperty(module, propertyKey) ?: return emptyArray()
        val sourceType = foundProperty.sourceType ?: return emptyArray()
        val sourceMember = PropertyUtil.findSourceMember(propertyKey, sourceType, project)
        if (sourceMember != null) {
            return PsiElementResolveResult.createResults(ConfigKeyPsiElement(sourceMember))
        }
        return emptyArray()
    }

}

class ConfigKeyPsiElement(private val member: PsiMember) : FakePsiElement() {

    override fun getParent(): PsiElement = member

    override fun getNavigationElement(): PsiElement = member.navigationElement

    override fun navigate(requestFocus: Boolean) {
        member.navigate(requestFocus)
    }

    override fun canNavigate(): Boolean {
        return member.canNavigate()
    }

    override fun canNavigateToSource(): Boolean {
        return member.canNavigateToSource()
    }

    override fun getPresentation(): ItemPresentation? {
        return member.presentation
    }

    override fun getPresentableText(): String? {
        return member.presentation?.presentableText
    }
}