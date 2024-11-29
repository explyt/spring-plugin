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

package com.explyt.spring.core.inspections

import com.cronutils.model.CronType
import com.cronutils.model.definition.CronDefinitionBuilder
import com.cronutils.parser.CronParser
import com.explyt.inspection.SpringBaseUastLocalInspectionTool
import com.explyt.spring.core.SpringCoreBundle.message
import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.util.UastUtil.getPropertyValue
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType.WARNING
import com.intellij.codeInspection.ProblemsHolder
import org.jetbrains.uast.*
import java.time.Duration

val CRON_PARSER = CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.SPRING53))

/**
 * Original spring code in org.springframework.scheduling.support.CronExpression.
 */
class SpringScheduledInspection : SpringBaseUastLocalInspectionTool() {

    override fun checkMethod(
        method: UMethod,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<ProblemDescriptor> {
        val annotation = method.findAnnotation(SpringCoreClasses.SCHEDULED) ?: return emptyArray()
        val problems = ProblemsHolder(manager, method.javaPsi.containingFile, isOnTheFly)
        checkScheduledParametersCount(annotation, problems)
        checkInitParametersCount(annotation, problems)
        checkCronValue(annotation, problems)
        checkStringValues(annotation, problems)
        return problems.resultsArray
    }

    private fun checkScheduledParametersCount(annotation: UAnnotation, problems: ProblemsHolder) {
        var processedSchedule = false
        var value = annotation.findAttributeValue(CRON_PARAM)
        if (isPresentValue(value)) {
            processedSchedule = true
        }

        value = annotation.findAttributeValue(FIXED_DELAY)
        if (isPresentValue(value) && processedSchedule) {
            registerExactlyOneProblem(annotation, problems)
            return
        } else if (isPresentValue(value)) {
            processedSchedule = true
        }
        value = annotation.findAttributeValue(FIXED_DELAY_STRING)
        if (isPresentValue(value) && processedSchedule) {
            registerExactlyOneProblem(annotation, problems)
            return
        } else if (isPresentValue(value)) {
            processedSchedule = true
        }
        value = annotation.findAttributeValue(FIXED_RATE)
        if (isPresentValue(value) && processedSchedule) {
            registerExactlyOneProblem(annotation, problems)
            return
        } else if (isPresentValue(value)) {
            processedSchedule = true
        }
        value = annotation.findAttributeValue(FIXED_RATE_STRING)
        if (isPresentValue(value) && processedSchedule) {
            registerExactlyOneProblem(annotation, problems)
            return
        } else if (isPresentValue(value)) {
            processedSchedule = true
        }
        if (!processedSchedule) {
            registerExactlyOneProblem(annotation, problems)
        }
    }

    private fun checkInitParametersCount(annotation: UAnnotation, problems: ProblemsHolder) {
        if (isPresentValue(annotation.findAttributeValue(INITIAL_DELAY))
            && isPresentValue(annotation.findAttributeValue(INITIAL_DELAY_STRING))
        ) {
            val psiElement = annotation.sourcePsi ?: return
            problems.registerProblem(psiElement, message("explyt.spring.inspection.scheduling.initial.both"), WARNING)
        }
    }

    private fun isPresentValue(value: UExpression?): Boolean {
        value ?: return false
        if (value is UUnknownExpression) return false
        if (value is UPrefixExpression && value.operand is UUnknownExpression) return false
        return true
    }

    private fun checkCronValue(annotation: UAnnotation, problems: ProblemsHolder) {
        val uExpression = annotation.findAttributeValue(CRON_PARAM) ?: return
        val value = uExpression.getPropertyValue() ?: return
        val psiElement = uExpression.sourcePsi ?: return
        try {
            CRON_PARSER.parse(value)
        } catch (e: Exception) {
            val errorString = e.message ?: return
            val finalMessage = errorString + System.lineSeparator() +
                    message("explyt.spring.inspection.scheduled.cron.hint")
            problems.registerProblem(psiElement, finalMessage)
        }
    }

    private fun checkStringValues(annotation: UAnnotation, problems: ProblemsHolder) {
        annotation.findAttributeValue(INITIAL_DELAY_STRING)?.also { checkStringValue(it, problems) }
        annotation.findAttributeValue(FIXED_DELAY_STRING)?.also { checkStringValue(it, problems) }
        annotation.findAttributeValue(FIXED_RATE_STRING)?.also { checkStringValue(it, problems) }
    }

    private fun checkStringValue(uExpression: UExpression, problems: ProblemsHolder) {
        val value = uExpression.getPropertyValue() ?: return
        if (isDurationString(value)) {
            try {
                Duration.parse(value)
            } catch (e: Exception) {
                val errorString = e.message ?: return
                val psiElement = uExpression.sourcePsi ?: return
                problems.registerProblem(psiElement, errorString, WARNING)
            }
        } else {
            try {
                value.toLong()
            } catch (e: Exception) {
                val psiElement = uExpression.sourcePsi ?: return
                problems.registerProblem(
                    psiElement, message("explyt.spring.inspection.scheduled.parse.long", value), WARNING
                )
            }
        }
    }

    private fun registerExactlyOneProblem(annotation: UAnnotation, problems: ProblemsHolder) {
        val psiElement = annotation.sourcePsi ?: return
        problems.registerProblem(psiElement, message("explyt.spring.inspection.scheduling.required"), WARNING)
    }

    private fun isDurationString(value: String): Boolean {
        return (value.length > 1 && (isP(value[0]) || isP(value[1])))
    }

    private fun isP(ch: Char): Boolean {
        return (ch == 'P' || ch == 'p')
    }

    companion object {
        const val CRON_PARAM = "cron"
        const val INITIAL_DELAY = "initialDelay"
        const val INITIAL_DELAY_STRING = "initialDelayString"
        const val FIXED_DELAY_STRING = "fixedDelayString"
        const val FIXED_RATE_STRING = "fixedRateString"
        const val FIXED_DELAY = "fixedDelay"
        const val FIXED_RATE = "fixedRate"
    }
}