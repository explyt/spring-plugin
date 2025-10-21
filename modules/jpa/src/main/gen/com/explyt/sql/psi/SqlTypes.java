// This is a generated file. Not intended for manual editing.
package com.explyt.sql.psi;

import com.explyt.sql.psi.impl.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;

public interface SqlTypes {

    IElementType ADDITIVE_EXPRESSION = new SqlElementType("ADDITIVE_EXPRESSION");
    IElementType AGGREGATE_EXPRESSION = new SqlElementType("AGGREGATE_EXPRESSION");
    IElementType ALIAS_DECLARATION = new SqlElementType("ALIAS_DECLARATION");
    IElementType ALL_OR_ANY_EXPRESSION = new SqlElementType("ALL_OR_ANY_EXPRESSION");
    IElementType ASTERISK = new SqlElementType("ASTERISK");
    IElementType BETWEEN_EXPRESSION = new SqlElementType("BETWEEN_EXPRESSION");
    IElementType BOOLEAN_LITERAL = new SqlElementType("BOOLEAN_LITERAL");
    IElementType CASE_OPERAND = new SqlElementType("CASE_OPERAND");
    IElementType COALESCE_EXPRESSION = new SqlElementType("COALESCE_EXPRESSION");
    IElementType COLLECTION_MEMBER_DECLARATION = new SqlElementType("COLLECTION_MEMBER_DECLARATION");
    IElementType COLLECTION_MEMBER_EXPRESSION = new SqlElementType("COLLECTION_MEMBER_EXPRESSION");
    IElementType COMPARISON_EXPRESSION = new SqlElementType("COMPARISON_EXPRESSION");
    IElementType CONDITIONAL_AND_EXPRESSION = new SqlElementType("CONDITIONAL_AND_EXPRESSION");
    IElementType CONDITIONAL_NOT_EXPRESSION = new SqlElementType("CONDITIONAL_NOT_EXPRESSION");
    IElementType CONDITIONAL_OR_EXPRESSION = new SqlElementType("CONDITIONAL_OR_EXPRESSION");
    IElementType DATETIME_FUNCTION_EXPRESSION = new SqlElementType("DATETIME_FUNCTION_EXPRESSION");
    IElementType DATETIME_LITERAL = new SqlElementType("DATETIME_LITERAL");
    IElementType DELETE_CLAUSE = new SqlElementType("DELETE_CLAUSE");
    IElementType DELETE_STATEMENT = new SqlElementType("DELETE_STATEMENT");
    IElementType DERIVED_COLLECTION_MEMBER_DECLARATION = new SqlElementType("DERIVED_COLLECTION_MEMBER_DECLARATION");
    IElementType EMPTY_COLLECTION_COMPARISON_EXPRESSION = new SqlElementType("EMPTY_COLLECTION_COMPARISON_EXPRESSION");
    IElementType EXISTS_EXPRESSION = new SqlElementType("EXISTS_EXPRESSION");
    IElementType EXPRESSION = new SqlElementType("EXPRESSION");
    IElementType FROM_CLAUSE = new SqlElementType("FROM_CLAUSE");
    IElementType FROM_CLAUSE_REFERENCE_LIST = new SqlElementType("FROM_CLAUSE_REFERENCE_LIST");
    IElementType FUNCTIONS_RETURNING_NUMERICS_EXPRESSION = new SqlElementType("FUNCTIONS_RETURNING_NUMERICS_EXPRESSION");
    IElementType FUNCTION_ARG = new SqlElementType("FUNCTION_ARG");
    IElementType FUNCTION_INVOCATION_EXPRESSION = new SqlElementType("FUNCTION_INVOCATION_EXPRESSION");
    IElementType GENERAL_CASE_EXPRESSION = new SqlElementType("GENERAL_CASE_EXPRESSION");
    IElementType GROUPBY_CLAUSE = new SqlElementType("GROUPBY_CLAUSE");
    IElementType GROUPBY_ITEM = new SqlElementType("GROUPBY_ITEM");
    IElementType HAVING_CLAUSE = new SqlElementType("HAVING_CLAUSE");
    IElementType IDENTIFICATION_VARIABLE_DECLARATION = new SqlElementType("IDENTIFICATION_VARIABLE_DECLARATION");
    IElementType IDENTIFIER = new SqlElementType("IDENTIFIER");
    IElementType INPUT_PARAMETER_EXPRESSION = new SqlElementType("INPUT_PARAMETER_EXPRESSION");
    IElementType INSERT_FIELDS = new SqlElementType("INSERT_FIELDS");
    IElementType INSERT_STATEMENT = new SqlElementType("INSERT_STATEMENT");
    IElementType INSERT_TUPLE = new SqlElementType("INSERT_TUPLE");
    IElementType INSERT_VALUE = new SqlElementType("INSERT_VALUE");
    IElementType IN_EXPRESSION = new SqlElementType("IN_EXPRESSION");
    IElementType IN_ITEM = new SqlElementType("IN_ITEM");
    IElementType JOIN_CONDITION = new SqlElementType("JOIN_CONDITION");
    IElementType JOIN_EXPRESSION = new SqlElementType("JOIN_EXPRESSION");
    IElementType JOIN_SPEC = new SqlElementType("JOIN_SPEC");
    IElementType LIKE_EXPRESSION = new SqlElementType("LIKE_EXPRESSION");
    IElementType LIMIT_CLAUSE = new SqlElementType("LIMIT_CLAUSE");
    IElementType MULTIPLICATIVE_EXPRESSION = new SqlElementType("MULTIPLICATIVE_EXPRESSION");
    IElementType NULLIF_EXPRESSION = new SqlElementType("NULLIF_EXPRESSION");
    IElementType NULL_COMPARISON_EXPRESSION = new SqlElementType("NULL_COMPARISON_EXPRESSION");
    IElementType NULL_EXPRESSION = new SqlElementType("NULL_EXPRESSION");
    IElementType NUMERIC_LITERAL = new SqlElementType("NUMERIC_LITERAL");
    IElementType OBJECT_EXPRESSION = new SqlElementType("OBJECT_EXPRESSION");
    IElementType OFFSET_CLAUSE = new SqlElementType("OFFSET_CLAUSE");
    IElementType ORDERBY_CLAUSE = new SqlElementType("ORDERBY_CLAUSE");
    IElementType ORDERBY_ITEM = new SqlElementType("ORDERBY_ITEM");
    IElementType PAREN_EXPRESSION = new SqlElementType("PAREN_EXPRESSION");
    IElementType PATH_REFERENCE_EXPRESSION = new SqlElementType("PATH_REFERENCE_EXPRESSION");
    IElementType REFERENCE_EXPRESSION = new SqlElementType("REFERENCE_EXPRESSION");
    IElementType SELECT_CLAUSE = new SqlElementType("SELECT_CLAUSE");
    IElementType SELECT_ITEM = new SqlElementType("SELECT_ITEM");
    IElementType SELECT_STATEMENT = new SqlElementType("SELECT_STATEMENT");
    IElementType SIMPLE_CASE_EXPRESSION = new SqlElementType("SIMPLE_CASE_EXPRESSION");
    IElementType SIMPLE_SELECT_CLAUSE = new SqlElementType("SIMPLE_SELECT_CLAUSE");
    IElementType SIMPLE_WHEN_CLAUSE = new SqlElementType("SIMPLE_WHEN_CLAUSE");
    IElementType STATEMENT = new SqlElementType("STATEMENT");
    IElementType STRING_FUNCTION_EXPRESSION = new SqlElementType("STRING_FUNCTION_EXPRESSION");
    IElementType STRING_LITERAL = new SqlElementType("STRING_LITERAL");
    IElementType SUBQUERY = new SqlElementType("SUBQUERY");
    IElementType SUBQUERY_EXPRESSION = new SqlElementType("SUBQUERY_EXPRESSION");
    IElementType SUBQUERY_FROM_CLAUSE = new SqlElementType("SUBQUERY_FROM_CLAUSE");
    IElementType TABLE_EXPRESSION = new SqlElementType("TABLE_EXPRESSION");
    IElementType TABLE_EXPRESSION_JOIN_DECLARATION = new SqlElementType("TABLE_EXPRESSION_JOIN_DECLARATION");
    IElementType TABLE_NAME_REF = new SqlElementType("TABLE_NAME_REF");
    IElementType TRIM_SPECIFICATION = new SqlElementType("TRIM_SPECIFICATION");
    IElementType TYPE_EXPRESSION = new SqlElementType("TYPE_EXPRESSION");
    IElementType TYPE_LITERAL = new SqlElementType("TYPE_LITERAL");
    IElementType UNARY_ARITHMETIC_EXPRESSION = new SqlElementType("UNARY_ARITHMETIC_EXPRESSION");
    IElementType UPDATE_CLAUSE = new SqlElementType("UPDATE_CLAUSE");
    IElementType UPDATE_ITEM = new SqlElementType("UPDATE_ITEM");
    IElementType UPDATE_STATEMENT = new SqlElementType("UPDATE_STATEMENT");
    IElementType WHEN_CLAUSE = new SqlElementType("WHEN_CLAUSE");
    IElementType WHERE_CLAUSE = new SqlElementType("WHERE_CLAUSE");

