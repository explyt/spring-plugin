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

package com.explyt.spring.core.properties.contributors

import com.explyt.spring.core.SpringCoreBundle.message
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns
import com.intellij.util.ProcessingContext
import org.jetbrains.uast.*

class ScheduledCronCompletionContributor : CompletionContributor() {

    init {
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement(),
            ConditionalOnConfigurationPrefixCompletionProvider()
        )
    }

    class ConditionalOnConfigurationPrefixCompletionProvider : CompletionProvider<CompletionParameters>() {

        override fun addCompletions(
            parameters: CompletionParameters,
            context: ProcessingContext,
            result: CompletionResultSet
        ) {
            val psiElement = parameters.position
            val uExpression = psiElement.parent.toUElement() as? UExpression ?: return
            uExpression.getParentOfType<UAnnotation>() ?: return
            val uNamedExpression = uExpression.getParentOfType<UNamedExpression>() ?: return
            if (uNamedExpression.name != "cron") return

            MACROS.forEachIndexed { index, element ->
                if (index % 2 == 0) {
                    result.addElement(
                        LookupElementBuilder.create(element)
                            .withPresentableText(element + " (${MACROS[index + 1]})")
                    )
                }
            }
            BASIC_CRON_LIST.forEach {
                result.addElement(
                    LookupElementBuilder.create(it.first)
                        .withPresentableText(it.first + " (${it.second})")
                )
            }

            result.stopHere()
        }
    }

    companion object {
        private val BASIC_CRON_LIST =
            listOf(
                Pair("* * * * * *", message("explyt.spring.inspection.scheduled.cron.seconds", "")),
                Pair("*/5 * * * * *", message("explyt.spring.inspection.scheduled.cron.seconds", "5 ")),
                Pair("0 * * * * *", message("explyt.spring.inspection.scheduled.cron.minute", "")),
                Pair("0 */5 * * * *", message("explyt.spring.inspection.scheduled.cron.minute", "5 ")),
            )

        private val MACROS: Array<String> = arrayOf(
            "@yearly", "0 0 0 1 1 *",
            "@annually", "0 0 0 1 1 *",
            "@monthly", "0 0 0 1 * *",
            "@weekly", "0 0 0 * * 0",
            "@daily", "0 0 0 * * *",
            "@midnight", "0 0 0 * * *",
            "@hourly", "0 0 * * * *"
        )
    }
}