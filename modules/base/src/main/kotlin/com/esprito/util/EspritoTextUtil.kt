package com.esprito.util

import org.apache.commons.lang3.StringUtils
import java.text.BreakIterator
import java.util.*

object EspritoTextUtil {

    fun getFirstSentenceWithoutDot(fullSentence: String): String {
        var fullSentenceCopy = fullSentence.trim()
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

}