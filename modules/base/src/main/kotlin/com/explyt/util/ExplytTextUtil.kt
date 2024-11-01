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

package com.explyt.util

import org.apache.commons.lang3.StringUtils
import java.text.BreakIterator
import java.util.*

object ExplytTextUtil {

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