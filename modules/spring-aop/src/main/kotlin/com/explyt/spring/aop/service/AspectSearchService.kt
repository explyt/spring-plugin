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

package com.explyt.spring.aop.service

import com.explyt.spring.core.externalsystem.model.SpringAspectData
import com.explyt.spring.core.service.NativeSearchService
import com.explyt.spring.core.tracker.ModificationTrackerManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.jetbrains.kotlin.idea.base.externalSystem.findAll


@Service(Service.Level.PROJECT)
class AspectSearchService(private val project: Project) {

    fun getAspectQualifiedClasses(): Set<String> {
        return CachedValuesManager.getManager(project).getCachedValue(project) {
            CachedValueProvider.Result(
                getAspectsData().mapTo(mutableSetOf()) { it.aspectQualifiedClassName },
                ModificationTrackerManager.getInstance(project).getExternalSystemTracker()
            )
        }
    }

    fun getPointCutQualifiedClasses(): Set<String> {
        return CachedValuesManager.getManager(project).getCachedValue(project) {
            CachedValueProvider.Result(
                getAspectsData().mapTo(mutableSetOf()) { it.beanQualifiedClassName },
                ModificationTrackerManager.getInstance(project).getExternalSystemTracker()
            )
        }
    }


    fun getAspectsData(): List<SpringAspectData> {
        return NativeSearchService.getInstance(project).getActiveProjectsNode()
            .flatMap { it.findAll(SpringAspectData.KEY).map { it.data } }
    }

    companion object {
        fun getInstance(project: Project): AspectSearchService = project.service()
    }
}