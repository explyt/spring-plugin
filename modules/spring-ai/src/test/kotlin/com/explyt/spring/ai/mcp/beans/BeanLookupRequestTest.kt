/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp.beans

import com.explyt.spring.core.service.beans.BeanQueryException
import com.explyt.spring.core.service.beans.BeanSourcePreference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * What the tool accepts before any PSI is touched.
 *
 * A selector that names two modes at once, or names none, has no single correct interpretation - answering it
 * would mean guessing which question was asked. The paging arguments are deliberately absent here: the writer
 * already owns their bounds, and validating them twice would let the two definitions drift apart.
 */
class BeanLookupRequestTest {

    @Test
    fun `a lookup needs a type or a name`() {
        val problem = rejected(request())

        assertEquals(BoundedBeanResponseWriter.INVALID_ARGUMENT, problem.code)
    }

    @Test
    fun `a type lookup is accepted`() {
        request(typeFqn = "java.time.Clock").validate()
    }

    @Test
    fun `a name lookup is accepted`() {
        request(beanName = "systemClock").validate()
    }

    @Test
    fun `a type and a name together intersect rather than conflict`() {
        request(typeFqn = "java.time.Clock", beanName = "systemClock").validate()
    }

    @Test
    fun `an injection point is accepted`() {
        request(filePath = "src/main/java/A.java", line = 12).validate()
    }

    /** Mixing the modes asks two different questions; the tool cannot tell which answer was wanted. */
    @Test
    fun `a selector and an injection point cannot be combined`() {
        val problem = rejected(request(typeFqn = "java.time.Clock", filePath = "src/main/java/A.java", line = 12))

        assertEquals(BoundedBeanResponseWriter.INVALID_ARGUMENT, problem.code)
    }

    @Test
    fun `a file without a line is incomplete`() {
        assertEquals(
            BoundedBeanResponseWriter.INVALID_ARGUMENT,
            rejected(request(filePath = "src/main/java/A.java")).code
        )
        assertEquals(BoundedBeanResponseWriter.INVALID_ARGUMENT, rejected(request(line = 12)).code)
    }

    @Test
    fun `a column is meaningless without an injection point`() {
        val problem = rejected(request(typeFqn = "java.time.Clock", column = 4))

        assertEquals(BoundedBeanResponseWriter.INVALID_ARGUMENT, problem.code)
    }

    /** An empty string is a selector the caller meant to fill in, not an absent one. */
    @Test
    fun `a blank selector is not an absent selector`() {
        assertEquals(BoundedBeanResponseWriter.INVALID_ARGUMENT, rejected(request(beanName = "  ")).code)
        assertEquals(BoundedBeanResponseWriter.INVALID_ARGUMENT, rejected(request(typeFqn = "")).code)
    }

    @Test
    fun `source names one of the supported models`() {
        assertEquals(BeanSourcePreference.NATIVE, request(typeFqn = "A", source = "NATIVE").source())
        assertEquals(BoundedBeanResponseWriter.INVALID_ARGUMENT, rejected(request(typeFqn = "A", source = "auto")).code)
        assertEquals(
            BoundedBeanResponseWriter.INVALID_ARGUMENT,
            rejected(request(typeFqn = "A", source = "LATEST")).code
        )
    }

    /** A static model has no loaded root to name, so a context id would silently do nothing. */
    @Test
    fun `a context id contradicts the static model`() {
        val problem = rejected(request(typeFqn = "A", source = "STATIC", contextId = "ctx-1"))

        assertEquals(BoundedBeanResponseWriter.INVALID_ARGUMENT, problem.code)
    }

    @Test
    fun `the mode follows from the input`() {
        assertEquals("LOOKUP", request(beanName = "clock").mode())
        assertEquals("INJECTION", request(filePath = "src/main/java/A.java", line = 12).mode())
    }

    /**
     * The fingerprint covers what was asked, not how it is being paged: a caller may widen `limit` or `maxChars`
     * between pages of one result, and binding those would reject a continuation of the very same answer.
     */
    @Test
    fun `the query fingerprint holds the question and not the paging`() {
        val query = request(beanName = "clock").normalizedQuery()

        assertEquals(
            setOf("application", "source", "contextId", "typeFqn", "beanName", "filePath", "line", "column", "details"),
            query.keys
        )
        assertEquals(query, request(beanName = "clock").normalizedQuery())
    }

    private fun rejected(request: BeanLookupRequest) =
        assertThrows(BeanQueryException::class.java) { request.validate() }.problem

    private fun request(
        applicationClassName: String? = null,
        source: String = "AUTO",
        contextId: String? = null,
        typeFqn: String? = null,
        beanName: String? = null,
        filePath: String? = null,
        line: Int? = null,
        column: Int? = null,
        includeDetails: Boolean = false
    ) = BeanLookupRequest(
        projectPath = "/tmp/project",
        applicationClassName = applicationClassName,
        source = source,
        contextId = contextId,
        typeFqn = typeFqn,
        beanName = beanName,
        filePath = filePath,
        line = line,
        column = column,
        includeDetails = includeDetails
    )
}
