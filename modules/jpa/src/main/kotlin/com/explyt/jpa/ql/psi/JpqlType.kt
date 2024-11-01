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

package com.explyt.jpa.ql.psi

import com.intellij.psi.CommonClassNames
import com.intellij.psi.PsiType
import com.intellij.psi.PsiTypes
import com.intellij.psi.util.InheritanceUtil
import com.siyeh.ig.psiutils.TypeUtils

sealed class JpqlType {
    object String : JpqlType()
    object Numeric : JpqlType()
    object Datetime : JpqlType()
    object Boolean : JpqlType()
    object Type : JpqlType()
    object Null : JpqlType()


    data class Entity(val psiType: PsiType) : JpqlType() {
        override fun isAssignableFrom(maybeSubtype: JpqlType): kotlin.Boolean {
            if (maybeSubtype == Null) {
                return true
            }

            if (maybeSubtype !is Entity) {
                return false
            }

            return psiType.isAssignableFrom(maybeSubtype.psiType)
        }
    }

    object Unknown : JpqlType()

    open fun isAssignableFrom(maybeSubtype: JpqlType): kotlin.Boolean {
        if (this == Datetime && maybeSubtype == String) {
            return true
        }

        return maybeSubtype == this
                || maybeSubtype == Null
                || maybeSubtype == Unknown
                || this == Unknown
    }

    override fun toString(): kotlin.String {
        if (this in NAMES)
            return NAMES.getValue(this)

        if (this is Entity) {
            return psiType.canonicalText
        }

        return super.toString()
    }

    companion object {
        private val NAMES = mapOf(
            String to "String",
            Numeric to "Numeric",
            Datetime to "Datetime",
            Boolean to "Boolean",
            Type to "Type",
            Null to "Null",
            Unknown to "Unknown",
        )

        private val numericPrimitives = listOf(
            PsiTypes.byteType(),
            PsiTypes.shortType(),
            PsiTypes.intType(),
            PsiTypes.longType(),
            PsiTypes.floatType(),
            PsiTypes.doubleType(),
        )

        private val numericObjects = listOf(
            CommonClassNames.JAVA_LANG_BYTE,
            CommonClassNames.JAVA_LANG_SHORT,
            CommonClassNames.JAVA_LANG_INTEGER,
            CommonClassNames.JAVA_LANG_LONG,
            CommonClassNames.JAVA_LANG_FLOAT,
            CommonClassNames.JAVA_LANG_DOUBLE,
            "java.math.BigDecimal",
            "java.math.BigInteger",
        )

        private val stringObjects = listOf(
            CommonClassNames.JAVA_LANG_CHARACTER,
            CommonClassNames.JAVA_LANG_STRING
        )

        private val datetimeObjects = listOf(
            CommonClassNames.JAVA_TIME_LOCAL_DATE,
            CommonClassNames.JAVA_TIME_LOCAL_DATE_TIME,
            CommonClassNames.JAVA_TIME_LOCAL_TIME,
            CommonClassNames.JAVA_TIME_OFFSET_TIME,
            CommonClassNames.JAVA_TIME_OFFSET_DATE_TIME,
            CommonClassNames.JAVA_TIME_ZONED_DATE_TIME,
            "java.time.Instant",
        )

        private fun List<kotlin.String>.checkType(psiType: PsiType): kotlin.Boolean {
            return any {
                TypeUtils.typeEquals(it, psiType)
            }
        }

        fun fromPsiType(psiType: PsiType): JpqlType {
            return when {
                PsiTypes.booleanType() == psiType
                        || TypeUtils.typeEquals(CommonClassNames.JAVA_LANG_BOOLEAN, psiType)
                -> Boolean

                psiType in numericPrimitives
                        || numericObjects.checkType(psiType)
                -> Numeric

                psiType == PsiTypes.charType()
                        || stringObjects.checkType(psiType)
                        || InheritanceUtil.isInheritor(psiType, CommonClassNames.JAVA_LANG_CHAR_SEQUENCE)
                -> String

                datetimeObjects.checkType(psiType) -> Datetime

                //todo

                else -> Unknown
            }
        }
    }
}