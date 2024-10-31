package com.esprito.spring.core.providers

import com.esprito.spring.core.SpringCoreBundle
import com.esprito.spring.core.SpringCoreClasses
import com.esprito.spring.core.SpringIcons
import com.esprito.spring.core.service.SpringSearchServiceFacade
import com.esprito.util.ExplytPsiUtil.isEqualOrInheritor
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.util.NotNullLazyValue
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiType
import org.jetbrains.uast.*

class GetBeanLineMarkerProvider : RelatedItemLineMarkerProvider() {

    override fun collectNavigationMarkers(
        psiElement: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        val uElement = psiElement.toUElement() ?: return
        if (uElement !is UIdentifier) return
        if (uElement.name != "getBean") return
        val uParent = uElement.getParentOfType<UQualifiedReferenceExpression>() ?: return
        val uCallExpression = uParent.selector as? UCallExpression ?: return
        if (uCallExpression.kind != UastCallKind.METHOD_CALL) return
        if (uCallExpression.methodName != "getBean") return
        val psiMethod = uCallExpression.resolve() ?: return
        val targetClass = psiMethod.containingClass ?: return
        if (!targetClass.isEqualOrInheritor(SpringCoreClasses.BEAN_FACTORY)) return

        val nameIndex = psiMethod.parameterList.parameters.indexOfFirst { it.name == "name" }
        val name = uCallExpression.valueArguments.getOrNull(nameIndex)?.evaluateString() ?: ""
        val requiredTypeIndex = psiMethod.parameterList.parameters.indexOfFirst { it.name == "requiredType" }
        val requiredType = when (val arg = uCallExpression.valueArguments.getOrNull(requiredTypeIndex)) {
            is UQualifiedReferenceExpression -> (arg.receiver as? UClassLiteralExpression)?.type
            is UClassLiteralExpression -> arg.type
            else -> null
        } ?: psiElement.getUastParentOfType<UBinaryExpressionWithType>()?.type

        val builder = NavigationGutterIconBuilder.create(SpringIcons.SpringBeanDependencies)
            .setAlignment(GutterIconRenderer.Alignment.LEFT)
            .setTargets(NotNullLazyValue.lazy { getBeans(psiElement, name, requiredType) })
            .setTooltipText(SpringCoreBundle.message("explyt.spring.gutter.tooltip.title.choose.bean.candidate"))
            .setPopupTitle(SpringCoreBundle.message("explyt.spring.gutter.popup.title.choose.bean.candidate"))
            .setEmptyPopupText(SpringCoreBundle.message("explyt.spring.gutter.notfound.title.choose.bean.candidate"))

        result.add(builder.createLineMarkerInfo(psiElement))
    }


    private fun getBeans(psiElement: PsiElement, name: String, requiredType: PsiType?): Collection<PsiElement> {
        val module = ModuleUtilCore.findModuleForPsiElement(psiElement) ?: return emptyList()
        return SpringSearchServiceFacade.getInstance(psiElement.project)
            .findActiveBeanDeclarations(module, name, psiElement.language, requiredType)
    }

}