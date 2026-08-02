/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.search

import com.explyt.spring.core.util.SpringCoreUtil
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.util.concurrency.AppExecutorUtil
import java.util.concurrent.Callable
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Caches whether the project is a Spring Boot one, so that Search Everywhere can decide on the EDT whether to show
 * the Beans tab.
 *
 * Search Everywhere queries contributor availability on the EDT, while the underlying detection resolves a library
 * class and therefore needs indices and a read action (see #233). The value is thus computed in the background and
 * only ever read from a volatile field on the EDT; it is recomputed when project roots change.
 */
@Service(Service.Level.PROJECT)
class BeanSearchAvailabilityService(private val project: Project) {

    @Volatile
    private var cached: Availability? = null
    private val updateScheduled = AtomicBoolean(false)

    /**
     * Returns the last known answer without touching indices, scheduling a background refresh when the cached value
     * is missing or outdated.
     *
     * Until the first computation finishes the tab stays visible: hiding it from a real Spring Boot project is a
     * worse failure than showing an empty tab once in a non-Spring project.
     */
    fun isSpringBootProject(): Boolean {
        val snapshot = cached
        val modificationCount = ProjectRootManager.getInstance(project).modificationCount
        if (snapshot == null || snapshot.modificationCount != modificationCount) {
            scheduleUpdate(modificationCount)
        }
        return snapshot?.isSpringBootProject ?: true
    }

    private fun scheduleUpdate(modificationCount: Long) {
        if (!updateScheduled.compareAndSet(false, true)) return

        ReadAction.nonBlocking(Callable { SpringCoreUtil.isSpringBootProject(project) })
            .expireWith(SpringCoreUtil.getDisposable(project))
            .inSmartMode(project)
            .finishOnUiThread({ com.intellij.openapi.application.ModalityState.any() }) { isSpringBoot ->
                cached = Availability(isSpringBoot, modificationCount)
                updateScheduled.set(false)
            }
            .submit(AppExecutorUtil.getAppScheduledExecutorService())
    }

    private data class Availability(val isSpringBootProject: Boolean, val modificationCount: Long)

    companion object {
        fun getInstance(project: Project): BeanSearchAvailabilityService = project.service()
    }
}
