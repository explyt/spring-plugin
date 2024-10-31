package com.esprito.spring.aop.service

import com.esprito.spring.aop.SpringAopBundle
import com.esprito.spring.aop.SpringAopIcons
import com.esprito.spring.core.externalsystem.model.SpringAspectData
import com.esprito.spring.core.externalsystem.utils.NativeBootUtils
import com.esprito.util.ExplytPsiUtil.isPrivate
import com.esprito.util.ExplytPsiUtil.resolvedPsiClass
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NotNullLazyValue
import com.intellij.psi.PsiElement
import org.jetbrains.uast.UClass
import org.jetbrains.uast.toUElement

class PointCutMethodsLineMarkerProvider : RelatedItemLineMarkerProvider() {
    override fun collectNavigationMarkers(
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        val uClass = element.toUElement() ?: return

        if (uClass is UClass) {
            val javaPsi = uClass.javaPsi
            val aspectSearchService = AspectSearchService.getInstance(element.project)
            val pointCutClasses = aspectSearchService.getPointCutQualifiedClasses()
            if (javaPsi.qualifiedName?.let { pointCutClasses.contains(it) } == false) return
            val aspectDataByMethodName = aspectSearchService.getAspectsData()
                .filter { it.beanQualifiedClassName == javaPsi.qualifiedName }
                .groupBy { it.beanMethodName }
            for (method in uClass.methods) {
                val sourcePsi = method.uastAnchor?.sourcePsi ?: continue
                if (method.isPrivate) continue
                val springAspectDataByMethod = aspectDataByMethodName[method.name] ?: continue
                val parametersList = method.javaPsi.parameterList.parameters
                    .mapNotNull { it.type.resolvedPsiClass?.qualifiedName }
                val aspectDataFilteredByParams = springAspectDataByMethod
                    .filter { it.methodQualifiedParams == parametersList }
                    .takeIf { it.isNotEmpty() } ?: continue
                val builder = NavigationGutterIconBuilder.create(SpringAopIcons.Advice)
                    .setAlignment(GutterIconRenderer.Alignment.LEFT)
                    .setTargets(NotNullLazyValue.lazy { findMethods(aspectDataFilteredByParams, element.project) })
                    .setTooltipText(SpringAopBundle.message("explyt.spring.gutter.aop.tooltip.pointcut.method"))
                    .setPopupTitle(SpringAopBundle.message("explyt.spring.gutter.aop.title.pointcut.method"))
                    .setEmptyPopupText(SpringAopBundle.message("explyt.spring.gutter.aop.title.pointcut.method.empty"))
                result.add(builder.createLineMarkerInfo(sourcePsi))
            }
        }
    }

    private fun findMethods(aspects: List<SpringAspectData>, project: Project): Collection<PsiElement> {
        return aspects.mapNotNull { toPsiMethod(it, project) }
    }

    private fun toPsiMethod(aspectData: SpringAspectData, project: Project): PsiElement? {
        val psiClass = NativeBootUtils.findProjectClass(aspectData.aspectQualifiedClassName, project) ?: return null
        return psiClass.findMethodsByName(aspectData.aspectMethodName, false).firstOrNull()
    }
}