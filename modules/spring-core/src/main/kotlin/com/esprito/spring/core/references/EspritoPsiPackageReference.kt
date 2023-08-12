package com.esprito.spring.core.references

import com.esprito.spring.core.util.PsiPackagesSearcher
import com.intellij.codeInsight.completion.CompletionUtilCore
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.util.TextRange
import com.intellij.pom.Navigatable
import com.intellij.psi.*
import com.intellij.psi.impl.FakePsiElement

class EspritoPsiPackageReference(element: PsiElement, range: TextRange?) :
    PsiReferenceBase.Poly<PsiElement>(element, range, false) {

    override fun getVariants(): Array<Any> {
        val text = (element as? PsiLiteralExpression)
            ?.value
            ?.toString()
            ?.substringBefore(CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED)
            ?: return emptyArray()

        val textWithoutLastWord = if (text.contains(".")) text.substringBeforeLast(".") + ".*" else "*"
        val prefilteredPackages = PsiPackagesSearcher.getFilteredPackages(element.project, element.resolveScope, textWithoutLastWord)

        val lastWord = if (textWithoutLastWord == "*") text else text.substringAfterLast(".")

        // we need filter for last word separate
        // made filter package for last word like in UE
        val filteredPackages =
            if (text.contains(".*."))
                prefilteredPackages.filter { it.name?.startsWith(lastWord) ?: false }
            else
                prefilteredPackages.filter { it.name?.contains(lastWord) ?: false }

        val countOfDistinctParentPackages = filteredPackages.map { it.parent as PsiPackage }.map { it.qualifiedName }.distinct().count()
        return filteredPackages
            .map {
                if (countOfDistinctParentPackages == 1)
                    LookupElementBuilder.create(it)
                else
                // if more than one variant show in popup full path of possible packages
                    LookupElementBuilder.create(it).withPresentableText(it.qualifiedName)
            }
            .toTypedArray()
    }

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult?> {
        val text = TextRange(
            ElementManipulators.getValueTextRange(element).startOffset,
            rangeInElement.endOffset
        ).substring(element.text)

        val packages = PsiPackagesSearcher.getFilteredPackages(element.project, element.resolveScope, text)
            .filter { it.parent != null }
            .map { NavigatablePackage(it) }

        return PsiElementResolveResult.createResults(packages)
    }

    override fun isReferenceTo(element: PsiElement): Boolean {
        if (element !is PsiPackage) return false
        val results = multiResolve(false)
        for (result in results) {
            if (element.manager.areElementsEquivalent(result!!.element, NavigatablePackage(element))) {
                return true
            }
        }
        return false
    }

    private class NavigatablePackage(
        private val psiPackage: PsiPackage
    ) : FakePsiElement(), Navigatable {
        override fun getParent(): PsiElement = psiPackage.parent

        override fun canNavigate(): Boolean {
            return true
        }

        override fun navigate(requestFocus: Boolean) {
            psiPackage.navigate(requestFocus)
        }

        override fun getName(): String {
            return psiPackage.qualifiedName
        }

        override fun isEquivalentTo(another: PsiElement?): Boolean {
            if (another !is NavigatablePackage) return false
            return this.psiPackage.isEquivalentTo(another.psiPackage)
        }
    }
}