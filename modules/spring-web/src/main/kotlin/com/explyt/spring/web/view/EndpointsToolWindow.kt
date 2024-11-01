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

import com.explyt.spring.web.SpringWebBundle
import com.explyt.spring.web.SpringWebClasses.URI_TYPE
import com.explyt.spring.web.service.SpringWebEndpointsSearcher
import com.explyt.spring.web.view.EndpointToolModelEventListener.EventType
import com.explyt.spring.web.view.EndpointToolModelEventListener.ModelEvent
import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.JBColor
import com.intellij.ui.SearchTextField
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.filterField.FilterField
import com.intellij.ui.filterField.FilterFieldAction
import com.intellij.ui.treeStructure.treetable.TreeTable
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.components.BorderLayoutPanel
import java.awt.Dimension
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.tree.TreeSelectionModel


class EndpointsToolWindow(private val project: Project) :
    SimpleToolWindowPanel(true),
    Disposable, DocumentListener {

    private var endpointsView: TreeTable
    private val endpointsModel: EndpointsTreeModel
    private val searchTextField: SearchTextField

    init {
        this.preferredSize = Dimension(1080, 880)

        val borderPanel = BorderLayoutPanel()
        borderPanel.border =
            JBUI.Borders.customLine(JBColor.border(), 0, 0, 1, 0)

        endpointsModel = EndpointsTreeModel(project)
        project.messageBus
            .connect(this)
            .subscribe(EndpointToolModelEventListener.TOPIC, endpointsModel)

        endpointsView = EndpointsTree(project, endpointsModel)
        endpointsView.setRootVisible(false)
        endpointsView.setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION)
        endpointsView.setTreeCellRenderer(EndpointCellRenderer())

        val scrollPane = JBScrollPane(endpointsView)
        borderPanel.add(scrollPane)

        setContent(borderPanel)
        val toolBar = JPanel()
        toolBar.layout = BoxLayout(toolBar, BoxLayout.X_AXIS)
        toolBar.add(createRefreshButton())
        val textFilter = endpointsModel.getTextFilterValue()
        searchTextField = SearchTextField()
        searchTextField.text = textFilter
        toolBar.add(searchTextField)
        toolBar.add(createHttpTypeFilterActions())
        toolBar.add(createEndpointTypeFilterActions())

        toolbar = toolBar
        searchTextField.addDocumentListener(this)
    }

    private fun createRefreshButton(): JComponent {
        val buttonPresentation = Presentation()
        buttonPresentation.text = "Refresh"
        buttonPresentation.icon = AllIcons.Actions.Refresh

        val disposable = this

        return ActionButton(object : AnAction() {
            override fun actionPerformed(event: AnActionEvent) {
                project.messageBus.syncPublisher(EndpointToolModelEventListener.TOPIC)
                    .handle(ModelEvent(EventType.UPDATE_DATA, disposable))
            }

        }, buttonPresentation, "unknown", ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE)
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
        val disposable = this

        return (object : FilterField(title) {
            override fun buildActions(): Collection<AnAction> {
                return URI_TYPE
                    .map { httpType ->
                        object : ToggleAction(httpType) {

                            override fun getActionUpdateThread(): ActionUpdateThread {
                                return ActionUpdateThread.BGT
                            }

                            override fun isSelected(event: AnActionEvent): Boolean {
                                return endpointsModel.isHttpTypeFilterActive(httpType)
                            }

                            override fun setSelected(event: AnActionEvent, state: Boolean) {
                                val project = event.project ?: return

                                endpointsModel.setHttpTypeFilterActive(httpType, state)

                                project.messageBus.syncPublisher(EndpointToolModelEventListener.TOPIC)
                                    .handle(ModelEvent(EventType.UPDATE_FILTERS, disposable))
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
                                return endpointsModel.isEndpointTypeFilterActive(endpointType)
                            }

                            override fun setSelected(event: AnActionEvent, state: Boolean) {
                                val project = event.project ?: return

                                endpointsModel.setEndpointTypeFilterActive(endpointType, state)

                                project.messageBus.syncPublisher(EndpointToolModelEventListener.TOPIC)
                                    .handle(ModelEvent(EventType.UPDATE_FILTERS, disposable))
                            }

                        }
                    }
            }

            override fun getCurrentText() = ""
        })

    }

    fun setup() {
    }

    fun start() {
        project.messageBus.syncPublisher(EndpointToolModelEventListener.TOPIC)
            .handle(ModelEvent(EventType.INIT, this))
    }

    override fun dispose() {
    }

    private fun onDocumentChange() {
        endpointsModel.setTextFilterValue(searchTextField.text)

        project.messageBus.syncPublisher(EndpointToolModelEventListener.TOPIC)
            .handle(ModelEvent(EventType.UPDATE_FILTERS, this))
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

}