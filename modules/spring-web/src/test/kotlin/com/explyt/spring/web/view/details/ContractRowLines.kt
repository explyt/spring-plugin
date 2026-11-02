/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.view.details

import com.explyt.spring.web.service.SpringWebEndpointsSearcher
import com.explyt.spring.web.view.EndpointElementViewData
import com.explyt.spring.web.view.EndpointsTreeData
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode

object ContractRowLines {

    fun lines(rows: List<ContractRow>, depth: Int = 0): List<String> = rows.flatMap { row ->
        listOf("  ".repeat(depth) + row.text + (row.note?.let { "  $it" } ?: "")) + lines(row.children, depth + 1)
    }

    fun lines(tree: JTree): List<String> {
        val root = tree.model.root as DefaultMutableTreeNode
        return lines(root.children().toList().map { (it as DefaultMutableTreeNode).userObject as ContractRow })
    }

    fun rowOf(project: Project, path: String, method: String): EndpointElementViewData =
        SpringWebEndpointsSearcher.getInstance(project).getAllEndpoints()
            .filter { it.path == path }
            .flatMap(EndpointsTreeData::rowsOf)
            .single { it.method == method }

    fun contractRowsOf(project: Project, row: EndpointElementViewData): List<ContractRow> =
        (EndpointDetailsReader.detailsOf(row, project) as EndpointDetails.Contract).rows

    fun find(rows: List<ContractRow>, text: String): ContractRow =
        rows.asSequence().flatMap { sequenceOf(it) + it.children.asSequence().flatMap { child -> walk(child) } }
            .first { it.text == text }

    fun targetOf(rows: List<ContractRow>, text: String): PsiElement? = find(rows, text).target?.element

    private fun walk(row: ContractRow): Sequence<ContractRow> =
        sequenceOf(row) + row.children.asSequence().flatMap(::walk)
}
