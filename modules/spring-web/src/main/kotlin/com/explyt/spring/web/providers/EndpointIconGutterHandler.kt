package com.explyt.spring.web.providers

import com.explyt.spring.core.SpringIcons
import com.explyt.spring.core.statistic.StatisticActionId
import com.explyt.spring.core.statistic.StatisticService
import com.explyt.spring.web.SpringWebBundle
import com.explyt.spring.web.inspections.quickfix.AddEndpointToOpenApiIntention
import com.explyt.spring.web.inspections.quickfix.AddEndpointToOpenApiIntention.EndpointInfo
import com.explyt.spring.web.providers.EndpointUsageSearcher.findMockMvcEndpointUsage
import com.explyt.spring.web.providers.EndpointUsageSearcher.findOpenApiJsonEndpoints
import com.explyt.spring.web.providers.EndpointUsageSearcher.findOpenApiYamlEndpoints
import com.explyt.spring.web.util.SpringWebUtil
import com.intellij.codeInsight.daemon.GutterIconNavigationHandler
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.ide.actions.ApplyIntentionAction
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.util.EditorUtil
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiUtilCore
import com.intellij.ui.awt.RelativePoint
import java.awt.event.MouseEvent

class EndpointIconGutterHandler(private val endpointInfo: EndpointInfo) : GutterIconNavigationHandler<PsiElement> {

    override fun navigate(e: MouseEvent, psiElement: PsiElement) {
        val module = ModuleUtilCore.findModuleForPsiElement(psiElement) ?: return
        StatisticService.getInstance().addActionUsage(StatisticActionId.GUTTER_CONTROLLER_ENDPOINT_USAGE)

        val path = endpointInfo.path
        val requestMethods = endpointInfo.requestMethods

        val navigationTargets = findOpenApiJsonEndpoints(path, requestMethods, module) +
                findOpenApiYamlEndpoints(path, requestMethods, module) +
                findMockMvcEndpointUsage(path, requestMethods, module)

        if (navigationTargets.isEmpty()) {
            val containingFile = psiElement.containingFile ?: return
            val virtualFile = PsiUtilCore.getVirtualFile(psiElement) ?: return
            val project = psiElement.project
            val editor = FileEditorManager.getInstance(project).selectedTextEditor ?: return
            editor.caretModel.moveToOffset(psiElement.textOffset)
            val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.document) ?: return

            if (psiFile.virtualFile != virtualFile) return

            val popup = createEndpointActionsGroupPopup(containingFile, endpointInfo, editor) ?: return
            popup.show(RelativePoint(e))
        } else {
            // Have tried to use corresponding navigationHandler, didn't work
            NavigationGutterIconBuilder
                .create(SpringIcons.ReadAccess)
                .setTargets(navigationTargets)
                .setTargetRenderer { SpringWebUtil.getTargetRenderer() }
                .setPopupTitle(SpringWebBundle.message("explyt.spring.web.gutter.endpoint.popup"))
                .createLineMarkerInfo(psiElement)
                .navigationHandler
                .navigate(e, psiElement)
        }
    }

    private fun createEndpointActionsGroupPopup(
        file: PsiFile, endpointInfo: EndpointInfo, editor: Editor
    ): JBPopup? {
        val intention = AddEndpointToOpenApiIntention(endpointInfo)
        val actions = listOf<AnAction>(
            ApplyIntentionAction(
                intention, intention.text, editor, file
            )
        )
        if (actions.isEmpty()) return null

        return JBPopupFactory.getInstance().createActionGroupPopup(
            SpringWebBundle.message("explyt.spring.web.gutter.endpoint.actions.title"),
            DefaultActionGroup(actions),
            EditorUtil.getEditorDataContext(editor),
            JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
            false
        )
    }

}
