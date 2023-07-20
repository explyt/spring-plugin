package com.esprito.spring.core.completion.properties

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.codeInsight.lookup.LookupElementRenderer
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.JBColor
import org.apache.commons.lang3.StringUtils
import java.text.BreakIterator
import java.util.*
import java.util.regex.Pattern

class PropertyRenderer : LookupElementRenderer<LookupElement>() {
    private val packageRemovalPattern = Pattern.compile("[a-zA-Z_][a-zA-Z_0-9]*\\.")

    override fun renderElement(element: LookupElement, presentation: LookupElementPresentation) {
        val availableProperty = element.`object` as ConfigurationProperty
        val lookupString = element.lookupString
        presentation.itemText = lookupString
        val defaultValue: Any? = availableProperty.defaultValue
        if (defaultValue != null) {
            val shortDescription = StringUtil.shortenTextWithEllipsis(defaultValue.toString(), 60, 0, true)
            presentation.setTailText("=$shortDescription", JBColor.GREEN)
        }

        if(availableProperty.type != null) {
            presentation.typeText = shortenedType(availableProperty.type)
        }

        if (availableProperty.description != null) {
            presentation.appendTailText(" (" + getFirstSentenceWithoutDot(availableProperty.description) + ")", true)
        }
    }

    private fun getFirstSentenceWithoutDot(fullSentence: String): String {
        var fullSentenceCopy = fullSentence
        if (fullSentenceCopy.contains('.')) {
            val breakIterator = BreakIterator.getSentenceInstance(Locale.US)
            breakIterator.setText(fullSentenceCopy)
            fullSentenceCopy = fullSentenceCopy.substring(breakIterator.first(), breakIterator.next()).trim { it <= ' ' }
        }
        if (fullSentenceCopy.isNotEmpty()) {
            val withoutDot = if (fullSentenceCopy.endsWith('.')) {
                fullSentenceCopy.substring(0, fullSentenceCopy.length - 1)
            } else {
                fullSentenceCopy
            }
            return withoutDot.replace(StringUtils.LF, StringUtils.EMPTY)
        }
        return StringUtils.EMPTY
    }

    private fun shortenedType(type: String?): String? {
        if (type == null) return null
        val matcher = packageRemovalPattern.matcher(type)
        return if (matcher.find()) matcher.replaceAll(StringUtils.EMPTY) else type
    }
}