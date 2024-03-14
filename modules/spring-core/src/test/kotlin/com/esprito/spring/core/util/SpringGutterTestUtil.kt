package com.esprito.spring.core.util

import com.intellij.codeInsight.daemon.GutterMark
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.navigation.NavigationGutterIconRenderer
import com.intellij.psi.PsiElement
import com.intellij.psi.presentation.java.SymbolPresentationUtil
import com.intellij.testFramework.fixtures.JavaCodeInsightTestFixture
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase
import com.intellij.util.Function
import com.intellij.util.containers.ContainerUtil
import junit.framework.TestCase
import javax.swing.Icon

object SpringGutterTestUtil {
    private val PSI_FUNCTION = Function<PsiElement, String> { SymbolPresentationUtil.getSymbolPresentableText(it) }

    fun getGutterTargetsStrings(gutterMark: GutterMark?): List<String> =
        when (gutterMark) {
            is LineMarkerInfo.LineMarkerGutterIconRenderer<*> -> {
                val renderer = LightJavaCodeInsightFixtureTestCase.assertInstanceOf(
                    gutterMark,
                    LineMarkerInfo.LineMarkerGutterIconRenderer::class.java
                )
                lineMarkersToString(renderer.lineMarkerInfo, PSI_FUNCTION)
            }

            is NavigationGutterIconRenderer -> ContainerUtil.map(gutterMark.targetElements, PSI_FUNCTION)
            null -> throw AssertionError("gutter is null")
            else -> throw IllegalArgumentException(gutterMark.javaClass.toString() + " not supported")
        }

    private fun lineMarkersToString(
        lineMarkerInfo: LineMarkerInfo<out PsiElement>,
        targetNamer: Function<PsiElement, String>
    ): List<String> {
        val navigationHandler = lineMarkerInfo.navigationHandler

        if (navigationHandler is NavigationGutterIconRenderer) {
            val targetElements = navigationHandler.targetElements
            return ContainerUtil.map(targetElements, targetNamer)
        }

        // custom GutterIconNavigationHandler: fallback to related items
        if (lineMarkerInfo is RelatedItemLineMarkerInfo) {
            val gotoItems =
                ArrayList(lineMarkerInfo.createGotoRelatedItems()).sortedBy { item -> item.customContainerName }
            return gotoItems.mapNotNull { it.element }.map { targetNamer.`fun`(it) }
        }
        return emptyList()
    }

    fun getAllBeanGuttersByIcon(myFixture: JavaCodeInsightTestFixture, icon: Icon): List<GutterMark> {
        val allBeanGutters = myFixture.findAllGutters()
            .filter { it.icon == icon }
        TestCase.assertTrue(allBeanGutters.isNotEmpty())
        return allBeanGutters
    }

    fun getGutterTargetString(allBeanGutters: List<GutterMark>): List<List<String>> {
        return allBeanGutters.asSequence()
            .map { getGutterTargetsStrings(it) }
            .filter { it.isNotEmpty() }
            .toList()
    }

}