    IElementType ABS = new SqlTokenType("ABS");
    IElementType ALL = new SqlTokenType("ALL");
    IElementType AND = new SqlTokenType("AND");
    IElementType ANY = new SqlTokenType("ANY");
    IElementType AS = new SqlTokenType("AS");
    IElementType ASC = new SqlTokenType("ASC");
    IElementType AVG = new SqlTokenType("AVG");
    IElementType BETWEEN = new SqlTokenType("BETWEEN");
    IElementType BOOLEAN = new SqlTokenType("BOOLEAN");
    IElementType BOTH = new SqlTokenType("BOTH");
    IElementType BY = new SqlTokenType("BY");
    IElementType CASE = new SqlTokenType("CASE");
    IElementType COALESCE = new SqlTokenType("COALESCE");
    IElementType COLON = new SqlTokenType(":");
    IElementType COMMA = new SqlTokenType(",");
    IElementType CONCAT = new SqlTokenType("CONCAT");
    IElementType COUNT = new SqlTokenType("COUNT");
    IElementType CURRENT_DATE = new SqlTokenType("CURRENT_DATE");
    IElementType CURRENT_TIME = new SqlTokenType("CURRENT_TIME");
    IElementType CURRENT_TIMESTAMP = new SqlTokenType("CURRENT_TIMESTAMP");
    IElementType DATE = new SqlTokenType("DATE");
    IElementType DATETIME = new SqlTokenType("datetime");
    IElementType DELETE = new SqlTokenType("DELETE");
    IElementType DESC = new SqlTokenType("DESC");
    IElementType DISTINCT = new SqlTokenType("DISTINCT");
    IElementType DIV = new SqlTokenType("/");
    IElementType DOT = new SqlTokenType(".");
    IElementType ELSE = new SqlTokenType("ELSE");
    IElementType EMPTY = new SqlTokenType("EMPTY");
    IElementType END = new SqlTokenType("END");
    IElementType ENTRY = new SqlTokenType("ENTRY");
    IElementType EQ = new SqlTokenType("=");
    IElementType ESCAPE = new SqlTokenType("ESCAPE");
    IElementType EXISTS = new SqlTokenType("EXISTS");
    IElementType FROM = new SqlTokenType("FROM");
    IElementType FUNCTION = new SqlTokenType("FUNCTION");
    IElementType GROUP = new SqlTokenType("GROUP");
    IElementType GT = new SqlTokenType(">");
    IElementType GTE = new SqlTokenType(">=");
    IElementType HAVING = new SqlTokenType("HAVING");
    IElementType ID = new SqlTokenType("id");
    IElementType IN = new SqlTokenType("IN");
    IElementType INDEX = new SqlTokenType("INDEX");
    IElementType INNER = new SqlTokenType("INNER");
    IElementType INSERT = new SqlTokenType("INSERT");
    IElementType INTEGER = new SqlTokenType("INTEGER");
    IElementType INTO = new SqlTokenType("INTO");
    IElementType IS = new SqlTokenType("IS");
    IElementType JOIN = new SqlTokenType("JOIN");
    IElementType KEY = new SqlTokenType("KEY");
    IElementType LEADING = new SqlTokenType("LEADING");
    IElementType LEFT = new SqlTokenType("LEFT");
    IElementType LENGTH = new SqlTokenType("LENGTH");
    IElementType LIKE = new SqlTokenType("LIKE");
    IElementType LIMIT = new SqlTokenType("LIMIT");
    IElementType LOCATE = new SqlTokenType("LOCATE");
    IElementType LOWER = new SqlTokenType("LOWER");
    IElementType LPAREN = new SqlTokenType("(");
    IElementType LT = new SqlTokenType("<");
    IElementType LTE = new SqlTokenType("<=");
    IElementType MAX = new SqlTokenType("MAX");
    IElementType MEMBER = new SqlTokenType("MEMBER");
    IElementType MIN = new SqlTokenType("MIN");
    IElementType MINUS = new SqlTokenType("-");
    IElementType MOD = new SqlTokenType("MOD");
    IElementType MUL = new SqlTokenType("*");
    IElementType NAMED_INPUT_PARAMETER = new SqlTokenType("named_input_parameter");
    IElementType NEQ = new SqlTokenType("<>");
    IElementType NOT = new SqlTokenType("NOT");
    IElementType NULL = new SqlTokenType("NULL");
    IElementType NULLIF = new SqlTokenType("NULLIF");
    IElementType NUMERIC = new SqlTokenType("numeric");
    IElementType NUMERIC_INPUT_PARAMETER = new SqlTokenType("numeric_input_parameter");
    IElementType OBJECT = new SqlTokenType("OBJECT");
    IElementType OF = new SqlTokenType("OF");
    IElementType OFFSET = new SqlTokenType("OFFSET");
    IElementType ON = new SqlTokenType("ON");
    IElementType OR = new SqlTokenType("OR");
    IElementType ORDER = new SqlTokenType("ORDER");
    IElementType OUTER = new SqlTokenType("OUTER");
    IElementType PLUS = new SqlTokenType("+");
    IElementType RBRACE = new SqlTokenType("}");
    IElementType RPAREN = new SqlTokenType(")");
    IElementType SELECT = new SqlTokenType("SELECT");
    IElementType SEMICOLON = new SqlTokenType(";");
    IElementType SET = new SqlTokenType("SET");
    IElementType SIZE = new SqlTokenType("SIZE");
    IElementType SOME = new SqlTokenType("SOME");
    IElementType SQRT = new SqlTokenType("SQRT");
    IElementType STRING = new SqlTokenType("STRING");
    IElementType SUBSTRING = new SqlTokenType("SUBSTRING");
    IElementType SUM = new SqlTokenType("SUM");
    IElementType THEN = new SqlTokenType("THEN");
    IElementType TIME = new SqlTokenType("TIME");
    IElementType TIMESTAMP = new SqlTokenType("TIMESTAMP");
    IElementType TRAILING = new SqlTokenType("TRAILING");
    IElementType TRIM = new SqlTokenType("TRIM");
    IElementType TYPE = new SqlTokenType("TYPE");
    IElementType UPDATE = new SqlTokenType("UPDATE");
    IElementType UPPER = new SqlTokenType("UPPER");
    IElementType VALUE = new SqlTokenType("VALUE");
    IElementType VALUES = new SqlTokenType("VALUES");
    IElementType WHEN = new SqlTokenType("WHEN");
    IElementType WHERE = new SqlTokenType("WHERE");

