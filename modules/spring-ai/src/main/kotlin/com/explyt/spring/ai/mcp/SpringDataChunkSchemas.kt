/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp

internal object SpringDataChunkSchemas {
    const val PAGE = "org.springframework.data.domain.Page"
    const val PAGE_IMPL = "org.springframework.data.domain.PageImpl"
    const val SLICE = "org.springframework.data.domain.Slice"
    const val SLICE_IMPL = "org.springframework.data.domain.SliceImpl"
    const val PAGED_MODEL = "org.springframework.data.web.PagedModel"
    private const val PAGE_METADATA = "org.springframework.data.web.PagedModel.PageMetadata"
    private const val PAGEABLE = "org.springframework.data.domain.Pageable"
    private const val SORT = "org.springframework.data.domain.Sort"

    val CHUNKS = setOf(PAGE, PAGE_IMPL, SLICE, SLICE_IMPL)

    fun sliceProperties(depth: Int): List<DtoFieldJson> = listOf(
        field("pageable", PAGEABLE, pageable(depth - 1)),
        field("first", "boolean"),
        field("last", "boolean"),
        field("size", "int"),
        field("number", "int"),
        field("sort", SORT, sort(depth - 1)),
        field("numberOfElements", "int"),
        field("empty", "boolean"),
    )

    fun pageProperties(depth: Int): List<DtoFieldJson> =
        sliceProperties(depth) + field("totalElements", "long") + field("totalPages", "int")

    fun pagedModelMetadata(depth: Int): DtoFieldJson = field(
        "page", PAGE_METADATA,
        schema(depth - 1, PAGE_METADATA, listOf("size", "number", "totalElements", "totalPages").map { field(it, "long") }),
    )

    private fun sort(depth: Int): DtoSchemaJson? =
        schema(depth, SORT, listOf("empty", "sorted", "unsorted").map { field(it, "boolean") })

    private fun pageable(depth: Int): DtoSchemaJson? = schema(
        depth, PAGEABLE,
        listOf(
            field("pageNumber", "int"),
            field("pageSize", "int"),
            field("sort", SORT, sort(depth - 1)),
            field("offset", "long"),
            field("paged", "boolean"),
            field("unpaged", "boolean"),
        ),
    )?.copy(wireShapeAssumed = true)

    private fun schema(depth: Int, className: String, fields: List<DtoFieldJson>): DtoSchemaJson? =
        DtoSchemaJson(className = className, fields = fields).takeIf { depth > 0 }

    private fun field(name: String, type: String, nested: DtoSchemaJson? = null): DtoFieldJson =
        DtoFieldJson(name = name, declaredName = null, type = type, nullable = false, nested = nested)
}
