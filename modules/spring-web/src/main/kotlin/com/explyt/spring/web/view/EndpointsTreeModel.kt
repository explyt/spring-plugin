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

import com.explyt.spring.web.SpringWebClasses
import com.explyt.spring.web.loader.EndpointElement
import com.explyt.spring.web.loader.EndpointType
import com.explyt.spring.web.service.SpringWebEndpointsSearcher
import com.explyt.spring.web.view.EndpointToolModelEventListener.EventType
import com.explyt.util.ExplytKotlinUtil.mapToList
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.project.Project
import com.intellij.ui.treeStructure.treetable.TreeTableModel
import com.intellij.util.concurrency.AppExecutorUtil
import org.jetbrains.kotlin.utils.keysToMap
import org.jetbrains.kotlin.utils.keysToMapExceptNulls
import javax.swing.JTree
import javax.swing.event.EventListenerList
import javax.swing.event.TreeModelEvent
import javax.swing.event.TreeModelListener
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreeNode
import javax.swing.tree.TreePath

class EndpointsTreeModel(private val project: Project) : TreeTableModel, EndpointToolModelEventListener {

    private val endpointsSearcher = SpringWebEndpointsSearcher.getInstance(project)
    private val propertiesComponent = PropertiesComponent.getInstance(project)

    private val rootNode = DefaultMutableTreeNode()
    private val listenerList: EventListenerList = EventListenerList()

    private var branchTypes = listOf<EndpointType>()
    private var branchEndpoints = mapOf<EndpointType, List<DefaultMutableTreeNode>>()
    private var branchNodes = mapOf<EndpointType, DefaultMutableTreeNode>()

    private var textFilter = ""
    private var httpMethodsFilter = setOf<String>()
    private var endpointTypesFilter = setOf<EndpointType>()

    override fun getRoot() =
        rootNode

    override fun getChild(parent: Any?, index: Int): Any? {
        return (parent as? TreeNode)?.getChildAt(index)
    }

    override fun getChildCount(parent: Any?): Int {
        return (parent as? TreeNode)?.childCount ?: 0
    }

    override fun isLeaf(node: Any?): Boolean {
        return (node as? TreeNode)?.isLeaf ?: true
    }

    override fun valueForPathChanged(path: TreePath?, newValue: Any) {}

    override fun getIndexOfChild(parent: Any?, child: Any?): Int {
        val parentNode = parent as? TreeNode ?: return -1
        val childNode = child as? TreeNode ?: return -1

        return parentNode.getIndex(childNode)
    }

    override fun addTreeModelListener(listener: TreeModelListener) {
        listenerList.add(TreeModelListener::class.java, listener)
    }

    override fun removeTreeModelListener(listener: TreeModelListener?) {
        listenerList.remove(TreeModelListener::class.java, listener)
    }

    override fun getColumnCount(): Int = 1

    override fun getColumnName(column: Int): String {
        return when (column) {
            0 -> "Path"
            else -> wrongColumn(column)
        }
    }

    override fun getColumnClass(column: Int): Class<*> {
        return when (column) {
            0 -> TreeTableModel::class.java
            else -> wrongColumn(column)
        }
    }

    override fun getValueAt(node: Any, column: Int): Any {
        val value = node as? DefaultMutableTreeNode ?: return ""
        val element = value.userObject as? EndpointElement ?: return ""
        return if (column == 0) element.path else ""
    }

    override fun isCellEditable(node: Any, column: Int) =
        false

    override fun setValueAt(value: Any, node: Any, column: Int) {}
    override fun setTree(tree: JTree) {}

    override fun handle(event: EndpointToolModelEventListener.ModelEvent) {
        when (event.type) {
            EventType.INIT -> callNonblocking(event.disposable, true) { initModel() }
            EventType.UPDATE_DATA -> callNonblocking(event.disposable, true) { updateData() }
            EventType.UPDATE_FILTERS -> callNonblocking(
                event.disposable,
                !rootNode.children().hasMoreElements()
            ) { updateFilters() }
        }
    }

    private fun callNonblocking(disposable: Disposable, reloadTree: Boolean, action: () -> Any) {
        ReadAction.nonBlocking<Any> {
            action()
        }
            .inSmartMode(project)
            .finishOnUiThread(ModalityState.any()) {
                applyFilters(reloadTree)
            }
            .expireWith(disposable)
            .submit(AppExecutorUtil.getAppExecutorService())
    }

    private fun wrongColumn(column: Int): Nothing {
        throw RuntimeException("Invalid column $column")
    }

    private fun initModel() {
        updateData()
        updateFilters()
    }

    private fun updateData() {
        branchTypes = endpointsSearcher.getLoadersTypes().toList()
        branchNodes = branchTypes.keysToMap {
            DefaultMutableTreeNode(it.readable)
        }

        val allEndpoints = endpointsSearcher.getAllEndpoints().asSequence()

        branchEndpoints = branchTypes.keysToMapExceptNulls { type ->
            branchNodes[type]?.let {
                allEndpoints
                    .filter { it.type == type }
                    .mapToList { DefaultMutableTreeNode(it) }
            }
        }
    }


