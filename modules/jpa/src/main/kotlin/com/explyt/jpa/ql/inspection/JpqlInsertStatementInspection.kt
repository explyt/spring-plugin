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

package com.explyt.jpa.ql.inspection

import com.explyt.jpa.JpaBundle
import com.explyt.jpa.ql.psi.JpqlInsertStatement
import com.explyt.jpa.ql.psi.JpqlTypeResolver
import com.explyt.jpa.ql.psi.JpqlVisitor
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor

class JpqlInsertStatementInspection : LocalInspectionTool() {
    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean,
        session: LocalInspectionToolSession
    ): PsiElementVisitor = object : JpqlVisitor() {
        val jpqlTypeResolver = JpqlTypeResolver.getInstance(holder.project)

        override fun visitInsertStatement(statement: JpqlInsertStatement) {
            validateDuplicatedNames(statement)
            validateInsertValuesCount(statement)
            validateTypes(statement)
        }

        private fun validateTypes(statement: JpqlInsertStatement) {
            val targetTypes =
                statement.insertFields.identifierList.map { jpqlTypeResolver.resolveIdentifierType(it) }

            statement.insertTupleList.forEach { insertTuple ->
                insertTuple.insertValueList.zip(targetTypes)
                    .filter { (insertValue, targetType) ->
                        !targetType.isAssignableFrom(insertValue.expression.type)
                    }
                    .forEach {
                        holder.registerProblem(
                            it.first,
                            JpaBundle.message(
                                "explyt.jpa.inspection.insert.type.mismatch",
                                it.second,
                                it.first.expression.type
                            ),
                            ProblemHighlightType.GENERIC_ERROR
                        )
                    }
            }


            val selectItems = statement.selectStatement?.selectClause?.selectItemList ?: return
            selectItems.zip(targetTypes)
                .filter { (selectItem, targetType) ->
                    !targetType.isAssignableFrom(selectItem.expression.type)
                }
                .forEach {
                    holder.registerProblem(
                        it.first,
                        JpaBundle.message(
                            "explyt.jpa.inspection.insert.type.mismatch",
                            it.second,
                            it.first.expression.type
                        ),
                        ProblemHighlightType.GENERIC_ERROR
                    )
                }
        }

        private fun validateDuplicatedNames(statement: JpqlInsertStatement) {
            val identifiers = statement.insertFields.identifierList

            val names = mutableSetOf<String>()

            for (identifier in identifiers) {
                if (identifier.name in names) {
                    holder.registerProblem(
                        identifier,
                        JpaBundle.message("explyt.jpa.inspection.insert.duplicated.field"),
                        ProblemHighlightType.GENERIC_ERROR
                    )
                }
                names.add(identifier.name)
            }
        }

        private fun validateInsertValuesCount(statement: JpqlInsertStatement) {
            val identifiers = statement.insertFields.identifierList

            statement.insertTupleList.forEach { insertTuple ->
                if (insertTuple.insertValueList.size > identifiers.size) {
                    insertTuple.insertValueList.drop(identifiers.size).forEach {
                        holder.registerProblem(
                            it,
                            JpaBundle.message(
                                "explyt.jpa.inspection.insert.parameters.count.unexpected",
                                identifiers.size
                            ),
                            ProblemHighlightType.GENERIC_ERROR
                        )
                    }
                }

                if (insertTuple.insertValueList.size < identifiers.size) {
                    holder.registerProblem(
                        insertTuple.lastChild,
                        JpaBundle.message("explyt.jpa.inspection.insert.parameters.count.lack", identifiers.size),
                        ProblemHighlightType.GENERIC_ERROR
                    )
                }
            }

            val selectClause = statement.selectStatement?.selectClause ?: return
            val selectItems = selectClause.selectItemList
            if (selectItems.size > identifiers.size) {
                selectItems.drop(identifiers.size).forEach {
                    holder.registerProblem(
                        it,
                        JpaBundle.message("explyt.jpa.inspection.insert.parameters.count.unexpected", identifiers.size),
                        ProblemHighlightType.GENERIC_ERROR
                    )
                }
            }

            if (selectItems.size < identifiers.size) {
                holder.registerProblem(
                    selectClause.firstChild,
                    JpaBundle.message("explyt.jpa.inspection.insert.parameters.count.lack", identifiers.size),
                    ProblemHighlightType.GENERIC_ERROR
                )
            }
        }
    }
}