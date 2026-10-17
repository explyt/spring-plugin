/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service

import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifierList
import com.intellij.psi.impl.light.LightClass
import com.intellij.psi.impl.light.LightMethod

class FactoryTouchDecoy(holder: PsiClass, factoryName: String) : LightClass(holder) {

    private val touches = mutableListOf<String>()
    private var armed = false

    val factory: PsiMethod = AnnotationGuardedMethod(holder.findMethodsByName(factoryName, false).single())

    fun arm() {
        touches.clear()
        armed = true
    }

    fun touches(): List<String> = synchronized(touches) { touches.toList() }

    override fun getMethods(): Array<PsiMethod> = arrayOf(factory)

    override fun findMethodsByName(name: String?, checkBases: Boolean): Array<PsiMethod> =
        if (name == factory.name) arrayOf(factory) else PsiMethod.EMPTY_ARRAY

    private fun <T> guarded(member: String, read: () -> T): T {
        if (armed) synchronized(touches) { touches += "$member from ${productionCaller()}" }
        return read()
    }

    private fun productionCaller(): String = Thread.currentThread().stackTrace
        .firstOrNull { it.className.startsWith("com.explyt.") && !it.className.startsWith(FactoryTouchDecoy::class.java.name) }
        ?.let { "${it.className.substringAfterLast('.')}.${it.methodName}" }
        ?: "unknown"

    private inner class AnnotationGuardedMethod(delegate: PsiMethod) :
        LightMethod(delegate.manager, delegate, this@FactoryTouchDecoy) {

        override fun getModifierList(): PsiModifierList = guarded("modifierList") { super.getModifierList() }

        override fun getAnnotations(): Array<PsiAnnotation> = guarded("annotations") { super.getAnnotations() }

        override fun hasAnnotation(fqn: String): Boolean = guarded("hasAnnotation") { super.hasAnnotation(fqn) }

        override fun getAnnotation(fqn: String): PsiAnnotation? = guarded("annotation") { super.getAnnotation(fqn) }
    }
}
