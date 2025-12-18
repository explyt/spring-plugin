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

package com.explyt.spring.web.view

import com.explyt.spring.core.statistic.StatisticActionId.*
import com.explyt.spring.core.statistic.StatisticService
import com.explyt.spring.web.SpringWebBundle
import com.explyt.spring.web.SpringWebClasses.URI_TYPE
import com.explyt.spring.web.loader.EndpointElement
import com.explyt.spring.web.loader.EndpointType
import com.explyt.spring.web.service.SpringWebEndpointsSearcher
import com.explyt.spring.web.view.nodes.EndpointNavigable
import com.explyt.spring.web.view.nodes.RootEndpointNode
import com.explyt.util.ExplytPsiUtil.toSmartPointer
import com.intellij.icons.AllIcons
import com.intellij.ide.util.treeView.AbstractTreeStructure
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.psi.PsiElement
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.ui.DoubleClickListener
import com.intellij.ui.JBColor
import com.intellij.ui.SearchTextField
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.filterField.FilterField
import com.intellij.ui.filterField.FilterFieldAction
import com.intellij.ui.tree.AsyncTreeModel
import com.intellij.ui.tree.StructureTreeModel
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.concurrency.AppExecutorUtil
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.components.BorderLayoutPanel
import com.intellij.util.ui.tree.TreeUtil
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import java.lang.ref.SoftReference
import java.util.concurrent.Callable
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath


