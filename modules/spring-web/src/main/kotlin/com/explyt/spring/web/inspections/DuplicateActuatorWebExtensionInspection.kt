/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.inspections

import com.explyt.spring.core.completion.properties.DefinedConfigurationPropertiesSearch
import com.explyt.spring.core.properties.references.ActuatorEndpoint
import com.explyt.spring.core.properties.references.ActuatorEndpointKeys
import com.explyt.spring.core.service.conditional.ConditionVerdict
import com.explyt.spring.web.SpringWebBundle
import com.explyt.spring.web.loader.ActuatorWebExtension
import com.explyt.spring.web.loader.ActuatorWebExtensions
import com.explyt.spring.web.loader.EndpointExposure
import com.explyt.spring.web.util.ActuatorExposure
import com.explyt.spring.web.util.WebApplicationStack
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import org.jetbrains.uast.UClass

class DuplicateActuatorWebExtensionInspection : SpringWebBaseUastLocalInspectionTool() {

    override fun checkClass(
        aClass: UClass,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<ProblemDescriptor>? {
        val psiClass = aClass.javaPsi
        val module = ModuleUtilCore.findModuleForPsiElement(psiClass) ?: return null
        if (!ActuatorWebExtensions.isDeclaredOn(aClass, module)) return null
        if (WebApplicationStack.of(module) == null) return null

        val exposure by lazy { exposureRule(module) }
        return endpointsOf(module).mapNotNull { endpoint ->
            val extensions = ActuatorWebExtensions.appliedTo(endpoint.psiClass, module)
            if (extensions.size < 2) return@mapNotNull null
            val carried = extensions.firstOrNull { psiClass.isEquivalentTo(it.carrier) } ?: return@mapNotNull null
            val anchor = aClass.uAnnotations.firstOrNull { it.qualifiedName == carried.annotationName }?.sourcePsi
                ?: return@mapNotNull null
            val endpointExposure = exposure.exposureOf(endpoint.id)
            if (endpointExposure == EndpointExposure.NOT_EXPOSED) return@mapNotNull null

            val others = extensions.mapNotNull { it.psiClass.qualifiedName }
                .filter { it != psiClass.qualifiedName }
                .sorted()
            val certain = endpointExposure == EndpointExposure.EXPOSED && extensions.count { !it.isUndecided } >= 2
            manager.createProblemDescriptor(
                anchor,
                SpringWebBundle.message(
                    if (certain) "explyt.spring.web.inspection.actuator.duplicateWebExtension.startupFailure"
                    else "explyt.spring.web.inspection.actuator.duplicateWebExtension.possibleStartupFailure",
                    endpoint.id,
                    others.joinToString()
                ),
                isOnTheFly,
                emptyArray(),
                if (certain) ProblemHighlightType.GENERIC_ERROR else ProblemHighlightType.WARNING
            )
        }.toTypedArray().ifEmpty { null }
    }

    private fun endpointsOf(module: Module): List<ActuatorEndpoint> {
        val declared = ActuatorEndpointKeys.endpointsById(module)
        val builtIn = ActuatorEndpointKeys.libraryEndpointsById(module).filterKeys { it !in declared }
        return (declared.values + builtIn.values).flatten().distinctBy(ActuatorEndpoint::psiClass)
    }

    private fun exposureRule(module: Module): ActuatorExposure.Rule {
        val definitions = DefinedConfigurationPropertiesSearch.getInstance(module.project).getAllProperties(module)
            .groupBy { it.key }
        return ActuatorExposure.of(module, definitions)
    }
}

private val ActuatorWebExtension.isUndecided: Boolean
    get() = verdict is ConditionVerdict.Undecided
