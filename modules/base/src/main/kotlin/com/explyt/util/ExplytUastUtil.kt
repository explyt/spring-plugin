/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.util

import org.jetbrains.uast.UComment

object ExplytUastUtil {

    fun UComment.getCommentText(): String {
        val commentText = text.trim()

        if (commentText.startsWith("//")) {
            return commentText.substring(2).trim()
        } else if (commentText.startsWith("/*") && commentText.endsWith("*/")) {
            return commentText.substring(2, commentText.length - 2).trimIndent()
        }

        return commentText
    }


}