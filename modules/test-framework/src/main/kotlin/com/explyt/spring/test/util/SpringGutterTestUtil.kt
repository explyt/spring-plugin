/*
 * Copyright © 2024 Explyt Ltd
 *
 * All rights reserved.
 *
 * This code and software are the property of Explyt Ltd and are protected by copyright and other intellectual property laws.
 *
 * You may use this code under the terms of the Explyt Source License Version 1.0 ("License"), if you accept its terms and conditions.
 *
 * By installing, downloading, accessing, using, or distributing this code, you agree to the terms and conditions of the License.
 * If you do not agree to such terms and conditions, you must cease using this code and immediately delete all copies of it.
 *
 * You may obtain a copy of the License at: https://github.com/explyt/spring-plugin/blob/main/EXPLYT-SOURCE-LICENSE.md
 *
 * Unauthorized use of this code constitutes a violation of intellectual property rights and may result in legal action.
 */

package com.explyt.spring.test.util

import com.intellij.codeInsight.daemon.GutterMark
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.MergeableLineMarkerInfo
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
        // Simple case: renderer with direct target elements
        val navigationHandler = lineMarkerInfo.navigationHandler
        if (navigationHandler is NavigationGutterIconRenderer) {
            val targetElements = navigationHandler.targetElements
            return ContainerUtil.map(targetElements, targetNamer)
        }

        // Related item fallback
        if (lineMarkerInfo is RelatedItemLineMarkerInfo<*>) {
            val gotoItems = ArrayList(lineMarkerInfo.createGotoRelatedItems())
                .sortedBy { item -> item.customContainerName }
            return gotoItems.mapNotNull { it.element }.map { targetNamer.`fun`(it) }
        }

        // Merged gutters: unwrap nested markers using public API
        val merged: List<MergeableLineMarkerInfo<*>> = MergeableLineMarkerInfo.getMergedMarkers(lineMarkerInfo)
        if (merged.isNotEmpty()) {
            val results = mutableListOf<String>()
            merged.forEach { info ->
                val renderer = info.createGutterRenderer()
                if (renderer is NavigationGutterIconRenderer) {
                    results += ContainerUtil.map(renderer.targetElements, targetNamer)
                } else if (info is RelatedItemLineMarkerInfo<*>) {
                    val gotoItems = ArrayList(info.createGotoRelatedItems())
                        .sortedBy { item -> item.customContainerName }
                    results += gotoItems.mapNotNull { it.element }.map { targetNamer.`fun`(it) }
                }
            }
            return results
        }
        return emptyList()
    }

    fun getAllBeanGuttersByIcon(myFixture: JavaCodeInsightTestFixture, icon: Icon): List<GutterMark> {
        return getAllBeanGuttersByIcon(myFixture, listOf(icon))
    }

    fun getAllBeanGuttersByIcon(myFixture: JavaCodeInsightTestFixture, icon: Collection<Icon>): List<GutterMark> {
        val allBeanGutters = myFixture.findAllGutters()
            .filter { icon.contains(it.icon) }
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