    private fun updateFilters() {
        textFilter = getTextFilterValue()

        httpMethodsFilter = SpringWebClasses.URI_TYPE
            .filterTo(mutableSetOf()) { httpType ->
                isHttpTypeFilterActive(httpType)
            }

        endpointTypesFilter = SpringWebEndpointsSearcher.getInstance(project)
            .getLoadersTypes()
            .filterTo(mutableSetOf()) { endpointType ->
                isEndpointTypeFilterActive(endpointType)
            }
    }

    private fun applyFilters(reloadTree: Boolean) {
        val oldBranches = rootNode.children().toList()
        rootNode.removeAllChildren()

        for (branchType in branchTypes) {
            if (!endpointTypesFilter.contains(branchType)) continue
            if (branchEndpoints[branchType]?.isEmpty() == true) continue
            val branchNode = branchNodes[branchType] ?: continue
            rootNode.add(branchNode)
        }
        if (!reloadTree) {
            publishNodeChanges(rootNode, oldBranches)
        }

        for (branchType in branchTypes) {
            if (!endpointTypesFilter.contains(branchType)) continue
            val branchNode = branchNodes[branchType] ?: continue
            val endpoints = branchEndpoints[branchType] ?: continue

            val oldChildren = branchNode.children().toList()
            branchNode.removeAllChildren()

            for (endpointNode in endpoints) {
                val endpointElement = endpointNode.userObject as? EndpointElement ?: continue
                if (textFilter.isNotBlank() && !endpointElement.path.contains(textFilter)) continue
                if (httpMethodsFilter
                        .intersect(endpointElement.requestMethods.toSet())
                        .isNotEmpty()
                ) {
                    branchNode.add(endpointNode)
                }
            }
            if (!reloadTree) {
                publishNodeChanges(branchNode, oldChildren)
            }
        }
        if (reloadTree) {
            publishTreeStructureChanged()
        }
    }

    private fun publishTreeStructureChanged() {
        val event = TreeModelEvent(
            this,
            rootNode.path,
            null,
            null
        )
        for (listener in listenerList.getListeners(TreeModelListener::class.java)) {
            listener.treeStructureChanged(event)
        }
    }

    private fun publishNodeChanges(node: DefaultMutableTreeNode, oldChildren: List<TreeNode>) {
        val newChildren = node.children().toList()
        val newChildrenSet = newChildren.toSet()

        val removedElementsIndices = mutableListOf<Int>()
        val removedElements = mutableListOf<TreeNode>()
        val oldChildrenWithoutRemoved = mutableSetOf<TreeNode>()

        for (i in oldChildren.indices) {
            val child = oldChildren[i]
            if (newChildrenSet.contains(child)) {
                oldChildrenWithoutRemoved.add(child)
            } else {
                removedElementsIndices.add(i)
                removedElements.add(child)
            }
        }

        if (removedElements.isNotEmpty()) {
            val event = TreeModelEvent(
                this,
                node.path,
                removedElementsIndices.toIntArray(),
                removedElements.toTypedArray()
            )
            for (listener in listenerList.getListeners(TreeModelListener::class.java)) {
                listener.treeNodesRemoved(event)
            }
        }
        if (oldChildrenWithoutRemoved.size != newChildren.size) {
            val addedElementsIndices = mutableListOf<Int>()
            val addedElements = mutableListOf<TreeNode>()

            for (i in newChildren.indices) {
                val child = newChildren[i]
                if (!oldChildrenWithoutRemoved.contains(child)) {
                    addedElementsIndices.add(i)
                    addedElements.add(child)
                }
            }

            val event = TreeModelEvent(
                this,
                node.path,
                addedElementsIndices.toIntArray(),
                addedElements.toTypedArray()
            )
            for (listener in listenerList.getListeners(TreeModelListener::class.java)) {
                listener.treeNodesInserted(event)
            }
        }

    }

    fun getTextFilterValue(): String {
        return propertiesComponent.getValue(ENDPOINT_PATH_FILTER_NAME, "")
    }

    fun setTextFilterValue(value: String) {
        propertiesComponent.setValue(ENDPOINT_PATH_FILTER_NAME, value)
    }

    fun isHttpTypeFilterActive(httpType: String): Boolean {
        return propertiesComponent
            .getBoolean(httpTypeFilterName(httpType), true)
    }

    fun setHttpTypeFilterActive(httpType: String, isActive: Boolean) {
        propertiesComponent
            .setValue(httpTypeFilterName(httpType), isActive, true)
    }

    fun isEndpointTypeFilterActive(endpointType: EndpointType): Boolean {
        return propertiesComponent
            .getBoolean(endpointTypeFilterName(endpointType), true)
    }

    fun setEndpointTypeFilterActive(endpointType: EndpointType, isActive: Boolean) {
        propertiesComponent
            .setValue(endpointTypeFilterName(endpointType), isActive, true)
    }

    companion object {
        private const val ENDPOINT_PATH_FILTER_NAME = "ExplytEndpointPathFilter"

        private fun httpTypeFilterName(httpType: String): String {
            return "ExplytHttpTypeFilter$httpType"
        }

        private fun endpointTypeFilterName(endpointType: EndpointType): String {
            return "ExplytEndpointTypeFilter$endpointType"
        }
    }

}