class EndpointsToolWindow(private val project: Project) :
    SimpleToolWindowPanel(true),
    Disposable, DocumentListener {

    private val progressBar = JProgressBar()
    private val searchTextField: SearchTextField
    private val refreshButtonPresentation = Presentation()

    //private val treeModel: StructureTreeModel<AbstractTreeStructure>
    private val endpointTree: Tree

    @Volatile
    private var endpoints: SoftReference<List<EndpointElementViewData>>? = null

    private val httpTypeState = HashSet<String>()
    private val endpointTypeState = HashSet<EndpointType>()
    private val urlChanged = AtomicBoolean(false)

    init {
        this.preferredSize = Dimension(1080, 880)

        val borderPanel = BorderLayoutPanel()
        borderPanel.border = JBUI.Borders.customLine(JBColor.border(), 0, 0, 1, 0)

        endpointTree = Tree(DefaultTreeModel(DefaultMutableTreeNode()))
        val endpointTreeStructure = EndpointTreeStructure(RootEndpointNode(emptyList()))
        val treeModel = StructureTreeModel<AbstractTreeStructure>(endpointTreeStructure, this)
        endpointTree.setModel(AsyncTreeModel(treeModel, this))
        endpointTree.setRootVisible(false)
        treeModel.invalidateAsync()

        val scrollPane = JBScrollPane(endpointTree)
        borderPanel.add(scrollPane)

        setContent(borderPanel)
        val toolBarPanel = JPanel(BorderLayout())
        val filterPanel = JPanel()
        filterPanel.layout = BoxLayout(filterPanel, BoxLayout.X_AXIS)
        filterPanel.add(createRefreshButton())
        filterPanel.add(createExpandsAll())
        filterPanel.add(createCollapseAll())
        searchTextField = SearchTextField()
        searchTextField.text = ""
        searchTextField.textEditor.emptyText.text = "Search"
        filterPanel.add(searchTextField)
        filterPanel.add(createHttpTypeFilterActions())
        filterPanel.add(createEndpointTypeFilterActions())

        progressBar.isVisible = false
        toolBarPanel.add(filterPanel, BorderLayout.NORTH)
        toolBarPanel.add(progressBar, BorderLayout.SOUTH)

        toolbar = toolBarPanel
        searchTextField.addDocumentListener(this)

        addClickListeners()

        SwingUtilities.invokeLater { refreshData() }
    }

    private fun addClickListeners() {
        addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                if (e.keyCode != KeyEvent.VK_ENTER) return
                if (endpointTree.selectionCount == 0) return
                endpointTree.selectionPath?.let { navigation(it) }
            }
        })
        object : DoubleClickListener() {
            override fun onDoubleClick(event: MouseEvent): Boolean {
                val closestPathForLocation = endpointTree.getClosestPathForLocation(event.x, event.y)
                navigation(closestPathForLocation)
                return false
            }
        }.installOn(endpointTree)
    }

    private fun navigation(closestPathForLocation: TreePath?) {
        val lastUserObject = TreeUtil.getLastUserObject(closestPathForLocation)
        (lastUserObject as? EndpointNavigable)?.navigate()
    }

    private fun createRefreshButton(): JComponent {
        refreshButtonPresentation.text = "Refresh"
        refreshButtonPresentation.icon = AllIcons.Actions.Refresh

        return ActionButton(object : AnAction() {
            override fun actionPerformed(event: AnActionEvent) {
                StatisticService.getInstance().addActionUsage(ENDPOINTS_TOOLWINDOW_REFRESH)
                endpoints = null
                refreshData()
            }

        }, refreshButtonPresentation, "unknown", ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE)
    }

    private fun createExpandsAll(): JComponent {
        val presentation = Presentation()
        presentation.text = "Expand All"
        presentation.icon = AllIcons.Actions.Expandall

        return ActionButton(object : AnAction() {
            override fun actionPerformed(event: AnActionEvent) {
                TreeUtil.expandAll(endpointTree)
            }

        }, presentation, "unknown", ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE)
    }

    private fun createCollapseAll(): JComponent {
        val presentation = Presentation()
        presentation.text = "Collapse All"
        presentation.icon = AllIcons.Actions.Collapseall

        return ActionButton(object : AnAction() {
            override fun actionPerformed(event: AnActionEvent) {
                TreeUtil.collapseAll(endpointTree, 2)
            }

        }, presentation, "unknown", ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE)
    }

    private fun createHttpTypeFilterActions(): JComponent {
        val actionGroup = DefaultActionGroup()
        actionGroup.add(FilterFieldAction {
            createHttpTypeFilter()
        })

        val actionToolbar = ActionManager.getInstance()
            .createActionToolbar("HttpType", actionGroup, true)
        actionToolbar.targetComponent = this
        return actionToolbar.component
    }

    private fun createHttpTypeFilter(): FilterField {
        val title: String = SpringWebBundle.message("explyt.web.endpoints.tool.filter.http.type")

        return (object : FilterField(title) {
            override fun buildActions(): Collection<AnAction> {
                return URI_TYPE
                    .map { httpType ->
                        object : ToggleAction(httpType) {

                            override fun getActionUpdateThread(): ActionUpdateThread {
                                return ActionUpdateThread.BGT
                            }

                            override fun isSelected(event: AnActionEvent): Boolean {
                                return httpTypeState.contains(httpType)
                            }

                            override fun setSelected(event: AnActionEvent, state: Boolean) {
                                StatisticService.getInstance().addActionUsage(ENDPOINTS_TOOLWINDOW_HTTP_TYPE_FILTER)
                                if (state) httpTypeState.add(httpType) else httpTypeState.remove(httpType)
                                refreshData()
                            }

                        }
                    }
            }

            override fun getCurrentText() = ""
        })

    }

    private fun createEndpointTypeFilterActions(): JComponent {
        val actionGroup = DefaultActionGroup()
        actionGroup.add(FilterFieldAction {
            createEndpointTypeFilter()
        })
        val actionToolbar = ActionManager.getInstance()
            .createActionToolbar("EndpointType", actionGroup, true)
        actionToolbar.targetComponent = this
        return actionToolbar.component
    }

    private fun createEndpointTypeFilter(): FilterField {
        val title: String = SpringWebBundle.message("explyt.web.endpoints.tool.filter.endpoint.type")
        val disposable = this

        return (object : FilterField(title) {
            override fun buildActions(): Collection<AnAction> {
                return SpringWebEndpointsSearcher.getInstance(project)
                    .getLoadersTypes()
                    .map { endpointType ->
                        object : ToggleAction(endpointType.readable) {

                            override fun getActionUpdateThread(): ActionUpdateThread {
                                return ActionUpdateThread.BGT
                            }

                            override fun isSelected(event: AnActionEvent): Boolean {
                                return endpointTypeState.contains(endpointType)
                            }

                            override fun setSelected(event: AnActionEvent, state: Boolean) {
                                StatisticService.getInstance()
                                    .addActionUsage(ENDPOINTS_TOOLWINDOW_ENDPOINT_TYPE_FILTER)
                                if (state) endpointTypeState.add(endpointType) else
                                    endpointTypeState.remove(endpointType)

                                refreshData()
                            }

                        }
                    }
            }

            override fun getCurrentText() = ""
        })

    }

    override fun dispose() {
    }

    private fun onDocumentChange() {
        StatisticService.getInstance().addActionUsage(ENDPOINTS_TOOLWINDOW_SEARCH_TEXT)
        urlChanged.set(true)
        AppExecutorUtil.getAppScheduledExecutorService().schedule(
            {
                val isChanged = urlChanged.get()
                if (isChanged) {
                    ApplicationManager.getApplication().invokeLater { refreshData() }
                    urlChanged.set(false)
                }
            }, 1, TimeUnit.SECONDS
        )
    }

    override fun insertUpdate(e: DocumentEvent?) {
        onDocumentChange()
    }

    override fun removeUpdate(e: DocumentEvent?) {
        onDocumentChange()
    }

    override fun changedUpdate(e: DocumentEvent?) {
        onDocumentChange()
    }

    private fun refreshData() {
        refreshButtonPresentation.isEnabled = false

        progressBar.setIndeterminate(true)
        progressBar.isVisible = true
        endpointTree.emptyText.text = "Loading..."

        ReadAction.nonBlocking(Callable { getViewData() })
            .inSmartMode(project)
            .finishOnUiThread(ModalityState.current()) {
                endpointTree.emptyText.text = "Nothing to show"
                val endpointTreeStructure = EndpointTreeStructure(RootEndpointNode(it))
                val treeModel = StructureTreeModel<AbstractTreeStructure>(endpointTreeStructure, this)
                endpointTree.setModel(AsyncTreeModel(treeModel, this))
                endpointTree.setRootVisible(false)
                treeModel.invalidateAsync()

                progressBar.setIndeterminate(false)
                progressBar.isVisible = false

                refreshButtonPresentation.isEnabled = true

                TreeUtil.expandAll(endpointTree)
            }
            .expireWith(this)
            .submit(AppExecutorUtil.getAppExecutorService())
    }

    private fun getViewData(): List<EndpointViewByType> {
        var endpointsLocal = endpoints?.get()
        if (endpointsLocal == null) {
            endpointsLocal = getAllEndpoints()
            endpoints = SoftReference(endpointsLocal)
        }
        val urlFilter = searchTextField.text ?: ""
        val filteredEndpointsMap = applyFilters(endpointsLocal, urlFilter, httpTypeState, endpointTypeState)
            .groupBy { it.type }
        val typeNodes = mutableListOf<EndpointViewByType>()
        for (type in EndpointType.entries) {
            val elements = filteredEndpointsMap[type] ?: continue
            val elementsByClass = groupElementsByClass(elements)
            typeNodes.add(EndpointViewByType(type, elementsByClass))
        }
        return typeNodes
    }

    private fun applyFilters(
        endpoints: List<EndpointElementViewData>,
        urlFilter: String,
        httpTypeState: Set<String>,
        endpointTypeState: HashSet<EndpointType>
    ): List<EndpointElementViewData> {
        if (urlFilter.length < 2 && httpTypeState.isEmpty() && endpointTypeState.isEmpty()) return endpoints
        return endpoints.asSequence()
            .filter { urlFilter.length < 2 || it.path.contains(urlFilter, true) }
            .filter { endpointTypeState.isEmpty() || it.type in endpointTypeState }
            .filter { httpTypeState.isEmpty() || it.method in httpTypeState }
            .toList()
    }

    private fun getAllEndpoints(): List<EndpointElementViewData> {
        return SpringWebEndpointsSearcher.getInstance(project).getAllEndpoints()
            .flatMap { mapToEndpointElementViewData(it) }
    }

    private fun mapToEndpointElementViewData(element: EndpointElement): List<EndpointElementViewData> {
        val classOrFileName = element.containingClass?.name
            ?: element.containingFile?.name ?: return emptyList()
        return element.requestMethods.asSequence()
            .map {
                EndpointElementViewData(
                    element.type, element.psiElement.toSmartPointer(), classOrFileName, it, element.path
                )
            }
            .sortedBy { it.classOrFileName + it.method }
            .toList()
    }

    private fun groupElementsByClass(list: List<EndpointElementViewData>): List<EndpointViewWithContainerName> {
        return list.groupBy { it.classOrFileName }.asSequence()
            .map { toEndpointViewByClass(it) }
            .sortedBy { it.classOrFileName }
            .toList()
    }

    private fun toEndpointViewByClass(
        entry: Map.Entry<String, List<EndpointElementViewData>>
    ): EndpointViewWithContainerName {
        return EndpointViewWithContainerName(entry.key, entry.value.sortedBy { it.method })
    }
}

data class EndpointElementViewData(
    val type: EndpointType,
    val psiPointer: SmartPsiElementPointer<PsiElement>,
    val classOrFileName: String,
    val method: String,
    val path: String
)

data class EndpointViewWithContainerName(val classOrFileName: String, val list: List<EndpointElementViewData>)

data class EndpointViewByType(val type: EndpointType, val list: List<EndpointViewWithContainerName>)