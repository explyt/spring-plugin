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

    internal val contractTree = detailsTree()
    internal val callChainTree = detailsTree()

    private val callChainPane = JBScrollPane(callChainTree)

    private val contractTab = DetailsTab(contractTree, message("explyt.web.endpoints.details.empty")) {
        EndpointDetailsReader.detailsOf(it, project)
    }
    private val callChainTab = DetailsTab(callChainTree, message("explyt.web.endpoints.details.chain.none")) {
        EndpointDetailsReader.callChainOf(it, project)
    }

    internal var shownRow: EndpointElementViewData? = null
        private set

    private var reportedEndpoint: EndpointRowKey? = null

    init {
        Disposer.register(parentDisposable, this)
        addTab(message("explyt.web.endpoints.details.tab.contract"), JBScrollPane(contractTree))
        addTab(message("explyt.web.endpoints.details.tab.call.chain"), callChainPane)
        installNavigation(contractTree)
        installNavigation(callChainTree)
        addChangeListener { selectedTab().show(shownRow) }
    }

    fun show(row: EndpointElementViewData?) {
        if (row === shownRow) return
        shownRow = row
        selectedTab().show(row)
    }

    private fun selectedTab(): DetailsTab = if (selectedComponent === callChainPane) callChainTab else contractTab

    private inner class DetailsTab(
        private val tree: Tree,
        private val noRowsText: String,
        private val compute: (EndpointElementViewData) -> EndpointDetails,
    ) {
        private var computedRow: EndpointElementViewData? = null

        fun show(row: EndpointElementViewData?) {
            if (row === computedRow) return
            computedRow = row
            if (row == null) {
                render(tree, emptyList(), message("explyt.web.endpoints.details.empty"))
                return
            }
            render(tree, emptyList(), message("explyt.web.endpoints.details.loading"))
            ReadAction.nonBlocking(Callable { compute(row) })
                .inSmartMode(project)
                .coalesceBy(this)
                .expireWith(this@EndpointDetailsPanel)
                .finishOnUiThread(ModalityState.any()) { details -> if (row === computedRow) present(row, details) }
                .submit(AppExecutorUtil.getAppExecutorService())
                .onError { error -> showFailure(row, error) }
        }

        private fun present(row: EndpointElementViewData, details: EndpointDetails) = when (details) {
            is EndpointDetails.Contract -> {
                render(tree, details.rows, noRowsText)
                reportShown(row.key)
            }
            is EndpointDetails.NoHttpContract ->
                render(tree, emptyList(), message("explyt.web.endpoints.details.no.contract", details.endpointType))
            EndpointDetails.NotFound -> render(tree, emptyList(), message("explyt.web.endpoints.details.not.found"))
        }

        private fun showFailure(row: EndpointElementViewData, error: Throwable) {
            if (error is ControlFlowException || error is CancellationException) return
            val reason = error.message ?: error.javaClass.name
            ApplicationManager.getApplication().invokeLater(
                { if (row === computedRow) render(tree, emptyList(), message("explyt.web.endpoints.details.failed", reason)) },
                ModalityState.any(),
                Condition<Any> { Disposer.isDisposed(this@EndpointDetailsPanel) },
            )
        }
    }

    private fun reportShown(endpoint: EndpointRowKey) {
        if (endpoint == reportedEndpoint) return
        reportedEndpoint = endpoint
        StatisticService.getInstance().addActionUsage(ENDPOINTS_TOOLWINDOW_DETAILS_SHOWN)
    }

    private fun render(tree: Tree, rows: List<ContractRow>, emptyText: String) {
        val root = DefaultMutableTreeNode()
        rows.forEach { root.add(nodeOf(it)) }
        tree.emptyText.text = emptyText
        tree.model = DefaultTreeModel(root)
        TreeUtil.expandAll(tree)
    }

    private fun nodeOf(row: ContractRow): DefaultMutableTreeNode =
        DefaultMutableTreeNode(row).apply { row.children.forEach { add(nodeOf(it)) } }

    private fun installNavigation(tree: Tree) {
        tree.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                if (e.keyCode == KeyEvent.VK_ENTER) navigate(tree.selectionPath)
            }
        })
        object : DoubleClickListener() {
            override fun onDoubleClick(event: MouseEvent): Boolean {
                navigate(tree.getClosestPathForLocation(event.x, event.y))
                return true
            }
        }.installOn(tree)
    }

    private fun navigate(path: TreePath?) {
        val target = (TreeUtil.getLastUserObject(path) as? ContractRow)?.target ?: return
        StatisticService.getInstance().addActionUsage(ENDPOINTS_TOOLWINDOW_DETAILS_NAVIGATE)
        ApplicationManager.getApplication().invokeLater {
            (target.element as? Navigatable)?.takeIf { it.canNavigate() }?.navigate(true)
        }
    }

    override fun dispose() = Unit

    private companion object {
        fun detailsTree() = Tree(DefaultTreeModel(DefaultMutableTreeNode())).apply {
            isRootVisible = false
            showsRootHandles = true
            cellRenderer = ContractRowRenderer()
            emptyText.text = message("explyt.web.endpoints.details.empty")
        }
    }
}
