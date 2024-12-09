package com.explyt.spring.web.providers

import com.explyt.spring.core.SpringIcons
import com.explyt.spring.core.statistic.StatisticActionId
import com.explyt.spring.core.statistic.StatisticService
import com.explyt.spring.web.SpringWebBundle
import com.explyt.spring.web.editor.openapi.OpenApiUIEditor
import com.explyt.spring.web.editor.openapi.OpenApiUtils.getTagAndOperationIdFor
import com.explyt.spring.web.inspections.quickfix.AddEndpointToOpenApiIntention
import com.explyt.spring.web.inspections.quickfix.AddEndpointToOpenApiIntention.EndpointInfo
import com.explyt.spring.web.providers.EndpointUsageSearcher.findMockMvcEndpointUsage
import com.explyt.spring.web.providers.EndpointUsageSearcher.findOpenApiJsonEndpoints
import com.explyt.spring.web.providers.EndpointUsageSearcher.findOpenApiYamlEndpoints
import com.explyt.spring.web.providers.EndpointUsageSearcher.findWebTestClientEndpointUsage
import com.explyt.spring.web.util.SpringWebUtil
import com.intellij.codeInsight.daemon.GutterIconNavigationHandler
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.icons.AllIcons
import com.intellij.ide.actions.ApplyIntentionAction
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.Separator
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.util.EditorUtil
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.fileEditor.TextEditorWithPreview.Layout.SHOW_PREVIEW
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.util.PsiUtilCore
import com.intellij.ui.awt.RelativePoint
import java.awt.event.MouseEvent

class EndpointIconGutterHandler(private val endpointInfo: EndpointInfo) : GutterIconNavigationHandler<PsiElement> {

    override fun navigate(e: MouseEvent, psiElement: PsiElement) {
        val module = ModuleUtilCore.findModuleForPsiElement(psiElement) ?: return
        StatisticService.getInstance().addActionUsage(StatisticActionId.GUTTER_CONTROLLER_ENDPOINT_USAGE)

        val path = endpointInfo.path
        val requestMethods = endpointInfo.requestMethods

        val openapiEndpoints = findOpenApiJsonEndpoints(path, requestMethods, module) +
                findOpenApiYamlEndpoints(path, requestMethods, module)

        val testEndpointUsages =
            findMockMvcEndpointUsage(path, requestMethods, module) +
                    findWebTestClientEndpointUsage(path, requestMethods, module)

        val navigatableLineMarker = createNavigatableLinemarker(openapiEndpoints + testEndpointUsages, psiElement)

        val containingFile = psiElement.containingFile ?: return
        val virtualFile = PsiUtilCore.getVirtualFile(psiElement) ?: return
        val project = psiElement.project
        val editor = FileEditorManager.getInstance(project).selectedTextEditor ?: return
        editor.caretModel.moveToOffset(psiElement.textOffset)
        val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.document) ?: return

        if (psiFile.virtualFile != virtualFile) return

        val popup =
            createEndpointActionsGroupPopup(
                containingFile,
                endpointInfo,
                editor,
                navigatableLineMarker,
                openapiEndpoints,
                e
            )
        popup.show(RelativePoint(e))
    }

    private fun createNavigatableLinemarker(
        endpointUsages: List<PsiElement>,
        psiElement: PsiElement
    ): RelatedItemLineMarkerInfo<PsiElement>? {
        if (endpointUsages.isEmpty()) return null

        return NavigationGutterIconBuilder
            .create(SpringIcons.ReadAccess)
            .setTargets(endpointUsages)
            .setTargetRenderer { SpringWebUtil.getTargetRenderer() }
            .setPopupTitle(SpringWebBundle.message("explyt.spring.web.gutter.endpoint.popup"))
            .createLineMarkerInfo(psiElement)
    }

    private fun createEndpointActionsGroupPopup(
        file: PsiFile,
        endpointInfo: EndpointInfo,
        editor: Editor,
        navigatableLineMarker: RelatedItemLineMarkerInfo<PsiElement>?,
        openapiEndpoints: List<PsiElement>,
        mouseEvent: MouseEvent
    ): JBPopup {
        val intention = AddEndpointToOpenApiIntention(endpointInfo)

        val actions = mutableListOf(
            if (openapiEndpoints.isEmpty()) ApplyIntentionAction(intention, intention.text, editor, file)
            else RunInSwaggerAction(openapiEndpoints.first())
        )

        if (navigatableLineMarker != null) {
            actions.addAll(
                listOf(
                    Separator.create(),
                    NavigateAction("Navigate To Endpoint Usage", navigatableLineMarker, mouseEvent)
                )
            )
        }

        return JBPopupFactory.getInstance().createActionGroupPopup(
            SpringWebBundle.message("explyt.spring.web.gutter.endpoint.actions.title"),
            DefaultActionGroup(actions),
            EditorUtil.getEditorDataContext(editor),
            JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
            false
        )
    }

    class RunInSwaggerAction(psiElement: PsiElement) :
        AnAction("Run in Swagger", null, AllIcons.RunConfigurations.TestState.Run) {
        private val psiElementPointer =
            SmartPointerManager.getInstance(psiElement.project).createSmartPsiElementPointer(psiElement)

        override fun actionPerformed(e: AnActionEvent) {
            val psiElement = psiElementPointer.element ?: return
            val project = psiElement.project
            val virtualFile = psiElement.containingFile.virtualFile
            val openFileDescriptor = OpenFileDescriptor(project, virtualFile)
            val (tag, operationId) = getTagAndOperationIdFor(psiElement) ?: return

            val openapiEditor = FileEditorManager.getInstance(project)
                .openEditor(openFileDescriptor, true)
                .firstNotNullOfOrNull { it as? OpenApiUIEditor }
                ?: return

            openapiEditor.showPreviewFor(tag, operationId, SHOW_PREVIEW)
        }

    }

}
