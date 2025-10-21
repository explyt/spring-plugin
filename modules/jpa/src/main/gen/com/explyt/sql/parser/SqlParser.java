// This is a generated file. Not intended for manual editing.
package com.explyt.sql.parser;

import com.intellij.lang.ASTNode;
import com.intellij.lang.LightPsiParser;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilder.Marker;
import com.intellij.lang.PsiParser;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;

import static com.explyt.sql.parser.SqlParserUtil.*;
import static com.explyt.sql.psi.SqlTypes.*;

@SuppressWarnings({"SimplifiableIfStatement", "UnusedAssignment"})
public class SqlParser implements PsiParser, LightPsiParser {

    public ASTNode parse(IElementType t, PsiBuilder b) {
        parseLight(t, b);
        return b.getTreeBuilt();
    }

    public void parseLight(IElementType t, PsiBuilder b) {
        boolean r;
        b = adapt_builder_(t, b, this, EXTENDS_SETS_);
        Marker m = enter_section_(b, 0, _COLLAPSE_, null);
        r = parse_root_(t, b);
        exit_section_(b, 0, m, t, r, true, TRUE_CONDITION);
    }

    protected boolean parse_root_(IElementType t, PsiBuilder b) {
        return parse_root_(t, b, 0);
    }

    static boolean parse_root_(IElementType t, PsiBuilder b, int l) {
        return file(b, l + 1);
    }

    public static final TokenSet[] EXTENDS_SETS_ = new TokenSet[]{
            create_token_set_(FROM_CLAUSE_REFERENCE_LIST, SUBQUERY_FROM_CLAUSE),
            create_token_set_(ADDITIVE_EXPRESSION, AGGREGATE_EXPRESSION, ALL_OR_ANY_EXPRESSION, BETWEEN_EXPRESSION,
                    BOOLEAN_LITERAL, COALESCE_EXPRESSION, COLLECTION_MEMBER_EXPRESSION, COMPARISON_EXPRESSION,
                    CONDITIONAL_AND_EXPRESSION, CONDITIONAL_NOT_EXPRESSION, CONDITIONAL_OR_EXPRESSION, DATETIME_FUNCTION_EXPRESSION,
                    DATETIME_LITERAL, EMPTY_COLLECTION_COMPARISON_EXPRESSION, EXISTS_EXPRESSION, EXPRESSION,
                    FUNCTIONS_RETURNING_NUMERICS_EXPRESSION, FUNCTION_INVOCATION_EXPRESSION, GENERAL_CASE_EXPRESSION, INPUT_PARAMETER_EXPRESSION,
                    IN_EXPRESSION, JOIN_EXPRESSION, LIKE_EXPRESSION, MULTIPLICATIVE_EXPRESSION,
                    NULLIF_EXPRESSION, NULL_COMPARISON_EXPRESSION, NULL_EXPRESSION, NUMERIC_LITERAL,
                    OBJECT_EXPRESSION, PAREN_EXPRESSION, PATH_REFERENCE_EXPRESSION, REFERENCE_EXPRESSION,
                    SIMPLE_CASE_EXPRESSION, STRING_FUNCTION_EXPRESSION, STRING_LITERAL, SUBQUERY_EXPRESSION,
                    TABLE_EXPRESSION, TYPE_EXPRESSION, UNARY_ARITHMETIC_EXPRESSION),
    };

