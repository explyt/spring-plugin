package com.esprito.spring.web.view

import com.esprito.spring.web.loader.EndpointElement
import com.intellij.ide.util.treeView.NodeRenderer
import com.intellij.ui.SimpleTextAttributes
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode

class EndpointCellRenderer : NodeRenderer() {
    override fun customizeCellRenderer(
        tree: JTree, value: Any?, selected: Boolean, expanded: Boolean, leaf: Boolean, row: Int, hasFocus: Boolean
    ) {

        val endpointElement = (value as? DefaultMutableTreeNode)?.userObject as? EndpointElement

        if (endpointElement == null) {
            super.customizeCellRenderer(tree, value, selected, expanded, leaf, row, hasFocus)
        } else {
            append(endpointElement.requestMethods.toString(), SimpleTextAttributes.GRAY_ATTRIBUTES)
            append(endpointElement.path, SimpleTextAttributes.SIMPLE_CELL_ATTRIBUTES, true)
        }
    }

}
