/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.view.details

import com.explyt.spring.core.statistic.StatisticActionId.ENDPOINTS_TOOLWINDOW_DETAILS_NAVIGATE
import com.explyt.spring.core.statistic.StatisticActionId.ENDPOINTS_TOOLWINDOW_DETAILS_SHOWN
import com.explyt.spring.core.statistic.StatisticService
import com.explyt.spring.web.SpringWebBundle.message
import com.explyt.spring.web.view.EndpointElementViewData
import com.explyt.spring.web.view.EndpointRowKey
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.diagnostic.ControlFlowException
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Condition
import com.intellij.openapi.util.Disposer
import com.intellij.pom.Navigatable
import com.intellij.ui.DoubleClickListener
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.concurrency.AppExecutorUtil
import com.intellij.util.ui.tree.TreeUtil
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import java.util.concurrent.Callable
import java.util.concurrent.CancellationException
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath

class EndpointDetailsPanel(private val project: Project, parentDisposable: Disposable) : JBTabbedPane(), Disposable {

    internal val contractTree = Tree(DefaultTreeModel(DefaultMutableTreeNode()))

    internal var shownRow: EndpointElementViewData? = null
        private set

    private var reportedEndpoint: EndpointRowKey? = null

    init {
        Disposer.register(parentDisposable, this)
        contractTree.isRootVisible = false
        contractTree.showsRootHandles = true
        contractTree.cellRenderer = ContractRowRenderer()
        contractTree.emptyText.text = message("explyt.web.endpoints.details.empty")
        addTab(message("explyt.web.endpoints.details.tab.contract"), JBScrollPane(contractTree))
        installNavigation()
    }

    fun show(row: EndpointElementViewData?) {
        if (row === shownRow) return
        shownRow = row
        if (row == null) {
            render(emptyList(), message("explyt.web.endpoints.details.empty"))
            return
        }
        render(emptyList(), message("explyt.web.endpoints.details.loading"))
        ReadAction.nonBlocking(Callable { EndpointDetailsReader.detailsOf(row, project) })
            .inSmartMode(project)
            .coalesceBy(this)
            .expireWith(this)
            .finishOnUiThread(ModalityState.any()) { details -> if (row === shownRow) present(row, details) }
            .submit(AppExecutorUtil.getAppExecutorService())
            .onError { error -> showFailure(row, error) }
    }

    private fun present(row: EndpointElementViewData, details: EndpointDetails) = when (details) {
        is EndpointDetails.Contract -> {
            render(details.rows, message("explyt.web.endpoints.details.empty"))
            reportShown(row.key)
        }
        is EndpointDetails.NoHttpContract ->
            render(emptyList(), message("explyt.web.endpoints.details.no.contract", details.endpointType))
        EndpointDetails.NotFound -> render(emptyList(), message("explyt.web.endpoints.details.not.found"))
    }

    private fun reportShown(endpoint: EndpointRowKey) {
        if (endpoint == reportedEndpoint) return
        reportedEndpoint = endpoint
        StatisticService.getInstance().addActionUsage(ENDPOINTS_TOOLWINDOW_DETAILS_SHOWN)
    }

    private fun showFailure(row: EndpointElementViewData, error: Throwable) {
        if (error is ControlFlowException || error is CancellationException) return
        val reason = error.message ?: error.javaClass.name
        ApplicationManager.getApplication().invokeLater(
            { if (row === shownRow) render(emptyList(), message("explyt.web.endpoints.details.failed", reason)) },
            ModalityState.any(),
            Condition<Any> { Disposer.isDisposed(this) },
        )
    }

    private fun render(rows: List<ContractRow>, emptyText: String) {
        val root = DefaultMutableTreeNode()
        rows.forEach { root.add(nodeOf(it)) }
        contractTree.emptyText.text = emptyText
        contractTree.model = DefaultTreeModel(root)
        TreeUtil.expandAll(contractTree)
    }

    private fun nodeOf(row: ContractRow): DefaultMutableTreeNode =
        DefaultMutableTreeNode(row).apply { row.children.forEach { add(nodeOf(it)) } }

    private fun installNavigation() {
        contractTree.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                if (e.keyCode == KeyEvent.VK_ENTER) navigate(contractTree.selectionPath)
            }
        })
        object : DoubleClickListener() {
            override fun onDoubleClick(event: MouseEvent): Boolean {
                navigate(contractTree.getClosestPathForLocation(event.x, event.y))
                return true
            }
        }.installOn(contractTree)
    }

    private fun navigate(path: TreePath?) {
        val target = (TreeUtil.getLastUserObject(path) as? ContractRow)?.target ?: return
        StatisticService.getInstance().addActionUsage(ENDPOINTS_TOOLWINDOW_DETAILS_NAVIGATE)
        ApplicationManager.getApplication().invokeLater {
            (target.element as? Navigatable)?.takeIf { it.canNavigate() }?.navigate(true)
        }
    }

    override fun dispose() = Unit
}
