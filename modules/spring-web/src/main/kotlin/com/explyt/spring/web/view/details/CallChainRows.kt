/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.view.details

import com.explyt.spring.web.SpringWebBundle.message
import com.explyt.spring.web.trace.CallChain
import com.explyt.spring.web.trace.CallChainTracer
import com.explyt.spring.web.trace.CallChains
import com.explyt.spring.web.trace.CallKind
import com.explyt.spring.web.trace.ProxyAnnotations
import com.explyt.spring.web.trace.SourceNames
import com.explyt.spring.web.trace.TracedCall
import com.explyt.spring.web.trace.TracedMethod
import com.explyt.spring.web.trace.layerOf
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiMethod
import com.intellij.psi.SmartPointerManager

object CallChainRows {

    fun of(chain: CallChain): List<ContractRow> {
        val start = chain.methods.firstOrNull() ?: return emptyList()
        val truncation = ContractRow(
            message("explyt.web.endpoints.details.chain.truncated", CallChains.MAX_TRACED_METHODS),
            ContractRowKind.NOTE,
            uncertain = true,
        ).takeIf { chain.truncated }
        return listOfNotNull(nodeRow(start, chain, mutableSetOf(), callNotes = emptyList()), truncation)
    }

    private fun nodeRow(
        traced: TracedMethod,
        chain: CallChain,
        shown: MutableSet<Int>,
        callNotes: List<String>,
    ): ContractRow {
        val method = traced.method
        chain.idOf(method)?.let(shown::add)
        val proxies = ProxyAnnotations.of(method)
        val selfInvocation = ContractRow(
            message("explyt.web.endpoints.details.chain.self.invocation"),
            ContractRowKind.NOTE,
            uncertain = true,
        ).takeIf { proxies.isNotEmpty() && traced.reachedBy == CallKind.INTERNAL }
        val layer = method.containingClass?.let(::layerOf)?.name
        return ContractRow(
            text = signatureOf(method),
            kind = ContractRowKind.CALL,
            note = (listOfNotNull(layer) + callNotes).joinToString().ifEmpty { null },
            target = pointerTo(method),
            children = proxies.map(::proxyRow) + listOfNotNull(selfInvocation) +
                    traced.calls.map { callRow(it, chain, shown) },
        )
    }

    private fun callRow(call: TracedCall, chain: CallChain, shown: MutableSet<Int>): ContractRow {
        val reached = call.reached
        val id = reached?.let(chain::idOf)
        val notes = listOfNotNull(
            call.line?.let { message("explyt.web.endpoints.details.chain.line", it) },
            call.via?.let { message("explyt.web.endpoints.details.chain.via", it) },
        )
        if (id != null && !call.accessor && id !in shown) return nodeRow(chain.methods[id], chain, shown, notes)
        val state = when {
            !call.resolved -> message("explyt.web.endpoints.details.chain.unresolved")
            call.kind == CallKind.EXTERNAL -> message("explyt.web.endpoints.details.chain.external")
            call.accessor -> message("explyt.web.endpoints.details.chain.accessor")
            id != null -> message("explyt.web.endpoints.details.chain.see.above")
            else -> message("explyt.web.endpoints.details.chain.not.traced")
        }
        return ContractRow(
            text = call.target,
            kind = ContractRowKind.CALL,
            note = (listOf(state) + notes).joinToString(),
            uncertain = !call.resolved,
            target = reached?.let(::pointerTo),
        )
    }

    private fun proxyRow(declared: ProxyAnnotations.Declared) = ContractRow(
        "@" + StringUtil.getShortName(declared.annotation),
        ContractRowKind.NOTE,
        note = when (declared.declaredOn) {
            ProxyAnnotations.DeclaredOn.METHOD -> message("explyt.web.endpoints.details.chain.declared.on.method")
            ProxyAnnotations.DeclaredOn.CLASS -> message("explyt.web.endpoints.details.chain.declared.on.class")
        },
    )

    private fun signatureOf(method: PsiMethod): String =
        "${SourceNames.shortNameOf(method)}(${CallChainTracer.sourceParametersOf(method).joinToString()})"

    private fun pointerTo(method: PsiMethod) = SmartPointerManager.createPointer(method.navigationElement)
}
