/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp.beans

import com.explyt.spring.core.service.beans.BeanAnnotationEvidence
import com.explyt.spring.core.service.beans.BeanDetailsReader
import com.explyt.spring.core.service.beans.BeanLookupSelector
import com.explyt.spring.core.service.beans.BeanModelSource
import com.explyt.spring.core.service.beans.BeanSelection
import com.explyt.spring.core.service.beans.ScopedBeanRecord
import com.explyt.spring.core.service.beans.ScopedBeanSnapshot
import com.explyt.spring.core.service.beans.SpringInjectionPoint
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMember
import com.intellij.psi.util.PsiUtilCore

/**
 * Turns one answer into the fixed JSON projection the tool promises.
 *
 * Records are ordered once, before anything is projected, so a continuation describes the same sequence the
 * first page came from; `candidateAt` then builds a single detached node per requested index. Declaration
 * details are read only when the caller asked for them - enumerating a context would otherwise pay for the
 * annotations of every bean to answer a question about one.
 *
 * Must run under the read action that produced [ScopedBeanSnapshot]: the projection touches PSI.
 */
class BeanResponseMapper(private val project: Project) {

    fun map(
        snapshot: ScopedBeanSnapshot,
        selection: BeanSelection,
        lookup: BeanLookupSelector?,
        injection: SpringInjectionPoint?,
        includeDetails: Boolean
    ): BeanResponseContent {
        val ordered = selection.match.records.sortedWith(CANDIDATE_ORDER)
        return BeanResponseContent(
            mode = if (injection != null) MODE_INJECTION else MODE_LOOKUP,
            model = model(snapshot, selection),
            outcome = selection.outcome.name,
            matchCompleteness = selection.match.completeness.name,
            unresolvedCount = selection.match.unresolvedCount,
            totalCount = ordered.size,
            candidateAt = { index -> candidate(ordered[index], lookup, includeDetails) },
            injection = injection?.let(::injection)
        )
    }

    private fun model(snapshot: ScopedBeanSnapshot, selection: BeanSelection): ObjectNode {
        val node = mapper.createObjectNode()
        node.put("source", snapshot.selection.source.name)
        node.put(
            "precision",
            if (snapshot.selection.source == BeanModelSource.NATIVE_SNAPSHOT) SELECTED_SNAPSHOT else MODULE_ESTIMATE
        )
        node.put("application", snapshot.application.className)
        node.put("module", snapshot.application.moduleName)
        snapshot.selection.nativeContext?.let { node.put("contextId", it.id) }

        val limitations = node.putArray("limitations")
        (snapshot.limitations + snapshot.selection.limitations + selection.match.limitations)
            .sorted()
            .forEach { limitations.add(it) }
        return node
    }

    private fun candidate(record: ScopedBeanRecord, lookup: BeanLookupSelector?, includeDetails: Boolean): ObjectNode {
        val node = mapper.createObjectNode()
        node.put("id", record.id)
        node.put("name", record.name)
        node.put("type", record.typeName)
        node.put("kind", record.kind.name)
        matchedName(record, lookup)?.let { node.put("matchedName", it) }
        declaration(record.declaration)?.let { node.set<ObjectNode>("declaration", it) }
        if (includeDetails) node.set<ObjectNode>("details", details(record))
        return node
    }

    /** Names the alias a query matched, so a caller who asked by one name recognises the canonical answer. */
    private fun matchedName(record: ScopedBeanRecord, lookup: BeanLookupSelector?): String? =
        lookup?.beanName?.takeIf { it != record.name && it in record.knownNames }

    private fun declaration(member: PsiMember?): ObjectNode? {
        val anchor = member?.takeIf { it.isValid }?.sourceAnchor() ?: return null
        val file = anchor.containingFile?.virtualFile ?: return null
        val node = mapper.createObjectNode()

        val basePath = project.basePath?.let { "$it/" }
        if (basePath != null && file.path.startsWith(basePath)) {
            node.put("filePath", file.path.removePrefix(basePath))
        } else {
            node.put("sourceUrl", file.url)
        }
        lineOf(anchor)?.let { node.put("line", it) }
        return node
    }

