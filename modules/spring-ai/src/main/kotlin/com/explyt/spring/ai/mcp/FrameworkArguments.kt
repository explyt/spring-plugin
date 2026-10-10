/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp

import com.explyt.spring.web.SpringWebClasses
import com.explyt.spring.web.util.WebApplicationStack
import com.intellij.psi.CommonClassNames
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiType
import com.intellij.psi.util.InheritanceUtil
import com.intellij.psi.util.PsiUtil

internal enum class ArgumentSource { FRAMEWORK, UNKNOWN }

internal enum class TypeMatch {
    EXACT {
        override fun test(type: PsiType, typeFqn: String): Boolean =
            (type as? PsiClassType)?.resolve()?.qualifiedName == typeFqn
    },
    SUBTYPE {
        override fun test(type: PsiType, typeFqn: String): Boolean = InheritanceUtil.isInheritor(type, typeFqn)
    };

    abstract fun test(type: PsiType, typeFqn: String): Boolean
}

internal enum class ArgumentStacks {
    SERVLET {
        override fun admits(stack: WebApplicationStack?): Boolean = stack != WebApplicationStack.REACTIVE
    },
    REACTIVE {
        override fun admits(stack: WebApplicationStack?): Boolean = stack == WebApplicationStack.REACTIVE
    },
    BOTH {
        override fun admits(stack: WebApplicationStack?): Boolean = true
    };

    abstract fun admits(stack: WebApplicationStack?): Boolean
}

internal enum class ArgumentWrapping {
    NONE {
        override fun candidates(type: PsiType): Sequence<PsiType> = sequenceOf(type)
    },
    OPTIONAL {
        override fun candidates(type: PsiType): Sequence<PsiType> =
            sequenceOf(type) + listOfNotNull(payloadOf(type, CommonClassNames.JAVA_UTIL_OPTIONAL))
    },
    REACTIVE_ADAPTER {
        override fun candidates(type: PsiType): Sequence<PsiType> =
            sequenceOf(type) + listOfNotNull(REACTIVE_ADAPTED_WRAPPERS.firstNotNullOfOrNull { payloadOf(type, it) })
    };

    abstract fun candidates(type: PsiType): Sequence<PsiType>

    private companion object {
        val REACTIVE_ADAPTED_WRAPPERS = listOf(
            "org.reactivestreams.Publisher",
            "java.util.concurrent.Flow.Publisher",
            "java.util.concurrent.CompletionStage",
        )

        fun payloadOf(type: PsiType, wrapperFqn: String): PsiType? =
            PsiUtil.substituteTypeParameter(type, wrapperFqn, 0, false)
    }
}

internal data class FrameworkArgumentRule(
    val typeFqn: String,
    val match: TypeMatch,
    val stacks: ArgumentStacks,
    val wrapping: ArgumentWrapping = ArgumentWrapping.NONE,
    val source: ArgumentSource = ArgumentSource.FRAMEWORK,
    val unannotatedOnly: Boolean = false,
) {
    fun appliesTo(type: PsiType, stack: WebApplicationStack?, annotated: Boolean): Boolean =
        !(annotated && unannotatedOnly) &&
                stacks.admits(stack) &&
                wrapping.candidates(type).any { match.test(it, typeFqn) }
}

internal object FrameworkArguments {

    fun sourceOf(type: PsiType, stack: WebApplicationStack?, annotated: Boolean): ArgumentSource? =
        RULES.firstOrNull { it.appliesTo(type, stack, annotated) }?.source

    private const val PRINCIPAL = "java.security.Principal"
    private const val ERRORS = "org.springframework.validation.Errors"
    private const val MODEL = "org.springframework.ui.Model"
    private const val MODEL_MAP = "org.springframework.ui.ModelMap"
    private const val LOCALE = "java.util.Locale"
    private const val TIME_ZONE = "java.util.TimeZone"
    private const val ZONE_ID = "java.time.ZoneId"
    private const val URI_COMPONENTS_BUILDER = "org.springframework.web.util.UriComponentsBuilder"
    private const val SESSION_STATUS = "org.springframework.web.bind.support.SessionStatus"
    private const val API_VERSION = "org.springframework.web.accept.SemanticApiVersionParser.Version"

