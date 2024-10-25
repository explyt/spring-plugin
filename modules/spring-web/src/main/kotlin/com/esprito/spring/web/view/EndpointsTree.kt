package com.esprito.spring.web.view

import com.esprito.spring.web.loader.EndpointElement
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.pom.Navigatable
import com.intellij.ui.DoubleClickListener
import com.intellij.ui.treeStructure.treetable.TreeTable
import com.intellij.util.ui.tree.TreeUtil
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import javax.swing.tree.TreePath

class EndpointsTree(val project: Project, model: EndpointsTreeModel) : TreeTable(model) {

    init {
        addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                if (e.keyCode != KeyEvent.VK_ENTER) return
                if (tree.selectionCount == 0) return

                tree.selectionPath?.let {
                    navigateToEndpoint(it)
                }
            }
        })

        object : DoubleClickListener() {
            override fun onDoubleClick(event: MouseEvent): Boolean {
                navigateToEndpoint(tree.getClosestPathForLocation(event.x, event.y))
                return false
            }
        }.installOn(this)
    }

    private fun navigateToEndpoint(nodePath: TreePath?) {
        ToolWindowManager.getInstance(project).invokeLater {
            (TreeUtil.getLastUserObject(EndpointElement::class.java, nodePath)
                ?.psiElement?.navigationElement as? Navigatable)?.navigate(true)
        }
    }

}