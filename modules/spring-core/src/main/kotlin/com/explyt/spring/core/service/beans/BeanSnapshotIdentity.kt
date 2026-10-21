/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service.beans

import java.security.MessageDigest

/**
 * Stable opaque identity for contexts, beans and model state.
 *
 * Parts are length-prefixed before hashing so that `["a", "bc"]` and `["ab", "c"]` cannot collide, and callers
 * pass an ordered list rather than a set: a hash that changes with iteration order would invalidate a
 * continuation that describes the very same model.
 *
 * The hash also keeps machine paths out of the wire: the linked path identifies a context internally, while the
 * caller only ever sees the digest.
 */
object BeanSnapshotIdentity {

    fun hash(parts: List<String>): String {
        val digest = MessageDigest.getInstance("SHA-256")
        parts.forEach { part ->
            val bytes = part.toByteArray(Charsets.UTF_8)
            digest.update(bytes.size.toString().toByteArray(Charsets.UTF_8))
            digest.update(SEPARATOR)
            digest.update(bytes)
        }
        return digest.digest().joinToString("") { "%02x".format(it) }.take(HASH_LENGTH)
    }

    private val SEPARATOR = byteArrayOf(0)
    private const val HASH_LENGTH = 16
}