    /**
     * A physical element to point at, or nothing.
     *
     * A light or synthetic member - a Kotlin `copy()`, a generated accessor - has no text range, and reading a
     * position from it fails; such a bean keeps its record and simply carries no declaration.
     */
    private fun PsiElement.sourceAnchor(): PsiElement? =
        takeIf { it.textRange != null && it.containingFile != null }
            ?: navigationElement?.takeIf { it.textRange != null && it.containingFile != null }

    private fun lineOf(anchor: PsiElement): Int? {
        val file = anchor.containingFile ?: return null
        val document = PsiUtilCore.getVirtualFile(file)
            ?.let { com.intellij.psi.PsiDocumentManager.getInstance(project).getDocument(file) }
            ?: return null
        val offset = anchor.textRange?.startOffset ?: return null
        return if (offset <= document.textLength) document.getLineNumber(offset) + 1 else null
    }

    /**
     * Declaration evidence, read on demand.
     *
     * A known-absent list stays as `[]` and a known `primary=false` stays `false`, while an unknown field is
     * omitted: collapsing the two would report "the model did not look" as "the declaration says no".
     */
    private fun details(record: ScopedBeanRecord): ObjectNode {
        val evidence = BeanDetailsReader.read(record)
        val node = mapper.createObjectNode()
        evidence.aliases?.let { aliases -> node.putArray("aliases").apply { aliases.sorted().forEach(::add) } }
        evidence.qualifiers?.let { node.set<ObjectNode>("qualifiers", annotations(it)) }
        evidence.primary?.let { node.put("primary", it) }
        evidence.profiles?.let { profiles -> node.putArray("profiles").apply { profiles.sorted().forEach(::add) } }
        evidence.conditions?.let { node.set<ObjectNode>("conditions", annotations(it)) }
        evidence.module?.let { node.put("module", it) }
        return node
    }

    private fun annotations(evidence: List<BeanAnnotationEvidence>) =
        mapper.createArrayNode().apply {
            evidence.sortedBy { it.annotationClass }.forEach { annotation ->
                val node = addObject()
                node.put("annotation", annotation.annotationClass)
                val attributes = node.putObject("attributes")
                annotation.attributes.toSortedMap().forEach { (key, value) -> attributes.put(key, value) }
                if (annotation.unknownAttributes.isNotEmpty()) {
                    node.putArray("unknownAttributes").apply { annotation.unknownAttributes.sorted().forEach(::add) }
                }
            }
        }

    private fun injection(point: SpringInjectionPoint): ObjectNode {
        val node = mapper.createObjectNode()
        node.put("name", point.name)
        node.put("type", (point.beanType ?: point.declaredType).canonicalText)
        node.put("shape", point.facts.shape.name)
        node.put("required", point.facts.required)
        node.put("hasDefaultValue", point.facts.hasDefaultValue)
        node.put("basis", point.facts.basis)
        if (point.facts.limitations.isNotEmpty()) {
            node.putArray("limitations").apply { point.facts.limitations.sorted().forEach(::add) }
        }
        return node
    }

    private val mapper = ObjectMapper()

    companion object {
        private const val MODE_LOOKUP = "LOOKUP"
        private const val MODE_INJECTION = "INJECTION"
        private const val MODULE_ESTIMATE = "MODULE_ESTIMATE"
        private const val SELECTED_SNAPSHOT = "SELECTED_SNAPSHOT"

        /**
         * Total order over candidates, so page N+1 continues page N rather than re-shuffling the same records.
         * Nulls sort first explicitly: a bean whose type the model never resolved still has a fixed place.
         */
        private val CANDIDATE_ORDER: Comparator<ScopedBeanRecord> =
            compareBy<ScopedBeanRecord> { it.name }
                .thenBy(nullsFirst()) { it.typeName }
                .thenBy(nullsFirst()) { it.declarationModule }
                .thenBy { it.id }
    }
}
