/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service

import com.intellij.openapi.util.Pair
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiSubstitutor
import com.intellij.psi.impl.light.LightClass
import com.intellij.psi.impl.light.LightMethod

class HierarchySearchDecoy(configuration: PsiClass) : LightClass(configuration) {

    fun declare(method: PsiMethod): PsiMethod = LightMethod(manager, method, this)

    override fun findMethodsAndTheirSubstitutorsByName(
        name: String,
        checkBases: Boolean
    ): List<Pair<PsiMethod, PsiSubstitutor>> =
        if (checkBases) searched() else super.findMethodsAndTheirSubstitutorsByName(name, false)

    override fun findMethodsByName(name: String?, checkBases: Boolean): Array<PsiMethod> =
        if (checkBases) searched() else super.findMethodsByName(name, false)

    override fun findMethodsBySignature(patternMethod: PsiMethod, checkBases: Boolean): Array<PsiMethod> =
        if (checkBases) searched() else super.findMethodsBySignature(patternMethod, false)

    override fun findMethodBySignature(patternMethod: PsiMethod, checkBases: Boolean): PsiMethod? =
        if (checkBases) searched() else super.findMethodBySignature(patternMethod, false)

    override fun getAllMethods(): Array<PsiMethod> = searched()

    override fun getAllMethodsAndTheirSubstitutors(): List<Pair<PsiMethod, PsiSubstitutor>> = searched()

    private fun searched(): Nothing = error("A factory declared in $name must not search the hierarchy of $name")
}