    /* ********************************************************** */
    // [AS] identifier
    public static boolean alias_declaration(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "alias_declaration")) return false;
        boolean r;
        Marker m = enter_section_(b, l, _NONE_, ALIAS_DECLARATION, "<alias declaration>");
        r = alias_declaration_0(b, l + 1);
        r = r && identifier(b, l + 1);
        exit_section_(b, l, m, r, false, null);
        return r;
    }

    // [AS]
    private static boolean alias_declaration_0(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "alias_declaration_0")) return false;
        consumeToken(b, AS);
        return true;
    }

    /* ********************************************************** */
    // '*'
    public static boolean asterisk(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "asterisk")) return false;
        if (!nextTokenIs(b, MUL)) return false;
        boolean r;
        Marker m = enter_section_(b);
        r = consumeToken(b, MUL);
        exit_section_(b, m, ASTERISK, r);
        return r;
    }

    /* ********************************************************** */
    // input_parameter_expression
    //     | case_expression
    //     | boolean_literal
    //     | function_invocation_expression
    //     | reference_expression
    static boolean boolean_expression(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "boolean_expression")) return false;
        boolean r;
        r = input_parameter_expression(b, l + 1);
        if (!r) r = expression(b, l + 1, 8);
        if (!r) r = boolean_literal(b, l + 1);
        if (!r) r = function_invocation_expression(b, l + 1);
        if (!r) r = reference_expression(b, l + 1);
        return r;
    }

    /* ********************************************************** */
    // reference_expression | type_expression
    public static boolean case_operand(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "case_operand")) return false;
        boolean r;
        Marker m = enter_section_(b, l, _NONE_, CASE_OPERAND, "<case operand>");
        r = reference_expression(b, l + 1);
        if (!r) r = type_expression(b, l + 1);
        exit_section_(b, l, m, r, false, null);
        return r;
    }

    /* ********************************************************** */
    // IN '(' reference_expression ')' alias_declaration
    public static boolean collection_member_declaration(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "collection_member_declaration")) return false;
        if (!nextTokenIs(b, IN)) return false;
        boolean r;
        Marker m = enter_section_(b);
        r = consumeTokens(b, 0, IN, LPAREN);
        r = r && reference_expression(b, l + 1);
        r = r && consumeToken(b, RPAREN);
        r = r && alias_declaration(b, l + 1);
        exit_section_(b, m, COLLECTION_MEMBER_DECLARATION, r);
        return r;
    }

    /* ********************************************************** */
    // '=' | '>' | '>=' | '<' | '<=' | '<>'
    static boolean comparison_operator(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "comparison_operator")) return false;
        boolean r;
        r = consumeToken(b, EQ);
        if (!r) r = consumeToken(b, GT);
        if (!r) r = consumeToken(b, GTE);
        if (!r) r = consumeToken(b, LT);
        if (!r) r = consumeToken(b, LTE);
        if (!r) r = consumeToken(b, NEQ);
        return r;
    }

    /* ********************************************************** */
    // input_parameter_expression
    //     | datetime_function_expression
    //     | aggregate_expression
    //     | case_expression
    //     | datetime_literal
    //     | function_invocation_expression
    //     | reference_expression
    static boolean datetime_expression(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "datetime_expression")) return false;
        boolean r;
        r = input_parameter_expression(b, l + 1);
        if (!r) r = datetime_function_expression(b, l + 1);
        if (!r) r = aggregate_expression(b, l + 1);
        if (!r) r = expression(b, l + 1, 8);
        if (!r) r = datetime_literal(b, l + 1);
        if (!r) r = function_invocation_expression(b, l + 1);
        if (!r) r = reference_expression(b, l + 1);
        return r;
    }

    /* ********************************************************** */
    // CURRENT_DATE
    //     | CURRENT_TIME
    //     | CURRENT_TIMESTAMP
    static boolean datetime_function(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "datetime_function")) return false;
        boolean r;
        r = consumeToken(b, CURRENT_DATE);
        if (!r) r = consumeToken(b, CURRENT_TIME);
        if (!r) r = consumeToken(b, CURRENT_TIMESTAMP);
        return r;
    }

    /* ********************************************************** */
    // DELETE FROM table_name_ref
    public static boolean delete_clause(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "delete_clause")) return false;
        if (!nextTokenIs(b, DELETE)) return false;
        boolean r;
        Marker m = enter_section_(b);
        r = consumeTokens(b, 0, DELETE, FROM);
        r = r && table_name_ref(b, l + 1);
        exit_section_(b, m, DELETE_CLAUSE, r);
        return r;
    }

    /* ********************************************************** */
    // delete_clause [where_clause]
    public static boolean delete_statement(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "delete_statement")) return false;
        if (!nextTokenIs(b, DELETE)) return false;
        boolean r;
        Marker m = enter_section_(b);
        r = delete_clause(b, l + 1);
        r = r && delete_statement_1(b, l + 1);
        exit_section_(b, m, DELETE_STATEMENT, r);
        return r;
    }

    // [where_clause]
    private static boolean delete_statement_1(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "delete_statement_1")) return false;
        where_clause(b, l + 1);
        return true;
    }

    /* ********************************************************** */
    // IN reference_expression
    public static boolean derived_collection_member_declaration(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "derived_collection_member_declaration")) return false;
        if (!nextTokenIs(b, IN)) return false;
        boolean r;
        Marker m = enter_section_(b);
        r = consumeToken(b, IN);
        r = r && reference_expression(b, l + 1);
        exit_section_(b, m, DERIVED_COLLECTION_MEMBER_DECLARATION, r);
        return r;
    }

    /* ********************************************************** */
    // reference_expression
    //     | identifier
    //     | input_parameter_expression
    //     | literal
    static boolean entity_or_value_expression(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "entity_or_value_expression")) return false;
        boolean r;
        r = reference_expression(b, l + 1);
        if (!r) r = identifier(b, l + 1);
        if (!r) r = input_parameter_expression(b, l + 1);
        if (!r) r = literal(b, l + 1);
        return r;
    }

    /* ********************************************************** */
    // semicolon_delimited_statements | statement
    static boolean file(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "file")) return false;
        boolean r;
        Marker m = enter_section_(b);
        r = semicolon_delimited_statements(b, l + 1);
        if (!r) r = statement(b, l + 1);
        exit_section_(b, m, null, r);
        return r;
    }

    /* ********************************************************** */
    // FROM from_clause_reference_list
    public static boolean from_clause(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "from_clause")) return false;
        if (!nextTokenIs(b, FROM)) return false;
        boolean r;
        Marker m = enter_section_(b);
        r = consumeToken(b, FROM);
        r = r && from_clause_reference_list(b, l + 1);
        exit_section_(b, m, FROM_CLAUSE, r);
        return r;
    }

    /* ********************************************************** */
    // table_expression_join_declaration {',' table_expression_join_declaration }*
    public static boolean from_clause_reference_list(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "from_clause_reference_list")) return false;
        boolean r;
        Marker m = enter_section_(b, l, _NONE_, FROM_CLAUSE_REFERENCE_LIST, "<from clause reference list>");
        r = table_expression_join_declaration(b, l + 1);
        r = r && from_clause_reference_list_1(b, l + 1);
        exit_section_(b, l, m, r, false, null);
        return r;
    }

    // {',' table_expression_join_declaration }*
    private static boolean from_clause_reference_list_1(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "from_clause_reference_list_1")) return false;
        while (true) {
            int c = current_position_(b);
            if (!from_clause_reference_list_1_0(b, l + 1)) break;
            if (!empty_element_parsed_guard_(b, "from_clause_reference_list_1", c)) break;
        }
        return true;
    }

    // ',' table_expression_join_declaration
    private static boolean from_clause_reference_list_1_0(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "from_clause_reference_list_1_0")) return false;
        boolean r;
        Marker m = enter_section_(b);
        r = consumeToken(b, COMMA);
        r = r && table_expression_join_declaration(b, l + 1);
        exit_section_(b, m, null, r);
        return r;
    }

    /* ********************************************************** */
    // literal_group
    //     | scalar_expression
    public static boolean function_arg(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "function_arg")) return false;
        boolean r;
        Marker m = enter_section_(b, l, _NONE_, FUNCTION_ARG, "<function arg>");
        r = expression(b, l + 1, 3);
        if (!r) r = scalar_expression(b, l + 1);
        exit_section_(b, l, m, r, false, null);
        return r;
    }

    /* ********************************************************** */
    // GROUP BY groupby_item {',' groupby_item}*
    public static boolean groupby_clause(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "groupby_clause")) return false;
        if (!nextTokenIs(b, GROUP)) return false;
        boolean r;
        Marker m = enter_section_(b);
        r = consumeTokens(b, 0, GROUP, BY);
        r = r && groupby_item(b, l + 1);
        r = r && groupby_clause_3(b, l + 1);
        exit_section_(b, m, GROUPBY_CLAUSE, r);
        return r;
    }

    // {',' groupby_item}*
    private static boolean groupby_clause_3(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "groupby_clause_3")) return false;
        while (true) {
            int c = current_position_(b);
            if (!groupby_clause_3_0(b, l + 1)) break;
            if (!empty_element_parsed_guard_(b, "groupby_clause_3", c)) break;
        }
        return true;
    }

    // ',' groupby_item
    private static boolean groupby_clause_3_0(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "groupby_clause_3_0")) return false;
        boolean r;
        Marker m = enter_section_(b);
        r = consumeToken(b, COMMA);
        r = r && groupby_item(b, l + 1);
        exit_section_(b, m, null, r);
        return r;
    }

    /* ********************************************************** */
    // reference_expression | identifier
    public static boolean groupby_item(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "groupby_item")) return false;
        boolean r;
        Marker m = enter_section_(b, l, _NONE_, GROUPBY_ITEM, "<groupby item>");
        r = reference_expression(b, l + 1);
        if (!r) r = identifier(b, l + 1);
        exit_section_(b, l, m, r, false, null);
        return r;
    }

    /* ********************************************************** */
    // HAVING expression
    public static boolean having_clause(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "having_clause")) return false;
        if (!nextTokenIs(b, HAVING)) return false;
        boolean r;
        Marker m = enter_section_(b);
        r = consumeToken(b, HAVING);
        r = r && expression(b, l + 1, -1);
        exit_section_(b, m, HAVING_CLAUSE, r);
        return r;
    }

    /* ********************************************************** */
    // table_name_ref { join_expression }*
    public static boolean identification_variable_declaration(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "identification_variable_declaration")) return false;
        boolean r;
        Marker m = enter_section_(b, l, _NONE_, IDENTIFICATION_VARIABLE_DECLARATION, "<identification variable declaration>");
        r = table_name_ref(b, l + 1);
        r = r && identification_variable_declaration_1(b, l + 1);
        exit_section_(b, l, m, r, false, null);
        return r;
    }

    // { join_expression }*
    private static boolean identification_variable_declaration_1(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "identification_variable_declaration_1")) return false;
        while (true) {
            int c = current_position_(b);
            if (!identification_variable_declaration_1_0(b, l + 1)) break;
            if (!empty_element_parsed_guard_(b, "identification_variable_declaration_1", c)) break;
        }
        return true;
    }

    // { join_expression }
    private static boolean identification_variable_declaration_1_0(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "identification_variable_declaration_1_0")) return false;
        boolean r;
        Marker m = enter_section_(b);
        r = join_expression(b, l + 1);
        exit_section_(b, m, null, r);
        return r;
    }

    /* ********************************************************** */
    // id | identifier_like_keyword
    public static boolean identifier(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "identifier")) return false;
        boolean r;
        Marker m = enter_section_(b, l, _NONE_, IDENTIFIER, "<identifier>");
        r = consumeToken(b, ID);
        if (!r) r = identifier_like_keyword(b, l + 1);
        exit_section_(b, l, m, r, false, null);
        return r;
    }

    /* ********************************************************** */
    // VALUE
    //     | KEY
    //     | TYPE
    //     | ENTRY
    //     | DATE
    //     | CURRENT_DATE
    //     | CURRENT_TIME
    //     | CURRENT_TIMESTAMP
    //     | TIME
    //     | TIMESTAMP
    //     | ORDER
    //     | OF
    static boolean identifier_like_keyword(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "identifier_like_keyword")) return false;
        boolean r;
        r = consumeToken(b, VALUE);
        if (!r) r = consumeToken(b, KEY);
        if (!r) r = consumeToken(b, TYPE);
        if (!r) r = consumeToken(b, ENTRY);
        if (!r) r = consumeToken(b, DATE);
        if (!r) r = consumeToken(b, CURRENT_DATE);
        if (!r) r = consumeToken(b, CURRENT_TIME);
        if (!r) r = consumeToken(b, CURRENT_TIMESTAMP);
        if (!r) r = consumeToken(b, TIME);
        if (!r) r = consumeToken(b, TIMESTAMP);
        if (!r) r = consumeToken(b, ORDER);
        if (!r) r = consumeToken(b, OF);
        return r;
    }

    /* ********************************************************** */
    // '(' in_item {',' in_item}* ')'
    static boolean in_collection(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "in_collection")) return false;
        if (!nextTokenIs(b, LPAREN)) return false;
        boolean r;
        Marker m = enter_section_(b);
        r = consumeToken(b, LPAREN);
        r = r && in_item(b, l + 1);
        r = r && in_collection_2(b, l + 1);
        r = r && consumeToken(b, RPAREN);
        exit_section_(b, m, null, r);
        return r;
    }

    // {',' in_item}*
    private static boolean in_collection_2(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "in_collection_2")) return false;
        while (true) {
            int c = current_position_(b);
            if (!in_collection_2_0(b, l + 1)) break;
            if (!empty_element_parsed_guard_(b, "in_collection_2", c)) break;
        }
        return true;
    }

    // ',' in_item
    private static boolean in_collection_2_0(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "in_collection_2_0")) return false;
        boolean r;
        Marker m = enter_section_(b);
        r = consumeToken(b, COMMA);
        r = r && in_item(b, l + 1);
        exit_section_(b, m, null, r);
        return r;
    }

    /* ********************************************************** */
    // literal | input_parameter_expression
    public static boolean in_item(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "in_item")) return false;
        boolean r;
        Marker m = enter_section_(b, l, _NONE_, IN_ITEM, "<in item>");
        r = literal(b, l + 1);
        if (!r) r = input_parameter_expression(b, l + 1);
        exit_section_(b, l, m, r, false, null);
        return r;
    }

    /* ********************************************************** */
    // '(' identifier {',' identifier}* ')'
    public static boolean insert_fields(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "insert_fields")) return false;
        if (!nextTokenIs(b, LPAREN)) return false;
        boolean r;
        Marker m = enter_section_(b);
        r = consumeToken(b, LPAREN);
        r = r && identifier(b, l + 1);
        r = r && insert_fields_2(b, l + 1);
        r = r && consumeToken(b, RPAREN);
        exit_section_(b, m, INSERT_FIELDS, r);
        return r;
    }

    // {',' identifier}*
    private static boolean insert_fields_2(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "insert_fields_2")) return false;
        while (true) {
            int c = current_position_(b);
            if (!insert_fields_2_0(b, l + 1)) break;
            if (!empty_element_parsed_guard_(b, "insert_fields_2", c)) break;
        }
        return true;
    }

    // ',' identifier
    private static boolean insert_fields_2_0(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "insert_fields_2_0")) return false;
        boolean r;
        Marker m = enter_section_(b);
        r = consumeToken(b, COMMA);
        r = r && identifier(b, l + 1);
        exit_section_(b, m, null, r);
        return r;
    }

    /* ********************************************************** */
    // INSERT [INTO] table_name_ref insert_fields (select_statement | values_list)
    public static boolean insert_statement(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "insert_statement")) return false;
        if (!nextTokenIs(b, INSERT)) return false;
        boolean r;
        Marker m = enter_section_(b);
        r = consumeToken(b, INSERT);
        r = r && insert_statement_1(b, l + 1);
        r = r && table_name_ref(b, l + 1);
        r = r && insert_fields(b, l + 1);
        r = r && insert_statement_4(b, l + 1);
        exit_section_(b, m, INSERT_STATEMENT, r);
        return r;
    }

    // [INTO]
    private static boolean insert_statement_1(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "insert_statement_1")) return false;
        consumeToken(b, INTO);
        return true;
    }

    // select_statement | values_list
    private static boolean insert_statement_4(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "insert_statement_4")) return false;
        boolean r;
        r = select_statement(b, l + 1);
        if (!r) r = values_list(b, l + 1);
        return r;
    }

    /* ********************************************************** */
    // '(' insert_value { ',' insert_value }* ')'
    public static boolean insert_tuple(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "insert_tuple")) return false;
        if (!nextTokenIs(b, LPAREN)) return false;
        boolean r;
        Marker m = enter_section_(b);
        r = consumeToken(b, LPAREN);
        r = r && insert_value(b, l + 1);
        r = r && insert_tuple_2(b, l + 1);
        r = r && consumeToken(b, RPAREN);
        exit_section_(b, m, INSERT_TUPLE, r);
        return r;
    }

    // { ',' insert_value }*
    private static boolean insert_tuple_2(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "insert_tuple_2")) return false;
        while (true) {
            int c = current_position_(b);
            if (!insert_tuple_2_0(b, l + 1)) break;
            if (!empty_element_parsed_guard_(b, "insert_tuple_2", c)) break;
        }
        return true;
    }

    // ',' insert_value
    private static boolean insert_tuple_2_0(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "insert_tuple_2_0")) return false;
        boolean r;
        Marker m = enter_section_(b);
        r = consumeToken(b, COMMA);
        r = r && insert_value(b, l + 1);
        exit_section_(b, m, null, r);
        return r;
    }

    /* ********************************************************** */
    // new_value
    public static boolean insert_value(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "insert_value")) return false;
        boolean r;
        Marker m = enter_section_(b, l, _NONE_, INSERT_VALUE, "<insert value>");
        r = new_value(b, l + 1);
        exit_section_(b, l, m, r, false, null);
        return r;
    }

    /* ********************************************************** */
    // { ON } conditional_group
    public static boolean join_condition(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "join_condition")) return false;
        if (!nextTokenIs(b, ON)) return false;
        boolean r;
        Marker m = enter_section_(b);
        r = consumeToken(b, ON);
        r = r && expression(b, l + 1, -1);
        exit_section_(b, m, JOIN_CONDITION, r);
        return r;
    }

    /* ********************************************************** */
    // join_spec reference_expression [alias_declaration] [ join_condition]
    public static boolean join_expression(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "join_expression")) return false;
        boolean r, p;
        Marker m = enter_section_(b, l, _NONE_, JOIN_EXPRESSION, "<join expression>");
        r = join_spec(b, l + 1);
        p = r; // pin = join_spec
        r = r && report_error_(b, reference_expression(b, l + 1));
        r = p && report_error_(b, join_expression_2(b, l + 1)) && r;
        r = p && join_expression_3(b, l + 1) && r;
        exit_section_(b, l, m, r, p, SqlParser::join_expression_recovery);
        return r || p;
    }

    // [alias_declaration]
    private static boolean join_expression_2(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "join_expression_2")) return false;
        alias_declaration(b, l + 1);
        return true;
    }

    // [ join_condition]
    private static boolean join_expression_3(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "join_expression_3")) return false;
        join_condition(b, l + 1);
        return true;
    }

    /* ********************************************************** */
    // ! {LEFT | OUTER | INNER | JOIN | WHERE | GROUP | HAVING | <<isUnitTestMode>> ';'}
    static boolean join_expression_recovery(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "join_expression_recovery")) return false;
        boolean r;
        Marker m = enter_section_(b, l, _NOT_);
        r = !join_expression_recovery_0(b, l + 1);
        exit_section_(b, l, m, r, false, null);
        return r;
    }

    // LEFT | OUTER | INNER | JOIN | WHERE | GROUP | HAVING | <<isUnitTestMode>> ';'
    private static boolean join_expression_recovery_0(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "join_expression_recovery_0")) return false;
        boolean r;
        Marker m = enter_section_(b);
        r = consumeToken(b, LEFT);
        if (!r) r = consumeToken(b, OUTER);
        if (!r) r = consumeToken(b, INNER);
        if (!r) r = consumeToken(b, JOIN);
        if (!r) r = consumeToken(b, WHERE);
        if (!r) r = consumeToken(b, GROUP);
        if (!r) r = consumeToken(b, HAVING);
        if (!r) r = join_expression_recovery_0_7(b, l + 1);
        exit_section_(b, m, null, r);
        return r;
    }

    // <<isUnitTestMode>> ';'
    private static boolean join_expression_recovery_0_7(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "join_expression_recovery_0_7")) return false;
        boolean r;
        Marker m = enter_section_(b);
        r = isUnitTestMode(b, l + 1);
        r = r && consumeToken(b, SEMICOLON);
        exit_section_(b, m, null, r);
        return r;
    }

    /* ********************************************************** */
    // [ LEFT [OUTER] | INNER ] JOIN
    public static boolean join_spec(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "join_spec")) return false;
        boolean r;
        Marker m = enter_section_(b, l, _NONE_, JOIN_SPEC, "<join spec>");
        r = join_spec_0(b, l + 1);
        r = r && consumeToken(b, JOIN);
        exit_section_(b, l, m, r, false, null);
        return r;
    }

    // [ LEFT [OUTER] | INNER ]
    private static boolean join_spec_0(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "join_spec_0")) return false;
        join_spec_0_0(b, l + 1);
        return true;
    }

    // LEFT [OUTER] | INNER
    private static boolean join_spec_0_0(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "join_spec_0_0")) return false;
        boolean r;
        Marker m = enter_section_(b);
        r = join_spec_0_0_0(b, l + 1);
        if (!r) r = consumeToken(b, INNER);
        exit_section_(b, m, null, r);
        return r;
    }

    // LEFT [OUTER]
    private static boolean join_spec_0_0_0(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "join_spec_0_0_0")) return false;
        boolean r;
        Marker m = enter_section_(b);
        r = consumeToken(b, LEFT);
        r = r && join_spec_0_0_0_1(b, l + 1);
        exit_section_(b, m, null, r);
        return r;
    }

    // [OUTER]
    private static boolean join_spec_0_0_0_1(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "join_spec_0_0_0_1")) return false;
        consumeToken(b, OUTER);
        return true;
    }

    /* ********************************************************** */
    // LIMIT numeric_or_input_parameter
    public static boolean limit_clause(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "limit_clause")) return false;
        if (!nextTokenIs(b, LIMIT)) return false;
        boolean r;
        Marker m = enter_section_(b);
        r = consumeToken(b, LIMIT);
        r = r && numeric_or_input_parameter(b, l + 1);
        exit_section_(b, m, LIMIT_CLAUSE, r);
        return r;
    }

    /* ********************************************************** */
    // string_literal
    //     | numeric_literal
    //     | boolean_literal
    //     | identifier
    //     | datetime_literal
    static boolean literal(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "literal")) return false;
        boolean r;
        r = string_literal(b, l + 1);
        if (!r) r = numeric_literal(b, l + 1);
        if (!r) r = boolean_literal(b, l + 1);
        if (!r) r = identifier(b, l + 1);
        if (!r) r = datetime_literal(b, l + 1);
        return r;
    }

    /* ********************************************************** */
    // scalar_expression | null_expression
    static boolean new_value(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "new_value")) return false;
        boolean r;
        r = scalar_expression(b, l + 1);
        if (!r) r = null_expression(b, l + 1);
        return r;
    }

    /* ********************************************************** */
    // NULL
    public static boolean null_expression(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "null_expression")) return false;
        if (!nextTokenIs(b, NULL)) return false;
        boolean r;
        Marker m = enter_section_(b);
        r = consumeToken(b, NULL);
        exit_section_(b, m, NULL_EXPRESSION, r);
        return r;
    }

    /* ********************************************************** */
    // numeric_literal | input_parameter_expression
    static boolean numeric_or_input_parameter(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "numeric_or_input_parameter")) return false;
        boolean r;
        r = numeric_literal(b, l + 1);
        if (!r) r = input_parameter_expression(b, l + 1);
        return r;
    }

    /* ********************************************************** */
    // OBJECT'('identifier')'
    public static boolean object_expression(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "object_expression")) return false;
        if (!nextTokenIs(b, OBJECT)) return false;
        boolean r;
        Marker m = enter_section_(b);
        r = consumeTokens(b, 0, OBJECT, LPAREN);
        r = r && identifier(b, l + 1);
        r = r && consumeToken(b, RPAREN);
        exit_section_(b, m, OBJECT_EXPRESSION, r);
        return r;
    }

    /* ********************************************************** */
    // OFFSET numeric_or_input_parameter
    public static boolean offset_clause(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "offset_clause")) return false;
        if (!nextTokenIs(b, OFFSET)) return false;
        boolean r;
        Marker m = enter_section_(b);
        r = consumeToken(b, OFFSET);
        r = r && numeric_or_input_parameter(b, l + 1);
        exit_section_(b, m, OFFSET_CLAUSE, r);
        return r;
    }

    /* ********************************************************** */
    // ORDER BY orderby_item {',' orderby_item}*
    public static boolean orderby_clause(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "orderby_clause")) return false;
        if (!nextTokenIs(b, ORDER)) return false;
        boolean r;
        Marker m = enter_section_(b);
        r = consumeTokens(b, 0, ORDER, BY);
        r = r && orderby_item(b, l + 1);
        r = r && orderby_clause_3(b, l + 1);
        exit_section_(b, m, ORDERBY_CLAUSE, r);
        return r;
    }

    // {',' orderby_item}*
    private static boolean orderby_clause_3(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "orderby_clause_3")) return false;
        while (true) {
            int c = current_position_(b);
            if (!orderby_clause_3_0(b, l + 1)) break;
            if (!empty_element_parsed_guard_(b, "orderby_clause_3", c)) break;
        }
        return true;
    }

    // ',' orderby_item
    private static boolean orderby_clause_3_0(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "orderby_clause_3_0")) return false;
        boolean r;
        Marker m = enter_section_(b);
        r = consumeToken(b, COMMA);
        r = r && orderby_item(b, l + 1);
        exit_section_(b, m, null, r);
        return r;
    }

    /* ********************************************************** */
    // { reference_expression | general_case_expression | simple_case_expression } [ ASC | DESC ]
    public static boolean orderby_item(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "orderby_item")) return false;
        boolean r;
        Marker m = enter_section_(b, l, _NONE_, ORDERBY_ITEM, "<orderby item>");
        r = orderby_item_0(b, l + 1);
        r = r && orderby_item_1(b, l + 1);
        exit_section_(b, l, m, r, false, null);
        return r;
    }

    // reference_expression | general_case_expression | simple_case_expression
    private static boolean orderby_item_0(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "orderby_item_0")) return false;
        boolean r;
        r = reference_expression(b, l + 1);
        if (!r) r = general_case_expression(b, l + 1);
        if (!r) r = simple_case_expression(b, l + 1);
        return r;
    }

    // [ ASC | DESC ]
    private static boolean orderby_item_1(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "orderby_item_1")) return false;
        orderby_item_1_0(b, l + 1);
        return true;
    }

    // ASC | DESC
    private static boolean orderby_item_1_0(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "orderby_item_1_0")) return false;
        boolean r;
        r = consumeToken(b, ASC);
        if (!r) r = consumeToken(b, DESC);
        return r;
    }

    /* ********************************************************** */
    // identifier {'.' (identifier | asterisk)}*
    public static boolean path_reference_expression(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "path_reference_expression")) return false;
        boolean r;
        Marker m = enter_section_(b, l, _NONE_, PATH_REFERENCE_EXPRESSION, "<path reference expression>");
        r = identifier(b, l + 1);
        r = r && path_reference_expression_1(b, l + 1);
        exit_section_(b, l, m, r, false, null);
        return r;
    }

    // {'.' (identifier | asterisk)}*
    private static boolean path_reference_expression_1(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "path_reference_expression_1")) return false;
        while (true) {
            int c = current_position_(b);
            if (!path_reference_expression_1_0(b, l + 1)) break;
            if (!empty_element_parsed_guard_(b, "path_reference_expression_1", c)) break;
        }
        return true;
    }

    // '.' (identifier | asterisk)
    private static boolean path_reference_expression_1_0(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "path_reference_expression_1_0")) return false;
        boolean r;
        Marker m = enter_section_(b);
        r = consumeToken(b, DOT);
        r = r && path_reference_expression_1_0_1(b, l + 1);
        exit_section_(b, m, null, r);
        return r;
    }

    // identifier | asterisk
    private static boolean path_reference_expression_1_0_1(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "path_reference_expression_1_0_1")) return false;
        boolean r;
        r = identifier(b, l + 1);
        if (!r) r = asterisk(b, l + 1);
        return r;
    }

    /* ********************************************************** */
    // reference_expression | input_parameter_expression
    static boolean reference_or_parameter(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "reference_or_parameter")) return false;
        boolean r;
        r = reference_expression(b, l + 1);
        if (!r) r = input_parameter_expression(b, l + 1);
        return r;
    }

    /* ********************************************************** */
    // simple_arithmetic_expression
    //     | string_expression
    //     | input_parameter_expression
    //     | case_expression
    //     | datetime_expression
    //     | literal_group
    //     | boolean_expression
    //     | type_expression
    //     | reference_expression
    static boolean scalar_expression(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "scalar_expression")) return false;
        boolean r;
        r = simple_arithmetic_expression(b, l + 1);
        if (!r) r = string_expression(b, l + 1);
        if (!r) r = input_parameter_expression(b, l + 1);
        if (!r) r = expression(b, l + 1, 8);
        if (!r) r = datetime_expression(b, l + 1);
        if (!r) r = expression(b, l + 1, 3);
        if (!r) r = boolean_expression(b, l + 1);
        if (!r) r = type_expression(b, l + 1);
        if (!r) r = reference_expression(b, l + 1);
        return r;
    }

    /* ********************************************************** */
    // SELECT [DISTINCT] select_item {',' select_item}*
    public static boolean select_clause(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "select_clause")) return false;
        if (!nextTokenIs(b, SELECT)) return false;
        boolean r, p;
        Marker m = enter_section_(b, l, _NONE_, SELECT_CLAUSE, null);
        r = consumeToken(b, SELECT);
        p = r; // pin = SELECT
        r = r && report_error_(b, select_clause_1(b, l + 1));
        r = p && report_error_(b, select_item(b, l + 1)) && r;
        r = p && select_clause_3(b, l + 1) && r;
        exit_section_(b, l, m, r, p, null);
        return r || p;
    }

    // [DISTINCT]
    private static boolean select_clause_1(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "select_clause_1")) return false;
        consumeToken(b, DISTINCT);
        return true;
    }

    // {',' select_item}*
    private static boolean select_clause_3(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "select_clause_3")) return false;
        while (true) {
            int c = current_position_(b);
            if (!select_clause_3_0(b, l + 1)) break;
            if (!empty_element_parsed_guard_(b, "select_clause_3", c)) break;
        }
        return true;
    }

    // ',' select_item
    private static boolean select_clause_3_0(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "select_clause_3_0")) return false;
        boolean r;
        Marker m = enter_section_(b);
        r = consumeToken(b, COMMA);
        r = r && select_item(b, l + 1);
        exit_section_(b, m, null, r);
        return r;
    }

    /* ********************************************************** */
    // reference_expression |
    //     scalar_expression |
    //     arithmetic_expression |
    //     aggregate_expression |
    //     object_expression |
    //     case_expression
    static boolean select_expression(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "select_expression")) return false;
        boolean r;
        r = reference_expression(b, l + 1);
        if (!r) r = scalar_expression(b, l + 1);
        if (!r) r = expression(b, l + 1, 1);
        if (!r) r = aggregate_expression(b, l + 1);
        if (!r) r = object_expression(b, l + 1);
        if (!r) r = expression(b, l + 1, 8);
        return r;
    }

    /* ********************************************************** */
    // select_expression [alias_declaration]
    public static boolean select_item(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "select_item")) return false;
        boolean r;
        Marker m = enter_section_(b, l, _NONE_, SELECT_ITEM, "<select item>");
        r = select_expression(b, l + 1);
        r = r && select_item_1(b, l + 1);
        exit_section_(b, l, m, r, false, null);
        return r;
    }

    // [alias_declaration]
    private static boolean select_item_1(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "select_item_1")) return false;
        alias_declaration(b, l + 1);
        return true;
    }

    /* ********************************************************** */
    // select_clause
    //     from_clause
    //     [where_clause]
    //     [groupby_clause]
    //     [having_clause]
    //     [orderby_clause]
    //     [limit_clause]
    //     [offset_clause]
    public static boolean select_statement(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "select_statement")) return false;
        if (!nextTokenIs(b, SELECT)) return false;
        boolean r;
        Marker m = enter_section_(b);
        r = select_clause(b, l + 1);
        r = r && from_clause(b, l + 1);
        r = r && select_statement_2(b, l + 1);
        r = r && select_statement_3(b, l + 1);
        r = r && select_statement_4(b, l + 1);
        r = r && select_statement_5(b, l + 1);
        r = r && select_statement_6(b, l + 1);
        r = r && select_statement_7(b, l + 1);
        exit_section_(b, m, SELECT_STATEMENT, r);
        return r;
    }

    // [where_clause]
    private static boolean select_statement_2(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "select_statement_2")) return false;
        where_clause(b, l + 1);
        return true;
    }

    // [groupby_clause]
    private static boolean select_statement_3(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "select_statement_3")) return false;
        groupby_clause(b, l + 1);
        return true;
    }

    // [having_clause]
    private static boolean select_statement_4(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "select_statement_4")) return false;
        having_clause(b, l + 1);
        return true;
    }

    // [orderby_clause]
    private static boolean select_statement_5(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "select_statement_5")) return false;
        orderby_clause(b, l + 1);
        return true;
    }

    // [limit_clause]
    private static boolean select_statement_6(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "select_statement_6")) return false;
        limit_clause(b, l + 1);
        return true;
    }

    // [offset_clause]
    private static boolean select_statement_7(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "select_statement_7")) return false;
        offset_clause(b, l + 1);
        return true;
    }

    /* ********************************************************** */
    // <<isUnitTestMode>> statement { [';'] statement }* [';']
    static boolean semicolon_delimited_statements(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "semicolon_delimited_statements")) return false;
        boolean r;
        Marker m = enter_section_(b);
        r = isUnitTestMode(b, l + 1);
        r = r && statement(b, l + 1);
        r = r && semicolon_delimited_statements_2(b, l + 1);
        r = r && semicolon_delimited_statements_3(b, l + 1);
        exit_section_(b, m, null, r);
        return r;
    }

    // { [';'] statement }*
    private static boolean semicolon_delimited_statements_2(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "semicolon_delimited_statements_2")) return false;
        while (true) {
            int c = current_position_(b);
            if (!semicolon_delimited_statements_2_0(b, l + 1)) break;
            if (!empty_element_parsed_guard_(b, "semicolon_delimited_statements_2", c)) break;
        }
        return true;
    }

    // [';'] statement
    private static boolean semicolon_delimited_statements_2_0(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "semicolon_delimited_statements_2_0")) return false;
        boolean r;
        Marker m = enter_section_(b);
        r = semicolon_delimited_statements_2_0_0(b, l + 1);
        r = r && statement(b, l + 1);
        exit_section_(b, m, null, r);
        return r;
    }

    // [';']
    private static boolean semicolon_delimited_statements_2_0_0(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "semicolon_delimited_statements_2_0_0")) return false;
        consumeToken(b, SEMICOLON);
        return true;
    }

    // [';']
    private static boolean semicolon_delimited_statements_3(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "semicolon_delimited_statements_3")) return false;
        consumeToken(b, SEMICOLON);
        return true;
    }

    /* ********************************************************** */
    // expression
    static boolean simple_arithmetic_expression(PsiBuilder b, int l) {
        return expression(b, l + 1, -1);
    }

    /* ********************************************************** */
    // SELECT [DISTINCT] simple_select_expression
    public static boolean simple_select_clause(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "simple_select_clause")) return false;
        if (!nextTokenIs(b, SELECT)) return false;
        boolean r;
        Marker m = enter_section_(b);
        r = consumeToken(b, SELECT);
        r = r && simple_select_clause_1(b, l + 1);
        r = r && simple_select_expression(b, l + 1);
        exit_section_(b, m, SIMPLE_SELECT_CLAUSE, r);
        return r;
    }

    // [DISTINCT]
    private static boolean simple_select_clause_1(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "simple_select_clause_1")) return false;
        consumeToken(b, DISTINCT);
        return true;
    }

    /* ********************************************************** */
    // reference_expression |
    //    scalar_expression |
    //    aggregate_expression
    static boolean simple_select_expression(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "simple_select_expression")) return false;
        boolean r;
        r = reference_expression(b, l + 1);
        if (!r) r = scalar_expression(b, l + 1);
        if (!r) r = aggregate_expression(b, l + 1);
        return r;
    }

    /* ********************************************************** */
    // WHEN expression THEN expression
    public static boolean simple_when_clause(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "simple_when_clause")) return false;
        if (!nextTokenIs(b, WHEN)) return false;
        boolean r;
        Marker m = enter_section_(b);
        r = consumeToken(b, WHEN);
        r = r && expression(b, l + 1, -1);
        r = r && consumeToken(b, THEN);
        r = r && expression(b, l + 1, -1);
        exit_section_(b, m, SIMPLE_WHEN_CLAUSE, r);
        return r;
    }

    /* ********************************************************** */
    // select_statement | update_statement | delete_statement | insert_statement
    public static boolean statement(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "statement")) return false;
        boolean r;
        Marker m = enter_section_(b, l, _NONE_, STATEMENT, "<statement>");
        r = select_statement(b, l + 1);
        if (!r) r = update_statement(b, l + 1);
        if (!r) r = delete_statement(b, l + 1);
        if (!r) r = insert_statement(b, l + 1);
        exit_section_(b, l, m, r, false, SqlParser::statement_recover);
        return r;
    }

    /* ********************************************************** */
    // !{';' | 'SELECT' | 'UPDATE' | 'DELETE' }
    static boolean statement_recover(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "statement_recover")) return false;
        boolean r;
        Marker m = enter_section_(b, l, _NOT_);
        r = !statement_recover_0(b, l + 1);
        exit_section_(b, l, m, r, false, null);
        return r;
    }

    // ';' | 'SELECT' | 'UPDATE' | 'DELETE'
    private static boolean statement_recover_0(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "statement_recover_0")) return false;
        boolean r;
        r = consumeToken(b, SEMICOLON);
        if (!r) r = consumeToken(b, "SELECT");
        if (!r) r = consumeToken(b, "UPDATE");
        if (!r) r = consumeToken(b, "DELETE");
        return r;
    }

    /* ********************************************************** */
    // string_literal
    //     | input_parameter_expression
    //     | string_function_expression
    //     | aggregate_expression
    //     | case_expression
    //     | function_invocation_expression
    //     | reference_expression
    static boolean string_expression(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "string_expression")) return false;
        boolean r;
        r = string_literal(b, l + 1);
        if (!r) r = input_parameter_expression(b, l + 1);
        if (!r) r = string_function_expression(b, l + 1);
        if (!r) r = aggregate_expression(b, l + 1);
        if (!r) r = expression(b, l + 1, 8);
        if (!r) r = function_invocation_expression(b, l + 1);
        if (!r) r = reference_expression(b, l + 1);
        return r;
    }

    /* ********************************************************** */
    // simple_select_clause subquery_from_clause [where_clause] [groupby_clause] [having_clause]
    public static boolean subquery(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "subquery")) return false;
        if (!nextTokenIs(b, SELECT)) return false;
        boolean r;
        Marker m = enter_section_(b);
        r = simple_select_clause(b, l + 1);
        r = r && subquery_from_clause(b, l + 1);
        r = r && subquery_2(b, l + 1);
        r = r && subquery_3(b, l + 1);
        r = r && subquery_4(b, l + 1);
        exit_section_(b, m, SUBQUERY, r);
        return r;
    }

    // [where_clause]
    private static boolean subquery_2(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "subquery_2")) return false;
        where_clause(b, l + 1);
        return true;
    }

    // [groupby_clause]
    private static boolean subquery_3(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "subquery_3")) return false;
        groupby_clause(b, l + 1);
        return true;
    }

    // [having_clause]
    private static boolean subquery_4(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "subquery_4")) return false;
        having_clause(b, l + 1);
        return true;
    }

    /* ********************************************************** */
    // FROM subselect_identification_variable_declaration
    //     {',' subselect_identification_variable_declaration |
    //      collection_member_declaration}*
    public static boolean subquery_from_clause(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "subquery_from_clause")) return false;
        if (!nextTokenIs(b, FROM)) return false;
        boolean r;
        Marker m = enter_section_(b);
        r = consumeToken(b, FROM);
        r = r && subselect_identification_variable_declaration(b, l + 1);
        r = r && subquery_from_clause_2(b, l + 1);
        exit_section_(b, m, SUBQUERY_FROM_CLAUSE, r);
        return r;
    }

    // {',' subselect_identification_variable_declaration |
    //      collection_member_declaration}*
    private static boolean subquery_from_clause_2(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "subquery_from_clause_2")) return false;
        while (true) {
            int c = current_position_(b);
            if (!subquery_from_clause_2_0(b, l + 1)) break;
            if (!empty_element_parsed_guard_(b, "subquery_from_clause_2", c)) break;
        }
        return true;
    }

    // ',' subselect_identification_variable_declaration |
    //      collection_member_declaration
    private static boolean subquery_from_clause_2_0(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "subquery_from_clause_2_0")) return false;
        boolean r;
        Marker m = enter_section_(b);
        r = subquery_from_clause_2_0_0(b, l + 1);
        if (!r) r = collection_member_declaration(b, l + 1);
        exit_section_(b, m, null, r);
        return r;
    }

    // ',' subselect_identification_variable_declaration
    private static boolean subquery_from_clause_2_0_0(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "subquery_from_clause_2_0_0")) return false;
        boolean r;
        Marker m = enter_section_(b);
        r = consumeToken(b, COMMA);
        r = r && subselect_identification_variable_declaration(b, l + 1);
        exit_section_(b, m, null, r);
        return r;
    }

    /* ********************************************************** */
    // identification_variable_declaration |
    //     reference_expression alias_declaration {join_expression}*|
    //     derived_collection_member_declaration
    static boolean subselect_identification_variable_declaration(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "subselect_identification_variable_declaration")) return false;
        boolean r;
        Marker m = enter_section_(b);
        r = identification_variable_declaration(b, l + 1);
        if (!r) r = subselect_identification_variable_declaration_1(b, l + 1);
        if (!r) r = derived_collection_member_declaration(b, l + 1);
        exit_section_(b, m, null, r);
        return r;
    }

    // reference_expression alias_declaration {join_expression}*
    private static boolean subselect_identification_variable_declaration_1(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "subselect_identification_variable_declaration_1")) return false;
        boolean r;
        Marker m = enter_section_(b);
        r = reference_expression(b, l + 1);
        r = r && alias_declaration(b, l + 1);
        r = r && subselect_identification_variable_declaration_1_2(b, l + 1);
        exit_section_(b, m, null, r);
        return r;
    }

    // {join_expression}*
    private static boolean subselect_identification_variable_declaration_1_2(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "subselect_identification_variable_declaration_1_2")) return false;
        while (true) {
            int c = current_position_(b);
            if (!subselect_identification_variable_declaration_1_2_0(b, l + 1)) break;
            if (!empty_element_parsed_guard_(b, "subselect_identification_variable_declaration_1_2", c)) break;
        }
        return true;
    }

    // {join_expression}
    private static boolean subselect_identification_variable_declaration_1_2_0(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "subselect_identification_variable_declaration_1_2_0")) return false;
        boolean r;
        Marker m = enter_section_(b);
        r = join_expression(b, l + 1);
        exit_section_(b, m, null, r);
        return r;
    }

    /* ********************************************************** */
    // subquery_expression | table_name_ref
    public static boolean table_expression(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "table_expression")) return false;
        boolean r;
        Marker m = enter_section_(b, l, _COLLAPSE_, TABLE_EXPRESSION, "<table expression>");
        r = subquery_expression(b, l + 1);
        if (!r) r = table_name_ref(b, l + 1);
        exit_section_(b, l, m, r, false, null);
        return r;
    }

    /* ********************************************************** */
    // table_expression { join_expression }* | {'(' table_expression { join_expression } ')'}*
    public static boolean table_expression_join_declaration(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "table_expression_join_declaration")) return false;
        boolean r;
        Marker m = enter_section_(b, l, _NONE_, TABLE_EXPRESSION_JOIN_DECLARATION, "<table expression join declaration>");
        r = table_expression_join_declaration_0(b, l + 1);
        if (!r) r = table_expression_join_declaration_1(b, l + 1);
        exit_section_(b, l, m, r, false, null);
        return r;
    }

    // table_expression { join_expression }*
    private static boolean table_expression_join_declaration_0(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "table_expression_join_declaration_0")) return false;
        boolean r;
        Marker m = enter_section_(b);
        r = table_expression(b, l + 1);
        r = r && table_expression_join_declaration_0_1(b, l + 1);
        exit_section_(b, m, null, r);
        return r;
    }

    // { join_expression }*
    private static boolean table_expression_join_declaration_0_1(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "table_expression_join_declaration_0_1")) return false;
        while (true) {
            int c = current_position_(b);
            if (!table_expression_join_declaration_0_1_0(b, l + 1)) break;
            if (!empty_element_parsed_guard_(b, "table_expression_join_declaration_0_1", c)) break;
        }
        return true;
    }

    // { join_expression }
    private static boolean table_expression_join_declaration_0_1_0(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "table_expression_join_declaration_0_1_0")) return false;
        boolean r;
        Marker m = enter_section_(b);
        r = join_expression(b, l + 1);
        exit_section_(b, m, null, r);
        return r;
    }

    // {'(' table_expression { join_expression } ')'}*
    private static boolean table_expression_join_declaration_1(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "table_expression_join_declaration_1")) return false;
        while (true) {
            int c = current_position_(b);
            if (!table_expression_join_declaration_1_0(b, l + 1)) break;
            if (!empty_element_parsed_guard_(b, "table_expression_join_declaration_1", c)) break;
        }
        return true;
    }

    // '(' table_expression { join_expression } ')'
    private static boolean table_expression_join_declaration_1_0(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "table_expression_join_declaration_1_0")) return false;
        boolean r;
        Marker m = enter_section_(b);
        r = consumeToken(b, LPAREN);
        r = r && table_expression(b, l + 1);
        r = r && table_expression_join_declaration_1_0_2(b, l + 1);
        r = r && consumeToken(b, RPAREN);
        exit_section_(b, m, null, r);
        return r;
    }

    // { join_expression }
    private static boolean table_expression_join_declaration_1_0_2(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "table_expression_join_declaration_1_0_2")) return false;
        boolean r;
        Marker m = enter_section_(b);
        r = join_expression(b, l + 1);
        exit_section_(b, m, null, r);
        return r;
    }

    /* ********************************************************** */
    // identifier [alias_declaration]
    public static boolean table_name_ref(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "table_name_ref")) return false;
        boolean r;
        Marker m = enter_section_(b, l, _NONE_, TABLE_NAME_REF, "<table name ref>");
        r = identifier(b, l + 1);
        r = r && table_name_ref_1(b, l + 1);
        exit_section_(b, l, m, r, false, null);
        return r;
    }

    // [alias_declaration]
    private static boolean table_name_ref_1(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "table_name_ref_1")) return false;
        alias_declaration(b, l + 1);
        return true;
    }

    /* ********************************************************** */
    // LEADING | TRAILING | BOTH
    public static boolean trim_specification(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "trim_specification")) return false;
        boolean r;
        Marker m = enter_section_(b, l, _NONE_, TRIM_SPECIFICATION, "<trim specification>");
        r = consumeToken(b, LEADING);
        if (!r) r = consumeToken(b, TRAILING);
        if (!r) r = consumeToken(b, BOTH);
        exit_section_(b, l, m, r, false, null);
        return r;
    }

    /* ********************************************************** */
    // INTEGER | STRING | DATE | TIME | TIMESTAMP | BOOLEAN
    public static boolean type_literal(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "type_literal")) return false;
        boolean r;
        Marker m = enter_section_(b, l, _NONE_, TYPE_LITERAL, "<type literal>");
        r = consumeToken(b, INTEGER);
        if (!r) r = consumeToken(b, STRING);
        if (!r) r = consumeToken(b, DATE);
        if (!r) r = consumeToken(b, TIME);
        if (!r) r = consumeToken(b, TIMESTAMP);
        if (!r) r = consumeToken(b, BOOLEAN);
        exit_section_(b, l, m, r, false, null);
        return r;
    }

    /* ********************************************************** */
    // UPDATE table_name_ref SET update_item {',' update_item}*
    public static boolean update_clause(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "update_clause")) return false;
        if (!nextTokenIs(b, UPDATE)) return false;
        boolean r;
        Marker m = enter_section_(b);
        r = consumeToken(b, UPDATE);
        r = r && table_name_ref(b, l + 1);
        r = r && consumeToken(b, SET);
        r = r && update_item(b, l + 1);
        r = r && update_clause_4(b, l + 1);
        exit_section_(b, m, UPDATE_CLAUSE, r);
        return r;
    }

    // {',' update_item}*
    private static boolean update_clause_4(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "update_clause_4")) return false;
        while (true) {
            int c = current_position_(b);
            if (!update_clause_4_0(b, l + 1)) break;
            if (!empty_element_parsed_guard_(b, "update_clause_4", c)) break;
        }
        return true;
    }

    // ',' update_item
    private static boolean update_clause_4_0(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "update_clause_4_0")) return false;
        boolean r;
        Marker m = enter_section_(b);
        r = consumeToken(b, COMMA);
        r = r && update_item(b, l + 1);
        exit_section_(b, m, null, r);
        return r;
    }

    /* ********************************************************** */
    // reference_expression '=' new_value
    public static boolean update_item(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "update_item")) return false;
        boolean r;
        Marker m = enter_section_(b, l, _NONE_, UPDATE_ITEM, "<update item>");
        r = reference_expression(b, l + 1);
        r = r && consumeToken(b, EQ);
        r = r && new_value(b, l + 1);
        exit_section_(b, l, m, r, false, null);
        return r;
    }

    /* ********************************************************** */
    // update_clause [where_clause]
    public static boolean update_statement(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "update_statement")) return false;
        if (!nextTokenIs(b, UPDATE)) return false;
        boolean r;
        Marker m = enter_section_(b);
        r = update_clause(b, l + 1);
        r = r && update_statement_1(b, l + 1);
        exit_section_(b, m, UPDATE_STATEMENT, r);
        return r;
    }

    // [where_clause]
    private static boolean update_statement_1(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "update_statement_1")) return false;
        where_clause(b, l + 1);
        return true;
    }

    /* ********************************************************** */
    // VALUES insert_tuple {',' insert_tuple }*
    static boolean values_list(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "values_list")) return false;
        if (!nextTokenIs(b, VALUES)) return false;
        boolean r;
        Marker m = enter_section_(b);
        r = consumeToken(b, VALUES);
        r = r && insert_tuple(b, l + 1);
        r = r && values_list_2(b, l + 1);
        exit_section_(b, m, null, r);
        return r;
    }

    // {',' insert_tuple }*
    private static boolean values_list_2(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "values_list_2")) return false;
        while (true) {
            int c = current_position_(b);
            if (!values_list_2_0(b, l + 1)) break;
            if (!empty_element_parsed_guard_(b, "values_list_2", c)) break;
        }
        return true;
    }

    // ',' insert_tuple
    private static boolean values_list_2_0(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "values_list_2_0")) return false;
        boolean r;
        Marker m = enter_section_(b);
        r = consumeToken(b, COMMA);
        r = r && insert_tuple(b, l + 1);
        exit_section_(b, m, null, r);
        return r;
    }

    /* ********************************************************** */
    // WHEN expression THEN scalar_expression
    public static boolean when_clause(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "when_clause")) return false;
        if (!nextTokenIs(b, WHEN)) return false;
        boolean r;
        Marker m = enter_section_(b);
        r = consumeToken(b, WHEN);
        r = r && expression(b, l + 1, -1);
        r = r && consumeToken(b, THEN);
        r = r && scalar_expression(b, l + 1);
        exit_section_(b, m, WHEN_CLAUSE, r);
        return r;
    }

    /* ********************************************************** */
    // WHERE { conditional_group | comparison_group }
    public static boolean where_clause(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "where_clause")) return false;
        if (!nextTokenIs(b, WHERE)) return false;
        boolean r;
        Marker m = enter_section_(b);
        r = consumeToken(b, WHERE);
        r = r && where_clause_1(b, l + 1);
        exit_section_(b, m, WHERE_CLAUSE, r);
        return r;
    }

    // conditional_group | comparison_group
    private static boolean where_clause_1(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "where_clause_1")) return false;
        boolean r;
        r = expression(b, l + 1, -1);
        if (!r) r = expression(b, l + 1, 0);
        return r;
    }

    /* ********************************************************** */
    // Expression root: expression
    // Operator priority table:
    // 0: BINARY(conditional_or_expression) BINARY(conditional_and_expression) PREFIX(conditional_not_expression)
    // 1: BINARY(comparison_expression) BINARY(between_expression) ATOM(in_expression) POSTFIX(like_expression)
    //    ATOM(null_comparison_expression) ATOM(empty_collection_comparison_expression) ATOM(collection_member_expression) ATOM(exists_expression)
    // 2: BINARY(additive_expression) BINARY(multiplicative_expression) PREFIX(unary_arithmetic_expression) ATOM(functions_returning_numerics_expression)
    //    PREFIX(function_invocation_expression)
    // 3: ATOM(all_or_any_expression)
    // 4: ATOM(string_literal) ATOM(boolean_literal) ATOM(datetime_literal) ATOM(numeric_literal)
    // 5: ATOM(input_parameter_expression)
    // 6: ATOM(subquery_expression)
    // 7: ATOM(string_function_expression)
    // 8: ATOM(aggregate_expression)
    // 9: ATOM(general_case_expression) ATOM(simple_case_expression) ATOM(coalesce_expression) ATOM(nullif_expression)
    // 10: ATOM(datetime_function_expression)
    // 11: ATOM(type_expression)
    // 12: ATOM(reference_expression)
    // 13: PREFIX(paren_expression)
    public static boolean expression(PsiBuilder b, int l, int g) {
        if (!recursion_guard_(b, l, "expression")) return false;
        addVariant(b, "<expression>");
        boolean r, p;
        Marker m = enter_section_(b, l, _NONE_, "<expression>");
        r = conditional_not_expression(b, l + 1);
        if (!r) r = in_expression(b, l + 1);
        if (!r) r = null_comparison_expression(b, l + 1);
        if (!r) r = empty_collection_comparison_expression(b, l + 1);
        if (!r) r = collection_member_expression(b, l + 1);
        if (!r) r = exists_expression(b, l + 1);
        if (!r) r = unary_arithmetic_expression(b, l + 1);
        if (!r) r = functions_returning_numerics_expression(b, l + 1);
        if (!r) r = function_invocation_expression(b, l + 1);
        if (!r) r = all_or_any_expression(b, l + 1);
        if (!r) r = string_literal(b, l + 1);
        if (!r) r = boolean_literal(b, l + 1);
        if (!r) r = datetime_literal(b, l + 1);
        if (!r) r = numeric_literal(b, l + 1);
        if (!r) r = input_parameter_expression(b, l + 1);
        if (!r) r = subquery_expression(b, l + 1);
        if (!r) r = string_function_expression(b, l + 1);
        if (!r) r = aggregate_expression(b, l + 1);
        if (!r) r = general_case_expression(b, l + 1);
        if (!r) r = simple_case_expression(b, l + 1);
        if (!r) r = coalesce_expression(b, l + 1);
        if (!r) r = nullif_expression(b, l + 1);
        if (!r) r = datetime_function_expression(b, l + 1);
        if (!r) r = type_expression(b, l + 1);
        if (!r) r = reference_expression(b, l + 1);
        if (!r) r = paren_expression(b, l + 1);
        p = r;
        r = r && expression_0(b, l + 1, g);
        exit_section_(b, l, m, null, r, p, null);
        return r || p;
    }

    public static boolean expression_0(PsiBuilder b, int l, int g) {
        if (!recursion_guard_(b, l, "expression_0")) return false;
        boolean r = true;
        while (true) {
            Marker m = enter_section_(b, l, _LEFT_, null);
            if (g < 0 && consumeTokenSmart(b, OR)) {
                r = expression(b, l, -1);
                exit_section_(b, l, m, CONDITIONAL_OR_EXPRESSION, r, true, null);
            } else if (g < 0 && consumeTokenSmart(b, AND)) {
                r = expression(b, l, 0);
                exit_section_(b, l, m, CONDITIONAL_AND_EXPRESSION, r, true, null);
            } else if (g < 1 && comparison_operator(b, l + 1)) {
                r = expression(b, l, 1);
                exit_section_(b, l, m, COMPARISON_EXPRESSION, r, true, null);
            } else if (g < 1 && between_expression_0(b, l + 1)) {
                r = report_error_(b, expression(b, l, 1));
                r = between_expression_1(b, l + 1) && r;
                exit_section_(b, l, m, BETWEEN_EXPRESSION, r, true, null);
            } else if (g < 1 && like_expression_0(b, l + 1)) {
                r = true;
                exit_section_(b, l, m, LIKE_EXPRESSION, r, true, null);
            } else if (g < 2 && additive_expression_0(b, l + 1)) {
                r = expression(b, l, 1);
                exit_section_(b, l, m, ADDITIVE_EXPRESSION, r, true, null);
            } else if (g < 2 && multiplicative_expression_0(b, l + 1)) {
                r = expression(b, l, 2);
                exit_section_(b, l, m, MULTIPLICATIVE_EXPRESSION, r, true, null);
            } else {
                exit_section_(b, l, m, null, false, false, null);
                break;
            }
        }
        return r;
    }

    public static boolean conditional_not_expression(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "conditional_not_expression")) return false;
        if (!nextTokenIsSmart(b, NOT)) return false;
        boolean r, p;
        Marker m = enter_section_(b, l, _NONE_, null);
        r = consumeTokenSmart(b, NOT);
        p = r;
        r = p && expression(b, l, 0);
        exit_section_(b, l, m, CONDITIONAL_NOT_EXPRESSION, r, p, null);
        return r || p;
    }

    // [NOT] BETWEEN
    private static boolean between_expression_0(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "between_expression_0")) return false;
        boolean r;
        Marker m = enter_section_(b);
        r = between_expression_0_0(b, l + 1);
        r = r && consumeToken(b, BETWEEN);
        exit_section_(b, m, null, r);
        return r;
    }

    // [NOT]
    private static boolean between_expression_0_0(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "between_expression_0_0")) return false;
        consumeTokenSmart(b, NOT);
        return true;
    }

    // AND expression
    private static boolean between_expression_1(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "between_expression_1")) return false;
        boolean r;
        Marker m = enter_section_(b);
        r = consumeToken(b, AND);
        r = r && expression(b, l + 1, -1);
        exit_section_(b, m, null, r);
        return r;
    }

    // { type_expression | reference_expression } [NOT] IN
    //     {in_collection | '('subquery')' | input_parameter_expression}
    public static boolean in_expression(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "in_expression")) return false;
        boolean r;
        Marker m = enter_section_(b, l, _NONE_, IN_EXPRESSION, "<in expression>");
        r = in_expression_0(b, l + 1);
        r = r && in_expression_1(b, l + 1);
        r = r && consumeToken(b, IN);
        r = r && in_expression_3(b, l + 1);
        exit_section_(b, l, m, r, false, null);
        return r;
    }

    // type_expression | reference_expression
    private static boolean in_expression_0(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "in_expression_0")) return false;
        boolean r;
        r = type_expression(b, l + 1);
        if (!r) r = reference_expression(b, l + 1);
        return r;
    }

    // [NOT]
    private static boolean in_expression_1(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "in_expression_1")) return false;
        consumeTokenSmart(b, NOT);
        return true;
    }

    // in_collection | '('subquery')' | input_parameter_expression
    private static boolean in_expression_3(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "in_expression_3")) return false;
        boolean r;
        Marker m = enter_section_(b);
        r = in_collection(b, l + 1);
        if (!r) r = in_expression_3_1(b, l + 1);
        if (!r) r = input_parameter_expression(b, l + 1);
        exit_section_(b, m, null, r);
        return r;
    }

    // '('subquery')'
    private static boolean in_expression_3_1(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "in_expression_3_1")) return false;
        boolean r;
        Marker m = enter_section_(b);
        r = consumeTokenSmart(b, LPAREN);
        r = r && subquery(b, l + 1);
        r = r && consumeToken(b, RPAREN);
        exit_section_(b, m, null, r);
        return r;
    }

    // [NOT] LIKE {['%']reference_or_parameter['%'] | string_literal | string_function_expression } [ESCAPE {string_literal | string_function_expression | input_parameter_expression}]
    private static boolean like_expression_0(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "like_expression_0")) return false;
        boolean r;
        Marker m = enter_section_(b);
        r = like_expression_0_0(b, l + 1);
        r = r && consumeToken(b, LIKE);
        r = r && like_expression_0_2(b, l + 1);
        r = r && like_expression_0_3(b, l + 1);
        exit_section_(b, m, null, r);
        return r;
    }

    // [NOT]
    private static boolean like_expression_0_0(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "like_expression_0_0")) return false;
        consumeTokenSmart(b, NOT);
        return true;
    }

    // ['%']reference_or_parameter['%'] | string_literal | string_function_expression
    private static boolean like_expression_0_2(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "like_expression_0_2")) return false;
        boolean r;
        Marker m = enter_section_(b);
        r = like_expression_0_2_0(b, l + 1);
        if (!r) r = string_literal(b, l + 1);
        if (!r) r = string_function_expression(b, l + 1);
        exit_section_(b, m, null, r);
        return r;
    }

    // ['%']reference_or_parameter['%']
    private static boolean like_expression_0_2_0(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "like_expression_0_2_0")) return false;
        boolean r;
        Marker m = enter_section_(b);
        r = like_expression_0_2_0_0(b, l + 1);
        r = r && reference_or_parameter(b, l + 1);
        r = r && like_expression_0_2_0_2(b, l + 1);
        exit_section_(b, m, null, r);
        return r;
    }

    // ['%']
    private static boolean like_expression_0_2_0_0(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "like_expression_0_2_0_0")) return false;
        consumeTokenSmart(b, "%");
        return true;
    }

    // ['%']
    private static boolean like_expression_0_2_0_2(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "like_expression_0_2_0_2")) return false;
        consumeTokenSmart(b, "%");
        return true;
    }

    // [ESCAPE {string_literal | string_function_expression | input_parameter_expression}]
    private static boolean like_expression_0_3(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "like_expression_0_3")) return false;
        like_expression_0_3_0(b, l + 1);
        return true;
    }

    // ESCAPE {string_literal | string_function_expression | input_parameter_expression}
    private static boolean like_expression_0_3_0(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "like_expression_0_3_0")) return false;
        boolean r;
        Marker m = enter_section_(b);
        r = consumeTokenSmart(b, ESCAPE);
        r = r && like_expression_0_3_0_1(b, l + 1);
        exit_section_(b, m, null, r);
        return r;
    }

    // string_literal | string_function_expression | input_parameter_expression
    private static boolean like_expression_0_3_0_1(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "like_expression_0_3_0_1")) return false;
        boolean r;
        r = string_literal(b, l + 1);
        if (!r) r = string_function_expression(b, l + 1);
        if (!r) r = input_parameter_expression(b, l + 1);
        return r;
    }

    // reference_or_parameter IS [NOT] NULL
    public static boolean null_comparison_expression(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "null_comparison_expression")) return false;
        boolean r;
        Marker m = enter_section_(b, l, _COLLAPSE_, NULL_COMPARISON_EXPRESSION, "<null comparison expression>");
        r = reference_or_parameter(b, l + 1);
        r = r && consumeToken(b, IS);
        r = r && null_comparison_expression_2(b, l + 1);
        r = r && consumeToken(b, NULL);
        exit_section_(b, l, m, r, false, null);
        return r;
    }

    // [NOT]
    private static boolean null_comparison_expression_2(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "null_comparison_expression_2")) return false;
        consumeTokenSmart(b, NOT);
        return true;
    }

    // {reference_expression} IS [NOT] EMPTY
    public static boolean empty_collection_comparison_expression(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "empty_collection_comparison_expression")) return false;
        boolean r;
        Marker m = enter_section_(b, l, _NONE_, EMPTY_COLLECTION_COMPARISON_EXPRESSION, "<empty collection comparison expression>");
        r = empty_collection_comparison_expression_0(b, l + 1);
        r = r && consumeToken(b, IS);
        r = r && empty_collection_comparison_expression_2(b, l + 1);
        r = r && consumeToken(b, EMPTY);
        exit_section_(b, l, m, r, false, null);
        return r;
    }

    // {reference_expression}
    private static boolean empty_collection_comparison_expression_0(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "empty_collection_comparison_expression_0")) return false;
        boolean r;
        Marker m = enter_section_(b);
        r = reference_expression(b, l + 1);
        exit_section_(b, m, null, r);
        return r;
    }

    // [NOT]
    private static boolean empty_collection_comparison_expression_2(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "empty_collection_comparison_expression_2")) return false;
        consumeTokenSmart(b, NOT);
        return true;
    }

    // entity_or_value_expression
    //     [NOT] MEMBER [OF] reference_expression
    public static boolean collection_member_expression(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "collection_member_expression")) return false;
        boolean r;
        Marker m = enter_section_(b, l, _COLLAPSE_, COLLECTION_MEMBER_EXPRESSION, "<collection member expression>");
        r = entity_or_value_expression(b, l + 1);
        r = r && collection_member_expression_1(b, l + 1);
        r = r && consumeToken(b, MEMBER);
        r = r && collection_member_expression_3(b, l + 1);
        r = r && reference_expression(b, l + 1);
        exit_section_(b, l, m, r, false, null);
        return r;
    }

    // [NOT]
    private static boolean collection_member_expression_1(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "collection_member_expression_1")) return false;
        consumeTokenSmart(b, NOT);
        return true;
    }

    // [OF]
    private static boolean collection_member_expression_3(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "collection_member_expression_3")) return false;
        consumeTokenSmart(b, OF);
        return true;
    }

    // [NOT] EXISTS '('subquery')'
    public static boolean exists_expression(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "exists_expression")) return false;
        if (!nextTokenIsSmart(b, EXISTS, NOT)) return false;
        boolean r;
        Marker m = enter_section_(b, l, _NONE_, EXISTS_EXPRESSION, "<exists expression>");
        r = exists_expression_0(b, l + 1);
        r = r && consumeTokensSmart(b, 0, EXISTS, LPAREN);
        r = r && subquery(b, l + 1);
        r = r && consumeToken(b, RPAREN);
        exit_section_(b, l, m, r, false, null);
        return r;
    }

    // [NOT]
    private static boolean exists_expression_0(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "exists_expression_0")) return false;
        consumeTokenSmart(b, NOT);
        return true;
    }

    // '+' | '-'
    private static boolean additive_expression_0(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "additive_expression_0")) return false;
        boolean r;
        r = consumeTokenSmart(b, PLUS);
        if (!r) r = consumeTokenSmart(b, MINUS);
        return r;
    }

    // '*' | '/'
    private static boolean multiplicative_expression_0(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "multiplicative_expression_0")) return false;
        boolean r;
        r = consumeTokenSmart(b, MUL);
        if (!r) r = consumeTokenSmart(b, DIV);
        return r;
    }

    public static boolean unary_arithmetic_expression(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "unary_arithmetic_expression")) return false;
        if (!nextTokenIsSmart(b, MINUS, PLUS)) return false;
        boolean r, p;
        Marker m = enter_section_(b, l, _NONE_, null);
        r = unary_arithmetic_expression_0(b, l + 1);
        p = r;
        r = p && expression(b, l, 2);
        exit_section_(b, l, m, UNARY_ARITHMETIC_EXPRESSION, r, p, null);
        return r || p;
    }

    // '+' | '-'
    private static boolean unary_arithmetic_expression_0(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "unary_arithmetic_expression_0")) return false;
        boolean r;
        r = consumeTokenSmart(b, PLUS);
        if (!r) r = consumeTokenSmart(b, MINUS);
        return r;
    }

    // LENGTH'('string_expression')'
    //     | LOCATE'('string_expression',' string_expression[',' simple_arithmetic_expression]')'
    //     | ABS'('simple_arithmetic_expression')'
    //     | SQRT'('simple_arithmetic_expression')'
    //     | MOD'('simple_arithmetic_expression',' simple_arithmetic_expression')'
    //     | SIZE'('reference_expression')'
    //     | INDEX'('identifier')'
    public static boolean functions_returning_numerics_expression(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "functions_returning_numerics_expression")) return false;
        boolean r;
        Marker m = enter_section_(b, l, _NONE_, FUNCTIONS_RETURNING_NUMERICS_EXPRESSION, "<functions returning numerics expression>");
        r = functions_returning_numerics_expression_0(b, l + 1);
        if (!r) r = functions_returning_numerics_expression_1(b, l + 1);
        if (!r) r = functions_returning_numerics_expression_2(b, l + 1);
        if (!r) r = functions_returning_numerics_expression_3(b, l + 1);
        if (!r) r = functions_returning_numerics_expression_4(b, l + 1);
        if (!r) r = functions_returning_numerics_expression_5(b, l + 1);
        if (!r) r = functions_returning_numerics_expression_6(b, l + 1);
        exit_section_(b, l, m, r, false, null);
        return r;
    }

    // LENGTH'('string_expression')'
    private static boolean functions_returning_numerics_expression_0(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "functions_returning_numerics_expression_0")) return false;
        boolean r;
        Marker m = enter_section_(b);
        r = consumeTokensSmart(b, 0, LENGTH, LPAREN);
        r = r && string_expression(b, l + 1);
        r = r && consumeToken(b, RPAREN);
        exit_section_(b, m, null, r);
        return r;
    }

    // LOCATE'('string_expression',' string_expression[',' simple_arithmetic_expression]')'
    private static boolean functions_returning_numerics_expression_1(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "functions_returning_numerics_expression_1")) return false;
        boolean r;
        Marker m = enter_section_(b);
        r = consumeTokensSmart(b, 0, LOCATE, LPAREN);
        r = r && string_expression(b, l + 1);
        r = r && consumeToken(b, COMMA);
        r = r && string_expression(b, l + 1);
        r = r && functions_returning_numerics_expression_1_5(b, l + 1);
        r = r && consumeToken(b, RPAREN);
        exit_section_(b, m, null, r);
        return r;
    }

    // [',' simple_arithmetic_expression]
    private static boolean functions_returning_numerics_expression_1_5(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "functions_returning_numerics_expression_1_5")) return false;
        functions_returning_numerics_expression_1_5_0(b, l + 1);
        return true;
    }

    // ',' simple_arithmetic_expression
    private static boolean functions_returning_numerics_expression_1_5_0(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "functions_returning_numerics_expression_1_5_0")) return false;
        boolean r;
        Marker m = enter_section_(b);
        r = consumeTokenSmart(b, COMMA);
        r = r && simple_arithmetic_expression(b, l + 1);
        exit_section_(b, m, null, r);
        return r;
    }

    // ABS'('simple_arithmetic_expression')'
    private static boolean functions_returning_numerics_expression_2(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "functions_returning_numerics_expression_2")) return false;
        boolean r;
        Marker m = enter_section_(b);
        r = consumeTokensSmart(b, 0, ABS, LPAREN);
        r = r && simple_arithmetic_expression(b, l + 1);
        r = r && consumeToken(b, RPAREN);
        exit_section_(b, m, null, r);
        return r;
    }

    // SQRT'('simple_arithmetic_expression')'
    private static boolean functions_returning_numerics_expression_3(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "functions_returning_numerics_expression_3")) return false;
        boolean r;
        Marker m = enter_section_(b);
        r = consumeTokensSmart(b, 0, SQRT, LPAREN);
        r = r && simple_arithmetic_expression(b, l + 1);
        r = r && consumeToken(b, RPAREN);
        exit_section_(b, m, null, r);
        return r;
    }

    // MOD'('simple_arithmetic_expression',' simple_arithmetic_expression')'
    private static boolean functions_returning_numerics_expression_4(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "functions_returning_numerics_expression_4")) return false;
        boolean r;
        Marker m = enter_section_(b);
        r = consumeTokensSmart(b, 0, MOD, LPAREN);
        r = r && simple_arithmetic_expression(b, l + 1);
        r = r && consumeToken(b, COMMA);
        r = r && simple_arithmetic_expression(b, l + 1);
        r = r && consumeToken(b, RPAREN);
        exit_section_(b, m, null, r);
        return r;
    }

    // SIZE'('reference_expression')'
    private static boolean functions_returning_numerics_expression_5(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "functions_returning_numerics_expression_5")) return false;
        boolean r;
        Marker m = enter_section_(b);
        r = consumeTokensSmart(b, 0, SIZE, LPAREN);
        r = r && reference_expression(b, l + 1);
        r = r && consumeToken(b, RPAREN);
        exit_section_(b, m, null, r);
        return r;
    }

    // INDEX'('identifier')'
    private static boolean functions_returning_numerics_expression_6(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "functions_returning_numerics_expression_6")) return false;
        boolean r;
        Marker m = enter_section_(b);
        r = consumeTokensSmart(b, 0, INDEX, LPAREN);
        r = r && identifier(b, l + 1);
        r = r && consumeToken(b, RPAREN);
        exit_section_(b, m, null, r);
        return r;
    }

    public static boolean function_invocation_expression(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "function_invocation_expression")) return false;
        if (!nextTokenIsSmart(b, FUNCTION)) return false;
        boolean r, p;
        Marker m = enter_section_(b, l, _NONE_, null);
        r = parseTokensSmart(b, 0, FUNCTION, LPAREN);
        p = r;
        r = p && expression(b, l, 3);
        r = p && report_error_(b, function_invocation_expression_1(b, l + 1)) && r;
        exit_section_(b, l, m, FUNCTION_INVOCATION_EXPRESSION, r, p, null);
        return r || p;
    }

    // {',' function_arg}* ')'
    private static boolean function_invocation_expression_1(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "function_invocation_expression_1")) return false;
        boolean r;
        Marker m = enter_section_(b);
        r = function_invocation_expression_1_0(b, l + 1);
        r = r && consumeToken(b, RPAREN);
        exit_section_(b, m, null, r);
        return r;
    }

    // {',' function_arg}*
    private static boolean function_invocation_expression_1_0(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "function_invocation_expression_1_0")) return false;
        while (true) {
            int c = current_position_(b);
            if (!function_invocation_expression_1_0_0(b, l + 1)) break;
            if (!empty_element_parsed_guard_(b, "function_invocation_expression_1_0", c)) break;
        }
        return true;
    }

    // ',' function_arg
    private static boolean function_invocation_expression_1_0_0(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "function_invocation_expression_1_0_0")) return false;
        boolean r;
        Marker m = enter_section_(b);
        r = consumeToken(b, COMMA);
        r = r && function_arg(b, l + 1);
        exit_section_(b, m, null, r);
        return r;
    }

    // { ALL'(' | ANY'(' | SOME'('} subquery')'
    public static boolean all_or_any_expression(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "all_or_any_expression")) return false;
        boolean r;
        Marker m = enter_section_(b, l, _NONE_, ALL_OR_ANY_EXPRESSION, "<all or any expression>");
        r = all_or_any_expression_0(b, l + 1);
        r = r && subquery(b, l + 1);
        r = r && consumeToken(b, RPAREN);
        exit_section_(b, l, m, r, false, null);
        return r;
    }

    // ALL'(' | ANY'(' | SOME'('
    private static boolean all_or_any_expression_0(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "all_or_any_expression_0")) return false;
        boolean r;
        Marker m = enter_section_(b);
        r = parseTokensSmart(b, 0, ALL, LPAREN);
        if (!r) r = parseTokensSmart(b, 0, ANY, LPAREN);
        if (!r) r = parseTokensSmart(b, 0, SOME, LPAREN);
        exit_section_(b, m, null, r);
        return r;
    }

    // string
    public static boolean string_literal(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "string_literal")) return false;
        if (!nextTokenIsSmart(b, STRING)) return false;
        boolean r;
        Marker m = enter_section_(b);
        r = consumeTokenSmart(b, STRING);
        exit_section_(b, m, STRING_LITERAL, r);
        return r;
    }

    // boolean
    public static boolean boolean_literal(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "boolean_literal")) return false;
        if (!nextTokenIsSmart(b, BOOLEAN)) return false;
        boolean r;
        Marker m = enter_section_(b);
        r = consumeTokenSmart(b, BOOLEAN);
        exit_section_(b, m, BOOLEAN_LITERAL, r);
        return r;
    }

    // datetime
    public static boolean datetime_literal(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "datetime_literal")) return false;
        if (!nextTokenIsSmart(b, DATETIME)) return false;
        boolean r;
        Marker m = enter_section_(b);
        r = consumeTokenSmart(b, DATETIME);
        exit_section_(b, m, DATETIME_LITERAL, r);
        return r;
    }

    // numeric
    public static boolean numeric_literal(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "numeric_literal")) return false;
        if (!nextTokenIsSmart(b, NUMERIC)) return false;
        boolean r;
        Marker m = enter_section_(b);
        r = consumeTokenSmart(b, NUMERIC);
        exit_section_(b, m, NUMERIC_LITERAL, r);
        return r;
    }

    // named_input_parameter | numeric_input_parameter
    public static boolean input_parameter_expression(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "input_parameter_expression")) return false;
        if (!nextTokenIsSmart(b, NAMED_INPUT_PARAMETER, NUMERIC_INPUT_PARAMETER)) return false;
        boolean r;
        Marker m = enter_section_(b, l, _NONE_, INPUT_PARAMETER_EXPRESSION, "<input parameter expression>");
        r = consumeTokenSmart(b, NAMED_INPUT_PARAMETER);
        if (!r) r = consumeTokenSmart(b, NUMERIC_INPUT_PARAMETER);
        exit_section_(b, l, m, r, false, null);
        return r;
    }

    // '(' subquery ')' [alias_declaration]
    public static boolean subquery_expression(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "subquery_expression")) return false;
        if (!nextTokenIsSmart(b, LPAREN)) return false;
        boolean r;
        Marker m = enter_section_(b);
        r = consumeTokenSmart(b, LPAREN);
        r = r && subquery(b, l + 1);
        r = r && consumeToken(b, RPAREN);
        r = r && subquery_expression_3(b, l + 1);
        exit_section_(b, m, SUBQUERY_EXPRESSION, r);
        return r;
    }

    // [alias_declaration]
    private static boolean subquery_expression_3(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "subquery_expression_3")) return false;
        alias_declaration(b, l + 1);
        return true;
    }

    // CONCAT'('string_expression',' string_expression {',' string_expression}*')'
    //     | SUBSTRING'('string_expression',' arithmetic_expression [',' arithmetic_expression]')'
    //     | TRIM'('[[trim_specification] [string_literal] FROM] string_expression')'
    //     | LOWER'('string_expression')'
    //     | UPPER'('string_expression')'
    public static boolean string_function_expression(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "string_function_expression")) return false;
        boolean r;
        Marker m = enter_section_(b, l, _NONE_, STRING_FUNCTION_EXPRESSION, "<string function expression>");
        r = string_function_expression_0(b, l + 1);
        if (!r) r = string_function_expression_1(b, l + 1);
        if (!r) r = string_function_expression_2(b, l + 1);
        if (!r) r = string_function_expression_3(b, l + 1);
        if (!r) r = string_function_expression_4(b, l + 1);
        exit_section_(b, l, m, r, false, null);
        return r;
    }

    // CONCAT'('string_expression',' string_expression {',' string_expression}*')'
    private static boolean string_function_expression_0(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "string_function_expression_0")) return false;
        boolean r;
        Marker m = enter_section_(b);
        r = consumeTokensSmart(b, 0, CONCAT, LPAREN);
        r = r && string_expression(b, l + 1);
        r = r && consumeToken(b, COMMA);
        r = r && string_expression(b, l + 1);
        r = r && string_function_expression_0_5(b, l + 1);
        r = r && consumeToken(b, RPAREN);
        exit_section_(b, m, null, r);
        return r;
    }

    // {',' string_expression}*
    private static boolean string_function_expression_0_5(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "string_function_expression_0_5")) return false;
        while (true) {
            int c = current_position_(b);
            if (!string_function_expression_0_5_0(b, l + 1)) break;
            if (!empty_element_parsed_guard_(b, "string_function_expression_0_5", c)) break;
        }
        return true;
    }

    // ',' string_expression
    private static boolean string_function_expression_0_5_0(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "string_function_expression_0_5_0")) return false;
        boolean r;
        Marker m = enter_section_(b);
        r = consumeTokenSmart(b, COMMA);
        r = r && string_expression(b, l + 1);
        exit_section_(b, m, null, r);
        return r;
    }

    // SUBSTRING'('string_expression',' arithmetic_expression [',' arithmetic_expression]')'
    private static boolean string_function_expression_1(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "string_function_expression_1")) return false;
        boolean r;
        Marker m = enter_section_(b);
        r = consumeTokensSmart(b, 0, SUBSTRING, LPAREN);
        r = r && string_expression(b, l + 1);
        r = r && consumeToken(b, COMMA);
        r = r && expression(b, l + 1, 1);
        r = r && string_function_expression_1_5(b, l + 1);
        r = r && consumeToken(b, RPAREN);
        exit_section_(b, m, null, r);
        return r;
    }

    // [',' arithmetic_expression]
    private static boolean string_function_expression_1_5(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "string_function_expression_1_5")) return false;
        string_function_expression_1_5_0(b, l + 1);
        return true;
    }

    // ',' arithmetic_expression
    private static boolean string_function_expression_1_5_0(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "string_function_expression_1_5_0")) return false;
        boolean r;
        Marker m = enter_section_(b);
        r = consumeTokenSmart(b, COMMA);
        r = r && expression(b, l + 1, 1);
        exit_section_(b, m, null, r);
        return r;
    }

    // TRIM'('[[trim_specification] [string_literal] FROM] string_expression')'
    private static boolean string_function_expression_2(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "string_function_expression_2")) return false;
        boolean r;
        Marker m = enter_section_(b);
        r = consumeTokensSmart(b, 0, TRIM, LPAREN);
        r = r && string_function_expression_2_2(b, l + 1);
        r = r && string_expression(b, l + 1);
        r = r && consumeToken(b, RPAREN);
        exit_section_(b, m, null, r);
        return r;
    }

    // [[trim_specification] [string_literal] FROM]
    private static boolean string_function_expression_2_2(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "string_function_expression_2_2")) return false;
        string_function_expression_2_2_0(b, l + 1);
        return true;
    }

    // [trim_specification] [string_literal] FROM
    private static boolean string_function_expression_2_2_0(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "string_function_expression_2_2_0")) return false;
        boolean r;
        Marker m = enter_section_(b);
        r = string_function_expression_2_2_0_0(b, l + 1);
        r = r && string_function_expression_2_2_0_1(b, l + 1);
        r = r && consumeToken(b, FROM);
        exit_section_(b, m, null, r);
        return r;
    }

    // [trim_specification]
    private static boolean string_function_expression_2_2_0_0(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "string_function_expression_2_2_0_0")) return false;
        trim_specification(b, l + 1);
        return true;
    }

    // [string_literal]
    private static boolean string_function_expression_2_2_0_1(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "string_function_expression_2_2_0_1")) return false;
        string_literal(b, l + 1);
        return true;
    }

    // LOWER'('string_expression')'
    private static boolean string_function_expression_3(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "string_function_expression_3")) return false;
        boolean r;
        Marker m = enter_section_(b);
        r = consumeTokensSmart(b, 0, LOWER, LPAREN);
        r = r && string_expression(b, l + 1);
        r = r && consumeToken(b, RPAREN);
        exit_section_(b, m, null, r);
        return r;
    }

    // UPPER'('string_expression')'
    private static boolean string_function_expression_4(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "string_function_expression_4")) return false;
        boolean r;
        Marker m = enter_section_(b);
        r = consumeTokensSmart(b, 0, UPPER, LPAREN);
        r = r && string_expression(b, l + 1);
        r = r && consumeToken(b, RPAREN);
        exit_section_(b, m, null, r);
        return r;
    }

    // { AVG'(' | MAX'(' | MIN'(' | SUM'(' | COUNT'(' } [DISTINCT] reference_expression')' | function_invocation_expression
    public static boolean aggregate_expression(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "aggregate_expression")) return false;
        boolean r;
        Marker m = enter_section_(b, l, _COLLAPSE_, AGGREGATE_EXPRESSION, "<aggregate expression>");
        r = aggregate_expression_0(b, l + 1);
        if (!r) r = function_invocation_expression(b, l + 1);
        exit_section_(b, l, m, r, false, null);
        return r;
    }

    // { AVG'(' | MAX'(' | MIN'(' | SUM'(' | COUNT'(' } [DISTINCT] reference_expression')'
    private static boolean aggregate_expression_0(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "aggregate_expression_0")) return false;
        boolean r;
        Marker m = enter_section_(b);
        r = aggregate_expression_0_0(b, l + 1);
        r = r && aggregate_expression_0_1(b, l + 1);
        r = r && reference_expression(b, l + 1);
        r = r && consumeToken(b, RPAREN);
        exit_section_(b, m, null, r);
        return r;
    }

    // AVG'(' | MAX'(' | MIN'(' | SUM'(' | COUNT'('
    private static boolean aggregate_expression_0_0(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "aggregate_expression_0_0")) return false;
        boolean r;
        Marker m = enter_section_(b);
        r = parseTokensSmart(b, 0, AVG, LPAREN);
        if (!r) r = parseTokensSmart(b, 0, MAX, LPAREN);
        if (!r) r = parseTokensSmart(b, 0, MIN, LPAREN);
        if (!r) r = parseTokensSmart(b, 0, SUM, LPAREN);
        if (!r) r = parseTokensSmart(b, 0, COUNT, LPAREN);
        exit_section_(b, m, null, r);
        return r;
    }

    // [DISTINCT]
    private static boolean aggregate_expression_0_1(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "aggregate_expression_0_1")) return false;
        consumeTokenSmart(b, DISTINCT);
        return true;
    }

    // CASE when_clause {when_clause}* ELSE scalar_expression END
    public static boolean general_case_expression(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "general_case_expression")) return false;
        if (!nextTokenIsSmart(b, CASE)) return false;
        boolean r;
        Marker m = enter_section_(b);
        r = consumeTokenSmart(b, CASE);
        r = r && when_clause(b, l + 1);
        r = r && general_case_expression_2(b, l + 1);
        r = r && consumeToken(b, ELSE);
        r = r && scalar_expression(b, l + 1);
        r = r && consumeToken(b, END);
        exit_section_(b, m, GENERAL_CASE_EXPRESSION, r);
        return r;
    }

    // {when_clause}*
    private static boolean general_case_expression_2(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "general_case_expression_2")) return false;
        while (true) {
            int c = current_position_(b);
            if (!general_case_expression_2_0(b, l + 1)) break;
            if (!empty_element_parsed_guard_(b, "general_case_expression_2", c)) break;
        }
        return true;
    }

    // {when_clause}
    private static boolean general_case_expression_2_0(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "general_case_expression_2_0")) return false;
        boolean r;
        Marker m = enter_section_(b);
        r = when_clause(b, l + 1);
        exit_section_(b, m, null, r);
        return r;
    }

    // CASE case_operand simple_when_clause {simple_when_clause}*
    //     ELSE scalar_expression
    //     END
    public static boolean simple_case_expression(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "simple_case_expression")) return false;
        if (!nextTokenIsSmart(b, CASE)) return false;
        boolean r;
        Marker m = enter_section_(b);
        r = consumeTokenSmart(b, CASE);
        r = r && case_operand(b, l + 1);
        r = r && simple_when_clause(b, l + 1);
        r = r && simple_case_expression_3(b, l + 1);
        r = r && consumeToken(b, ELSE);
        r = r && scalar_expression(b, l + 1);
        r = r && consumeToken(b, END);
        exit_section_(b, m, SIMPLE_CASE_EXPRESSION, r);
        return r;
    }

    // {simple_when_clause}*
    private static boolean simple_case_expression_3(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "simple_case_expression_3")) return false;
        while (true) {
            int c = current_position_(b);
            if (!simple_case_expression_3_0(b, l + 1)) break;
            if (!empty_element_parsed_guard_(b, "simple_case_expression_3", c)) break;
        }
        return true;
    }

    // {simple_when_clause}
    private static boolean simple_case_expression_3_0(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "simple_case_expression_3_0")) return false;
        boolean r;
        Marker m = enter_section_(b);
        r = simple_when_clause(b, l + 1);
        exit_section_(b, m, null, r);
        return r;
    }

    // COALESCE'('scalar_expression {',' scalar_expression}+')'
    public static boolean coalesce_expression(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "coalesce_expression")) return false;
        if (!nextTokenIsSmart(b, COALESCE)) return false;
        boolean r;
        Marker m = enter_section_(b);
        r = consumeTokensSmart(b, 0, COALESCE, LPAREN);
        r = r && scalar_expression(b, l + 1);
        r = r && coalesce_expression_3(b, l + 1);
        r = r && consumeToken(b, RPAREN);
        exit_section_(b, m, COALESCE_EXPRESSION, r);
        return r;
    }

    // {',' scalar_expression}+
    private static boolean coalesce_expression_3(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "coalesce_expression_3")) return false;
        boolean r;
        Marker m = enter_section_(b);
        r = coalesce_expression_3_0(b, l + 1);
        while (r) {
            int c = current_position_(b);
            if (!coalesce_expression_3_0(b, l + 1)) break;
            if (!empty_element_parsed_guard_(b, "coalesce_expression_3", c)) break;
        }
        exit_section_(b, m, null, r);
        return r;
    }

    // ',' scalar_expression
    private static boolean coalesce_expression_3_0(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "coalesce_expression_3_0")) return false;
        boolean r;
        Marker m = enter_section_(b);
        r = consumeTokenSmart(b, COMMA);
        r = r && scalar_expression(b, l + 1);
        exit_section_(b, m, null, r);
        return r;
    }

    // NULLIF'('scalar_expression',' scalar_expression')'
    public static boolean nullif_expression(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "nullif_expression")) return false;
        if (!nextTokenIsSmart(b, NULLIF)) return false;
        boolean r;
        Marker m = enter_section_(b);
        r = consumeTokensSmart(b, 0, NULLIF, LPAREN);
        r = r && scalar_expression(b, l + 1);
        r = r && consumeToken(b, COMMA);
        r = r && scalar_expression(b, l + 1);
        r = r && consumeToken(b, RPAREN);
        exit_section_(b, m, NULLIF_EXPRESSION, r);
        return r;
    }

    // datetime_function
    public static boolean datetime_function_expression(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "datetime_function_expression")) return false;
        boolean r;
        Marker m = enter_section_(b, l, _NONE_, DATETIME_FUNCTION_EXPRESSION, "<datetime function expression>");
        r = datetime_function(b, l + 1);
        exit_section_(b, l, m, r, false, null);
        return r;
    }

    // TYPE'('{ reference_expression | input_parameter_expression }')' | type_literal
    public static boolean type_expression(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "type_expression")) return false;
        boolean r;
        Marker m = enter_section_(b, l, _NONE_, TYPE_EXPRESSION, "<type expression>");
        r = type_expression_0(b, l + 1);
        if (!r) r = type_literal(b, l + 1);
        exit_section_(b, l, m, r, false, null);
        return r;
    }

    // TYPE'('{ reference_expression | input_parameter_expression }')'
    private static boolean type_expression_0(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "type_expression_0")) return false;
        boolean r;
        Marker m = enter_section_(b);
        r = consumeTokensSmart(b, 0, TYPE, LPAREN);
        r = r && type_expression_0_2(b, l + 1);
        r = r && consumeToken(b, RPAREN);
        exit_section_(b, m, null, r);
        return r;
    }

    // reference_expression | input_parameter_expression
    private static boolean type_expression_0_2(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "type_expression_0_2")) return false;
        boolean r;
        r = reference_expression(b, l + 1);
        if (!r) r = input_parameter_expression(b, l + 1);
        return r;
    }

    // path_reference_expression | table_expression | asterisk
    public static boolean reference_expression(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "reference_expression")) return false;
        boolean r;
        Marker m = enter_section_(b, l, _COLLAPSE_, REFERENCE_EXPRESSION, "<reference expression>");
        r = path_reference_expression(b, l + 1);
        if (!r) r = table_expression(b, l + 1);
        if (!r) r = asterisk(b, l + 1);
        exit_section_(b, l, m, r, false, null);
        return r;
    }

    public static boolean paren_expression(PsiBuilder b, int l) {
        if (!recursion_guard_(b, l, "paren_expression")) return false;
        if (!nextTokenIsSmart(b, LPAREN)) return false;
        boolean r, p;
        Marker m = enter_section_(b, l, _NONE_, null);
        r = consumeTokenSmart(b, LPAREN);
        p = r;
        r = p && expression(b, l, -1);
        r = p && report_error_(b, consumeToken(b, RPAREN)) && r;
        exit_section_(b, l, m, PAREN_EXPRESSION, r, p, null);
        return r || p;
    }

}
