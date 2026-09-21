/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.test.util

import com.intellij.codeInsight.daemon.GutterMark
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.MergeableLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.navigation.NavigationGutterIconRenderer
import com.intellij.codeInsight.navigation.impl.PsiTargetPresentationRenderer
import com.intellij.platform.backend.presentation.TargetPresentation
import com.intellij.psi.PsiElement
import com.intellij.psi.presentation.java.SymbolPresentationUtil
import com.intellij.testFramework.fixtures.JavaCodeInsightTestFixture
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase
import com.intellij.util.Function
import com.intellij.util.containers.ContainerUtil
import junit.framework.TestCase
import java.util.function.Supplier
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

    /**
     * Renders the targets the way the navigation popup will, so a test constrains the renderer the gutter actually
     * installs. Building a renderer in the test instead would stay green even when the gutter installs none and the
     * platform falls back to raw element text.
     */
    fun getGutterTargetPresentations(gutterMark: GutterMark?): List<TargetPresentation> {
        val gutterRenderer = toNavigationGutterIconRenderer(gutterMark)
            ?: throw AssertionError("Expected a navigating gutter, but got: $gutterMark")
        val targetRenderer = installedTargetRenderer(gutterRenderer)
        return gutterRenderer.targetElements.map { targetRenderer.getPresentation(it) }
    }

    fun getGutterPopupTitle(gutterMark: GutterMark?): String? {
        val gutterRenderer = toNavigationGutterIconRenderer(gutterMark)
            ?: throw AssertionError("Expected a navigating gutter, but got: $gutterMark")
        return readPlatformField(gutterRenderer, "myPopupTitle") as? String
    }

    /**
     * Reads the balloon text the gutter shows when it resolves no target. Lazy targets keep the icon installed
     * even with nothing to navigate to, so this string is the whole answer a user gets in that case.
     */
    fun getGutterEmptyText(gutterMark: GutterMark?): String? {
        val gutterRenderer = toNavigationGutterIconRenderer(gutterMark)
            ?: throw AssertionError("Expected a navigating gutter, but got: $gutterMark")
        return readPlatformField(gutterRenderer, "myEmptyText") as? String
    }

    private fun installedTargetRenderer(
        gutterRenderer: NavigationGutterIconRenderer
    ): PsiTargetPresentationRenderer<PsiElement> {
        @Suppress("UNCHECKED_CAST")
        val supplier = readPlatformField(gutterRenderer, "myTargetRenderer")
            as? Supplier<PsiTargetPresentationRenderer<PsiElement>>
            ?: throw AssertionError(
                "The gutter installs no target renderer, so the platform presents targets by raw element text"
            )
        return supplier.get()
    }

    private fun readPlatformField(gutterRenderer: NavigationGutterIconRenderer, fieldName: String): Any? {
        val field = try {
            NavigationGutterIconRenderer::class.java.getDeclaredField(fieldName)
        } catch (e: NoSuchFieldException) {
            throw AssertionError("NavigationGutterIconRenderer.$fieldName is gone; update this helper", e)
        }
        field.isAccessible = true
        return field.get(gutterRenderer)
    }

    private fun toNavigationGutterIconRenderer(gutterMark: GutterMark?): NavigationGutterIconRenderer? =
        when (gutterMark) {
            is NavigationGutterIconRenderer -> gutterMark
            is LineMarkerInfo.LineMarkerGutterIconRenderer<*> ->
                gutterMark.lineMarkerInfo.navigationHandler as? NavigationGutterIconRenderer

            else -> null
        }

}
