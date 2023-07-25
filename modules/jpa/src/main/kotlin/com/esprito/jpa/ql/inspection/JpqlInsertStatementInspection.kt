package com.esprito.jpa.ql.inspection

import com.esprito.jpa.JpaBundle
import com.esprito.jpa.ql.psi.JpqlInsertStatement
import com.esprito.jpa.ql.psi.JpqlTypeResolver
import com.esprito.jpa.ql.psi.JpqlVisitor
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
                                "esprito.jpa.inspection.insert.type.mismatch",
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
                            "esprito.jpa.inspection.insert.type.mismatch",
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
                        JpaBundle.message("esprito.jpa.inspection.insert.duplicated.field"),
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
                            JpaBundle.message("esprito.jpa.inspection.insert.parameters.count.unexpected", identifiers.size),
                            ProblemHighlightType.GENERIC_ERROR
                        )
                    }
                }

                if (insertTuple.insertValueList.size < identifiers.size) {
                    holder.registerProblem(
                        insertTuple.lastChild,
                        JpaBundle.message("esprito.jpa.inspection.insert.parameters.count.lack", identifiers.size),
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
                        JpaBundle.message("esprito.jpa.inspection.insert.parameters.count.unexpected", identifiers.size),
                        ProblemHighlightType.GENERIC_ERROR
                    )
                }
            }

            if (selectItems.size < identifiers.size) {
                holder.registerProblem(
                    selectClause.firstChild,
                    JpaBundle.message("esprito.jpa.inspection.insert.parameters.count.lack", identifiers.size),
                    ProblemHighlightType.GENERIC_ERROR
                )
            }
        }
    }
}