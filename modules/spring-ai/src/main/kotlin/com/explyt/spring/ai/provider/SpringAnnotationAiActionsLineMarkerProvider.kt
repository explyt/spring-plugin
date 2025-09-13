/*
 * Copyright © 2025 Explyt Ltd
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

package com.explyt.spring.ai.provider

import com.explyt.spring.ai.SpringAiBundle.message
import com.explyt.spring.ai.SpringAiIcons
import com.explyt.spring.ai.action.ConvertControllerToOpenapiAction
import com.explyt.spring.ai.service.AiPluginService
import com.explyt.spring.ai.service.AiUtils
import com.explyt.spring.core.SpringCoreClasses.CONTROLLER
import com.explyt.spring.core.SpringCoreClasses.SPRING_BOOT_APPLICATION
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedByOrSelf
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.editor.markup.MarkupEditorFilter
import com.intellij.openapi.editor.markup.MarkupEditorFilterFactory
import com.intellij.psi.PsiElement
import org.jetbrains.uast.UAnnotation
import org.jetbrains.uast.UClass
import org.jetbrains.uast.getUParentForIdentifier
import javax.swing.Icon


class SpringAiActionsLineMarkerProvider : LineMarkerProvider {

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        val uElement = getUParentForIdentifier(element) as? UClass ?: return null
        val springAnnotation = findUAnnotation(uElement) ?: return null
        val sourcePsi = springAnnotation.uastAnchor?.sourcePsi ?: return null
        val actionGroup = DefaultActionGroup()
        val aiAnnotationType = aiAnnotateType(springAnnotation)
        when (aiAnnotationType) {
            AiAnnotateType.SPRING_BOOT -> {
                actionGroup.add(GenerateKafkaConfigAction(sourcePsi))
                actionGroup.add(GenerateSecurityConfigAction(sourcePsi))
            }

            AiAnnotateType.CONTROLLER -> {
                actionGroup.add(ConvertControllerToOpenapiAction())
            }

            else -> {
                return null
            }
        }

        return SpringMarkerInfo(
            sourcePsi,
            SpringAiIcons.aiAssistant,
            { message("explyt.spring.ai.actions") },
            actionGroup
        )
    }

    private fun aiAnnotateType(springAnnotation: UAnnotation): AiAnnotateType? {
        return when {
            springAnnotation.javaPsi?.isMetaAnnotatedByOrSelf(SPRING_BOOT_APPLICATION) == true -> {
                AiAnnotateType.SPRING_BOOT
            }

            springAnnotation.javaPsi?.isMetaAnnotatedByOrSelf(CONTROLLER) == true -> {
                AiAnnotateType.CONTROLLER
            }

            else -> {
                null
            }
        }
    }

    private fun findUAnnotation(uElement: UClass): UAnnotation? {
        return uElement.uAnnotations
            .firstOrNull {
                it.javaPsi?.isMetaAnnotatedByOrSelf(SPRING_BOOT_APPLICATION) == true
                        || it.javaPsi?.isMetaAnnotatedByOrSelf(CONTROLLER) == true
            }
    }

    private enum class AiAnnotateType {
        SPRING_BOOT, CONTROLLER
    }
}

private class GenerateKafkaConfigAction(val sourcePsi: PsiElement) :
    AnAction(message("explyt.spring.ai.action.kafka.conf")) {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val languageSentence = AiUtils.getLanguageSentence(sourcePsi)
        val prompt = "Generate Spring Kafka configuration. Add spring-kafka dependency if needed. $languageSentence"
        AiPluginService.getInstance(project).performPrompt(prompt, emptyList())
    }
}

private class GenerateSecurityConfigAction(val sourcePsi: PsiElement) :
    AnAction(message("explyt.spring.ai.action.security.conf")) {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val languageSentence = AiUtils.getLanguageSentence(sourcePsi)
        val prompt = """
            Generate Spring Security configuration. Add spring-security dependency if needed. $languageSentence 
            Ask me what authentication i want: BasicAuthentication, DaoAuthentication, JwtAuthentication, OAuth2Authentication, LdapAuthentication ?
        """.trimIndent()
        AiPluginService.getInstance(project).performPrompt(prompt, emptyList())
    }
}

class SpringMarkerInfo(
    element: PsiElement,
    icon: Icon,
    tooltipProvider: (PsiElement) -> String,
    private val myActionGroup: DefaultActionGroup,
) : LineMarkerInfo<PsiElement>(
    element,
    element.textRange,
    icon,
    tooltipProvider,
    { _, _ -> }, // to have `hand` cursor on hover
    GutterIconRenderer.Alignment.LEFT,
    { tooltipProvider(element) }
) {

    override fun createGutterRenderer(): LineMarkerGutterIconRenderer<PsiElement> =
        object : LineMarkerGutterIconRenderer<PsiElement>(this) {
            override fun getPopupMenuActions(): ActionGroup = myActionGroup

            override fun getClickAction(): AnAction? = null
        }

    override fun getEditorFilter(): MarkupEditorFilter =
        MarkupEditorFilterFactory.createIsNotDiffFilter()
}
