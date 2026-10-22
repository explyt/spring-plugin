/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp

import com.explyt.spring.core.tracker.ModificationTrackerManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.psi.util.PsiModificationTracker

/**
 * The last few projected answers, so walking the pages of one answer projects each record once.
 *
 * A page call needs the whole answer for its digest; without this cache a walk over n records in p pages projects
 * p × n of them. An entry is valid only for the PSI and Spring model state it was projected from: any PSI write,
 * dumb-mode switch or model change starts a fresh projection, whose digest stays equal when the answer did not
 * change. Nothing depends on this cache, so it never invalidates anything else.
 */
@Service(Service.Level.PROJECT)
class WholeAnswerCache(private val project: Project) : Disposable {

    private data class Key(val query: Map<String, String?>, val psiStamp: Long, val modelStamp: Long)

    private val answers = object : LinkedHashMap<Key, WholeAnswer>(CAPACITY, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Key, WholeAnswer>) = size > CAPACITY
    }

    fun answerFor(
        query: Map<String, String?>,
        freshCount: Int? = null,
        projection: () -> WholeAnswer,
    ): WholeAnswer {
        val key = Key(
            query.filterKeys { it !in PAGING_KEYS },
            PsiModificationTracker.getInstance(this.project).modificationCount,
            ModificationTrackerManager.getInstance(this.project).getModelRevisionTracker().modificationCount,
        )
        synchronized(answers) { answers[key] }
            ?.takeIf { freshCount == null || it.totalCount == freshCount }
            ?.let { return it }
        val answer = projection()
        synchronized(answers) { answers[key] = answer }
        return answer
    }

    override fun dispose() {
        synchronized(answers) { answers.clear() }
    }

    companion object {
        private const val CAPACITY = 4
        private val PAGING_KEYS = setOf("offset", "limit", "maxChars", "expectedRevision")

        fun getInstance(project: Project): WholeAnswerCache = project.service()
    }
}
