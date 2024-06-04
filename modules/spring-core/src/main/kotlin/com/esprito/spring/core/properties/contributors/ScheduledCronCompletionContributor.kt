package com.esprito.spring.core.properties.contributors

import com.esprito.spring.core.SpringCoreBundle.message
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
                Pair("* * * * * *", message("esprito.spring.inspection.scheduled.cron.seconds", "")),
                Pair("*/5 * * * * *", message("esprito.spring.inspection.scheduled.cron.seconds", "5 ")),
                Pair("0 * * * * *", message("esprito.spring.inspection.scheduled.cron.minute", "")),
                Pair("0 */5 * * * *", message("esprito.spring.inspection.scheduled.cron.minute", "5 ")),
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