    private val RULES: List<FrameworkArgumentRule> = buildList {
        add(ArgumentStacks.SERVLET, TypeMatch.SUBTYPE,
            "org.springframework.web.context.request.WebRequest",
            SpringWebClasses.JAKARTA_SERVLET_REQUEST,
            SpringWebClasses.JAVAX_SERVLET_REQUEST,
            "org.springframework.web.multipart.MultipartRequest",
            "jakarta.servlet.http.HttpSession",
            "javax.servlet.http.HttpSession",
            "jakarta.servlet.http.PushBuilder",
            "javax.servlet.http.PushBuilder",
            PRINCIPAL,
            "java.io.InputStream",
            "java.io.Reader",
        )
        add(ArgumentStacks.SERVLET, TypeMatch.EXACT, SpringWebClasses.HTTP_METHOD, LOCALE, TIME_ZONE, ZONE_ID)
        add(ArgumentStacks.SERVLET, TypeMatch.SUBTYPE,
            "jakarta.servlet.ServletResponse",
            "javax.servlet.ServletResponse",
            "java.io.OutputStream",
            "java.io.Writer",
            "org.springframework.web.servlet.mvc.support.RedirectAttributes",
            MODEL,
        )
        add(ArgumentStacks.SERVLET, TypeMatch.SUBTYPE, MODEL_MAP, unannotatedOnly = true)
        add(ArgumentStacks.SERVLET, TypeMatch.EXACT, CommonClassNames.JAVA_UTIL_MAP, unannotatedOnly = true)
        add(ArgumentStacks.SERVLET, TypeMatch.SUBTYPE, ERRORS)
        add(ArgumentStacks.SERVLET, TypeMatch.EXACT,
            "org.springframework.web.servlet.support.ServletUriComponentsBuilder",
        )

        add(ArgumentStacks.REACTIVE, TypeMatch.SUBTYPE, MODEL)
        add(ArgumentStacks.REACTIVE, TypeMatch.SUBTYPE, MODEL_MAP, source = ArgumentSource.UNKNOWN)
        add(ArgumentStacks.REACTIVE, TypeMatch.SUBTYPE, CommonClassNames.JAVA_UTIL_MAP, unannotatedOnly = true)
        add(ArgumentStacks.REACTIVE, TypeMatch.SUBTYPE, ERRORS, wrapping = ArgumentWrapping.REACTIVE_ADAPTER)
        add(ArgumentStacks.REACTIVE, TypeMatch.SUBTYPE,
            "org.springframework.web.server.ServerWebExchange",
            "org.springframework.http.server.reactive.ServerHttpRequest",
            "org.springframework.http.server.reactive.ServerHttpResponse",
        )
        add(ArgumentStacks.REACTIVE, TypeMatch.EXACT,
            SpringWebClasses.HTTP_METHOD, LOCALE, TIME_ZONE, ZONE_ID, "org.springframework.web.util.UriBuilder",
        )
        add(ArgumentStacks.REACTIVE, TypeMatch.SUBTYPE, PRINCIPAL, wrapping = ArgumentWrapping.REACTIVE_ADAPTER)
        add(ArgumentStacks.REACTIVE, TypeMatch.SUBTYPE,
            "org.springframework.web.server.WebSession", wrapping = ArgumentWrapping.REACTIVE_ADAPTER,
        )

        add(ArgumentStacks.BOTH, TypeMatch.EXACT, SESSION_STATUS, URI_COMPONENTS_BUILDER)
        add(ArgumentStacks.BOTH, TypeMatch.EXACT, API_VERSION, wrapping = ArgumentWrapping.OPTIONAL)
    }

    private fun MutableList<FrameworkArgumentRule>.add(
        stacks: ArgumentStacks,
        match: TypeMatch,
        vararg typeFqns: String,
        wrapping: ArgumentWrapping = ArgumentWrapping.NONE,
        source: ArgumentSource = ArgumentSource.FRAMEWORK,
        unannotatedOnly: Boolean = false,
    ) {
        typeFqns.mapTo(this) { FrameworkArgumentRule(it, match, stacks, wrapping, source, unannotatedOnly) }
    }
}