    class Factory {
        public static PsiElement createElement(ASTNode node) {
            IElementType type = node.getElementType();
            if (type == ADDITIVE_EXPRESSION) {
                return new SqlAdditiveExpressionImpl(node);
            } else if (type == AGGREGATE_EXPRESSION) {
                return new SqlAggregateExpressionImpl(node);
            } else if (type == ALIAS_DECLARATION) {
                return new SqlAliasDeclarationImpl(node);
            } else if (type == ALL_OR_ANY_EXPRESSION) {
                return new SqlAllOrAnyExpressionImpl(node);
            } else if (type == ASTERISK) {
                return new SqlAsteriskImpl(node);
            } else if (type == BETWEEN_EXPRESSION) {
                return new SqlBetweenExpressionImpl(node);
            } else if (type == BOOLEAN_LITERAL) {
                return new SqlBooleanLiteralImpl(node);
            } else if (type == CASE_OPERAND) {
                return new SqlCaseOperandImpl(node);
            } else if (type == COALESCE_EXPRESSION) {
                return new SqlCoalesceExpressionImpl(node);
            } else if (type == COLLECTION_MEMBER_DECLARATION) {
                return new SqlCollectionMemberDeclarationImpl(node);
            } else if (type == COLLECTION_MEMBER_EXPRESSION) {
                return new SqlCollectionMemberExpressionImpl(node);
            } else if (type == COMPARISON_EXPRESSION) {
                return new SqlComparisonExpressionImpl(node);
            } else if (type == CONDITIONAL_AND_EXPRESSION) {
                return new SqlConditionalAndExpressionImpl(node);
            } else if (type == CONDITIONAL_NOT_EXPRESSION) {
                return new SqlConditionalNotExpressionImpl(node);
            } else if (type == CONDITIONAL_OR_EXPRESSION) {
                return new SqlConditionalOrExpressionImpl(node);
            } else if (type == DATETIME_FUNCTION_EXPRESSION) {
                return new SqlDatetimeFunctionExpressionImpl(node);
            } else if (type == DATETIME_LITERAL) {
                return new SqlDatetimeLiteralImpl(node);
            } else if (type == DELETE_CLAUSE) {
                return new SqlDeleteClauseImpl(node);
            } else if (type == DELETE_STATEMENT) {
                return new SqlDeleteStatementImpl(node);
            } else if (type == DERIVED_COLLECTION_MEMBER_DECLARATION) {
                return new SqlDerivedCollectionMemberDeclarationImpl(node);
            } else if (type == EMPTY_COLLECTION_COMPARISON_EXPRESSION) {
                return new SqlEmptyCollectionComparisonExpressionImpl(node);
            } else if (type == EXISTS_EXPRESSION) {
                return new SqlExistsExpressionImpl(node);
            } else if (type == FROM_CLAUSE) {
                return new SqlFromClauseImpl(node);
            } else if (type == FROM_CLAUSE_REFERENCE_LIST) {
                return new SqlFromClauseReferenceListImpl(node);
            } else if (type == FUNCTIONS_RETURNING_NUMERICS_EXPRESSION) {
                return new SqlFunctionsReturningNumericsExpressionImpl(node);
            } else if (type == FUNCTION_ARG) {
                return new SqlFunctionArgImpl(node);
            } else if (type == FUNCTION_INVOCATION_EXPRESSION) {
                return new SqlFunctionInvocationExpressionImpl(node);
            } else if (type == GENERAL_CASE_EXPRESSION) {
                return new SqlGeneralCaseExpressionImpl(node);
            } else if (type == GROUPBY_CLAUSE) {
                return new SqlGroupbyClauseImpl(node);
            } else if (type == GROUPBY_ITEM) {
                return new SqlGroupbyItemImpl(node);
            } else if (type == HAVING_CLAUSE) {
                return new SqlHavingClauseImpl(node);
            } else if (type == IDENTIFICATION_VARIABLE_DECLARATION) {
                return new SqlIdentificationVariableDeclarationImpl(node);
            } else if (type == IDENTIFIER) {
                return new SqlIdentifierImpl(node);
            } else if (type == INPUT_PARAMETER_EXPRESSION) {
                return new SqlInputParameterExpressionImpl(node);
            } else if (type == INSERT_FIELDS) {
                return new SqlInsertFieldsImpl(node);
            } else if (type == INSERT_STATEMENT) {
                return new SqlInsertStatementImpl(node);
            } else if (type == INSERT_TUPLE) {
                return new SqlInsertTupleImpl(node);
            } else if (type == INSERT_VALUE) {
                return new SqlInsertValueImpl(node);
            } else if (type == IN_EXPRESSION) {
                return new SqlInExpressionImpl(node);
            } else if (type == IN_ITEM) {
                return new SqlInItemImpl(node);
            } else if (type == JOIN_CONDITION) {
                return new SqlJoinConditionImpl(node);
            } else if (type == JOIN_EXPRESSION) {
                return new SqlJoinExpressionImpl(node);
            } else if (type == JOIN_SPEC) {
                return new SqlJoinSpecImpl(node);
            } else if (type == LIKE_EXPRESSION) {
                return new SqlLikeExpressionImpl(node);
            } else if (type == LIMIT_CLAUSE) {
                return new SqlLimitClauseImpl(node);
            } else if (type == MULTIPLICATIVE_EXPRESSION) {
                return new SqlMultiplicativeExpressionImpl(node);
            } else if (type == NULLIF_EXPRESSION) {
                return new SqlNullifExpressionImpl(node);
            } else if (type == NULL_COMPARISON_EXPRESSION) {
                return new SqlNullComparisonExpressionImpl(node);
            } else if (type == NULL_EXPRESSION) {
                return new SqlNullExpressionImpl(node);
            } else if (type == NUMERIC_LITERAL) {
                return new SqlNumericLiteralImpl(node);
            } else if (type == OBJECT_EXPRESSION) {
                return new SqlObjectExpressionImpl(node);
            } else if (type == OFFSET_CLAUSE) {
                return new SqlOffsetClauseImpl(node);
            } else if (type == ORDERBY_CLAUSE) {
                return new SqlOrderbyClauseImpl(node);
            } else if (type == ORDERBY_ITEM) {
                return new SqlOrderbyItemImpl(node);
            } else if (type == PAREN_EXPRESSION) {
                return new SqlParenExpressionImpl(node);
            } else if (type == PATH_REFERENCE_EXPRESSION) {
                return new SqlPathReferenceExpressionImpl(node);
            } else if (type == REFERENCE_EXPRESSION) {
                return new SqlReferenceExpressionImpl(node);
            } else if (type == SELECT_CLAUSE) {
                return new SqlSelectClauseImpl(node);
            } else if (type == SELECT_ITEM) {
                return new SqlSelectItemImpl(node);
            } else if (type == SELECT_STATEMENT) {
                return new SqlSelectStatementImpl(node);
            } else if (type == SIMPLE_CASE_EXPRESSION) {
                return new SqlSimpleCaseExpressionImpl(node);
            } else if (type == SIMPLE_SELECT_CLAUSE) {
                return new SqlSimpleSelectClauseImpl(node);
            } else if (type == SIMPLE_WHEN_CLAUSE) {
                return new SqlSimpleWhenClauseImpl(node);
            } else if (type == STATEMENT) {
                return new SqlStatementImpl(node);
            } else if (type == STRING_FUNCTION_EXPRESSION) {
                return new SqlStringFunctionExpressionImpl(node);
            } else if (type == STRING_LITERAL) {
                return new SqlStringLiteralImpl(node);
            } else if (type == SUBQUERY) {
                return new SqlSubqueryImpl(node);
            } else if (type == SUBQUERY_EXPRESSION) {
                return new SqlSubqueryExpressionImpl(node);
            } else if (type == SUBQUERY_FROM_CLAUSE) {
                return new SqlSubqueryFromClauseImpl(node);
            } else if (type == TABLE_EXPRESSION) {
                return new SqlTableExpressionImpl(node);
            } else if (type == TABLE_EXPRESSION_JOIN_DECLARATION) {
                return new SqlTableExpressionJoinDeclarationImpl(node);
            } else if (type == TABLE_NAME_REF) {
                return new SqlTableNameRefImpl(node);
            } else if (type == TRIM_SPECIFICATION) {
                return new SqlTrimSpecificationImpl(node);
            } else if (type == TYPE_EXPRESSION) {
                return new SqlTypeExpressionImpl(node);
            } else if (type == TYPE_LITERAL) {
                return new SqlTypeLiteralImpl(node);
            } else if (type == UNARY_ARITHMETIC_EXPRESSION) {
                return new SqlUnaryArithmeticExpressionImpl(node);
            } else if (type == UPDATE_CLAUSE) {
                return new SqlUpdateClauseImpl(node);
            } else if (type == UPDATE_ITEM) {
                return new SqlUpdateItemImpl(node);
            } else if (type == UPDATE_STATEMENT) {
                return new SqlUpdateStatementImpl(node);
            } else if (type == WHEN_CLAUSE) {
                return new SqlWhenClauseImpl(node);
            } else if (type == WHERE_CLAUSE) {
                return new SqlWhereClauseImpl(node);
            }
            throw new AssertionError("Unknown element type: " + type);
        }
    }
}
