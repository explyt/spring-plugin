// This is a generated file. Not intended for manual editing.
package com.esprito.jpa.ql.parser;

import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilder.Marker;
import static com.esprito.jpa.ql.psi.JpqlTypes.*;
import static com.esprito.jpa.ql.parser.JpqlParserUtil.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.lang.ASTNode;
import com.intellij.psi.tree.TokenSet;
import com.intellij.lang.PsiParser;
import com.intellij.lang.LightPsiParser;

@SuppressWarnings({"SimplifiableIfStatement", "UnusedAssignment"})
public class JpqlParser implements PsiParser, LightPsiParser {

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
    return QL_file(b, l + 1);
  }

  public static final TokenSet[] EXTENDS_SETS_ = new TokenSet[] {
    create_token_set_(ADDITIVE_EXPRESSION, AGGREGATE_EXPRESSION, ALL_OR_ANY_EXPRESSION, BETWEEN_EXPRESSION,
      BOOLEAN_LITERAL, CASE_EXPRESSION, COALESCE_EXPRESSION, COLLECTION_MEMBER_EXPRESSION,
      COMPARISON_EXPRESSION, CONDITIONAL_AND_EXPRESSION, CONDITIONAL_NOT_EXPRESSION, CONDITIONAL_OR_EXPRESSION,
      CONSTRUCTOR_EXPRESSION, DATETIME_FUNCTION_EXPRESSION, DATETIME_LITERAL, EMPTY_COLLECTION_COMPARISON_EXPRESSION,
      ENTITY_OR_VALUE_EXPRESSION, EXISTS_EXPRESSION, EXPRESSION, FUNCTIONS_RETURNING_NUMERICS_EXPRESSION,
      FUNCTION_INVOCATION_EXPRESSION, GENERAL_CASE_EXPRESSION, INPUT_PARAMETER_EXPRESSION, IN_EXPRESSION,
      JOIN_EXPRESSION, LIKE_EXPRESSION, MAP_BASED_REFERENCE_EXPRESSION, MULTIPLICATIVE_EXPRESSION,
      NULLIF_EXPRESSION, NULL_COMPARISON_EXPRESSION, NUMERIC_LITERAL, OBJECT_EXPRESSION,
      PAREN_EXPRESSION, REFERENCE_EXPRESSION, SIMPLE_CASE_EXPRESSION, SIMPLE_ENTITY_OR_VALUE_EXPRESSION,
      STRING_FUNCTION_EXPRESSION, STRING_LITERAL, SUBQUERY_EXPRESSION, TYPE_EXPRESSION,
      UNARY_ARITHMETIC_EXPRESSION),
  };

  /* ********************************************************** */
  // semicolon_delimited_statements | QL_statement
  static boolean QL_file(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "QL_file")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = semicolon_delimited_statements(b, l + 1);
    if (!r) r = QL_statement(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // select_statement | update_statement | delete_statement
  public static boolean QL_statement(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "QL_statement")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, QL_STATEMENT, "<ql statement>");
    r = select_statement(b, l + 1);
    if (!r) r = update_statement(b, l + 1);
    if (!r) r = delete_statement(b, l + 1);
    exit_section_(b, l, m, r, false, JpqlParser::statement_recover);
    return r;
  }

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
  // reference_expression
  //     | input_parameter_expression
  //     | case_expression
  //     | boolean_literal
  //     | function_invocation_expression
  static boolean boolean_expression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "boolean_expression")) return false;
    boolean r;
    r = reference_expression(b, l + 1);
    if (!r) r = input_parameter_expression(b, l + 1);
    if (!r) r = case_expression(b, l + 1);
    if (!r) r = boolean_literal(b, l + 1);
    if (!r) r = function_invocation_expression(b, l + 1);
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
  // COALESCE'('scalar_expression {',' scalar_expression}+')'
  public static boolean coalesce_expression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "coalesce_expression")) return false;
    if (!nextTokenIs(b, COALESCE)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, COALESCE, LPAREN);
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
    r = consumeToken(b, COMMA);
    r = r && scalar_expression(b, l + 1);
    exit_section_(b, m, null, r);
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
  // expression {',' expression}*
  public static boolean constructor_arguments_list(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "constructor_arguments_list")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, CONSTRUCTOR_ARGUMENTS_LIST, "<constructor arguments list>");
    r = expression(b, l + 1, -1);
    r = r && constructor_arguments_list_1(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // {',' expression}*
  private static boolean constructor_arguments_list_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "constructor_arguments_list_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!constructor_arguments_list_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "constructor_arguments_list_1", c)) break;
    }
    return true;
  }

  // ',' expression
  private static boolean constructor_arguments_list_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "constructor_arguments_list_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COMMA);
    r = r && expression(b, l + 1, -1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // NEW reference_expression '(' constructor_arguments_list ')'
  public static boolean constructor_expression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "constructor_expression")) return false;
    if (!nextTokenIs(b, NEW)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, NEW);
    r = r && reference_expression(b, l + 1);
    r = r && consumeToken(b, LPAREN);
    r = r && constructor_arguments_list(b, l + 1);
    r = r && consumeToken(b, RPAREN);
    exit_section_(b, m, CONSTRUCTOR_EXPRESSION, r);
    return r;
  }

  /* ********************************************************** */
  // reference_expression
  //     | input_parameter_expression
  //     | datetime_function_expression
  //     | aggregate_expression
  //     | case_expression
  //     | datetime_literal
  //     | function_invocation_expression
  static boolean datetime_expression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "datetime_expression")) return false;
    boolean r;
    r = reference_expression(b, l + 1);
    if (!r) r = input_parameter_expression(b, l + 1);
    if (!r) r = datetime_function_expression(b, l + 1);
    if (!r) r = aggregate_expression(b, l + 1);
    if (!r) r = case_expression(b, l + 1);
    if (!r) r = datetime_literal(b, l + 1);
    if (!r) r = function_invocation_expression(b, l + 1);
    return r;
  }

  /* ********************************************************** */
  // CURRENT_DATE |
  //     CURRENT_TIME |
  //     CURRENT_TIMESTAMP
  public static boolean datetime_function(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "datetime_function")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, DATETIME_FUNCTION, "<datetime function>");
    r = consumeToken(b, CURRENT_DATE);
    if (!r) r = consumeToken(b, CURRENT_TIME);
    if (!r) r = consumeToken(b, CURRENT_TIMESTAMP);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // DELETE FROM identifier [alias_declaration]
  public static boolean delete_clause(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "delete_clause")) return false;
    if (!nextTokenIs(b, DELETE)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, DELETE, FROM);
    r = r && identifier(b, l + 1);
    r = r && delete_clause_3(b, l + 1);
    exit_section_(b, m, DELETE_CLAUSE, r);
    return r;
  }

  // [alias_declaration]
  private static boolean delete_clause_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "delete_clause_3")) return false;
    alias_declaration(b, l + 1);
    return true;
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
  //     | simple_entity_or_value_expression
  public static boolean entity_or_value_expression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "entity_or_value_expression")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _COLLAPSE_, ENTITY_OR_VALUE_EXPRESSION, "<entity or value expression>");
    r = reference_expression(b, l + 1);
    if (!r) r = simple_entity_or_value_expression(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // type_expression
  //     | reference_expression
  //     | input_parameter_expression
  static boolean entity_type_expression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "entity_type_expression")) return false;
    boolean r;
    r = type_expression(b, l + 1);
    if (!r) r = reference_expression(b, l + 1);
    if (!r) r = input_parameter_expression(b, l + 1);
    return r;
  }

  /* ********************************************************** */
  // numeric_or_input_parameter [PERCENT]
  public static boolean fetchCountOrPercent(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "fetchCountOrPercent")) return false;
    if (!nextTokenIs(b, "<fetch count or percent>", COLON, NUMERIC)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, FETCH_COUNT_OR_PERCENT, "<fetch count or percent>");
    r = numeric_or_input_parameter(b, l + 1);
    r = r && fetchCountOrPercent_1(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // [PERCENT]
  private static boolean fetchCountOrPercent_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "fetchCountOrPercent_1")) return false;
    consumeToken(b, PERCENT);
    return true;
  }

  /* ********************************************************** */
  // FETCH (FIRST | NEXT) fetchCountOrPercent (ROW | ROWS) (ONLY | WITH TIES)
  public static boolean fetch_clause(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "fetch_clause")) return false;
    if (!nextTokenIs(b, FETCH)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, FETCH);
    r = r && fetch_clause_1(b, l + 1);
    r = r && fetchCountOrPercent(b, l + 1);
    r = r && fetch_clause_3(b, l + 1);
    r = r && fetch_clause_4(b, l + 1);
    exit_section_(b, m, FETCH_CLAUSE, r);
    return r;
  }

  // FIRST | NEXT
  private static boolean fetch_clause_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "fetch_clause_1")) return false;
    boolean r;
    r = consumeToken(b, FIRST);
    if (!r) r = consumeToken(b, NEXT);
    return r;
  }

  // ROW | ROWS
  private static boolean fetch_clause_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "fetch_clause_3")) return false;
    boolean r;
    r = consumeToken(b, ROW);
    if (!r) r = consumeToken(b, ROWS);
    return r;
  }

  // ONLY | WITH TIES
  private static boolean fetch_clause_4(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "fetch_clause_4")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, ONLY);
    if (!r) r = parseTokens(b, 0, WITH, TIES);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // join_spec FETCH reference_expression alias_declaration
  public static boolean fetch_join(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "fetch_join")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, FETCH_JOIN, "<fetch join>");
    r = join_spec(b, l + 1);
    r = r && consumeToken(b, FETCH);
    r = r && reference_expression(b, l + 1);
    r = r && alias_declaration(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // FROM identification_variable_declaration {',' {identification_variable_declaration | collection_member_declaration}}*
  public static boolean from_clause(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "from_clause")) return false;
    if (!nextTokenIs(b, FROM)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, FROM_CLAUSE, null);
    r = consumeToken(b, FROM);
    p = r; // pin = FROM
    r = r && report_error_(b, identification_variable_declaration(b, l + 1));
    r = p && from_clause_2(b, l + 1) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // {',' {identification_variable_declaration | collection_member_declaration}}*
  private static boolean from_clause_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "from_clause_2")) return false;
    while (true) {
      int c = current_position_(b);
      if (!from_clause_2_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "from_clause_2", c)) break;
    }
    return true;
  }

  // ',' {identification_variable_declaration | collection_member_declaration}
  private static boolean from_clause_2_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "from_clause_2_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COMMA);
    r = r && from_clause_2_0_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // identification_variable_declaration | collection_member_declaration
  private static boolean from_clause_2_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "from_clause_2_0_1")) return false;
    boolean r;
    r = identification_variable_declaration(b, l + 1);
    if (!r) r = collection_member_declaration(b, l + 1);
    return r;
  }

  /* ********************************************************** */
  // literal_group
  //     | reference_expression
  //     | input_parameter_expression
  //     | scalar_expression
  public static boolean function_arg(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "function_arg")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, FUNCTION_ARG, "<function arg>");
    r = expression(b, l + 1, 4);
    if (!r) r = reference_expression(b, l + 1);
    if (!r) r = input_parameter_expression(b, l + 1);
    if (!r) r = scalar_expression(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // CASE when_clause {when_clause}* ELSE scalar_expression END
  public static boolean general_case_expression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "general_case_expression")) return false;
    if (!nextTokenIs(b, CASE)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, CASE);
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
  // range_variable_declaration { join_expression | fetch_join }*
  public static boolean identification_variable_declaration(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "identification_variable_declaration")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, IDENTIFICATION_VARIABLE_DECLARATION, "<identification variable declaration>");
    r = range_variable_declaration(b, l + 1);
    r = r && identification_variable_declaration_1(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // { join_expression | fetch_join }*
  private static boolean identification_variable_declaration_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "identification_variable_declaration_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!identification_variable_declaration_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "identification_variable_declaration_1", c)) break;
    }
    return true;
  }

  // join_expression | fetch_join
  private static boolean identification_variable_declaration_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "identification_variable_declaration_1_0")) return false;
    boolean r;
    r = join_expression(b, l + 1);
    if (!r) r = fetch_join(b, l + 1);
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
  // ON conditional_group
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
  // join_spec reference_expression alias_declaration [join_condition]
  public static boolean join_expression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "join_expression")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, JOIN_EXPRESSION, "<join expression>");
    r = join_spec(b, l + 1);
    r = r && reference_expression(b, l + 1);
    r = r && alias_declaration(b, l + 1);
    r = r && join_expression_3(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // [join_condition]
  private static boolean join_expression_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "join_expression_3")) return false;
    join_condition(b, l + 1);
    return true;
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
  // KEY'('reference_expression')'
  //     | VALUE'('reference_expression')'
  //     | ENTRY'('reference_expression')'
  public static boolean map_based_reference_expression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "map_based_reference_expression")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, MAP_BASED_REFERENCE_EXPRESSION, "<map based reference expression>");
    r = map_based_reference_expression_0(b, l + 1);
    if (!r) r = map_based_reference_expression_1(b, l + 1);
    if (!r) r = map_based_reference_expression_2(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // KEY'('reference_expression')'
  private static boolean map_based_reference_expression_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "map_based_reference_expression_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, KEY, LPAREN);
    r = r && reference_expression(b, l + 1);
    r = r && consumeToken(b, RPAREN);
    exit_section_(b, m, null, r);
    return r;
  }

  // VALUE'('reference_expression')'
  private static boolean map_based_reference_expression_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "map_based_reference_expression_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, VALUE, LPAREN);
    r = r && reference_expression(b, l + 1);
    r = r && consumeToken(b, RPAREN);
    exit_section_(b, m, null, r);
    return r;
  }

  // ENTRY'('reference_expression')'
  private static boolean map_based_reference_expression_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "map_based_reference_expression_2")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, ENTRY, LPAREN);
    r = r && reference_expression(b, l + 1);
    r = r && consumeToken(b, RPAREN);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // scalar_expression | NULL
  static boolean new_value(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "new_value")) return false;
    boolean r;
    r = scalar_expression(b, l + 1);
    if (!r) r = consumeToken(b, NULL);
    return r;
  }

  /* ********************************************************** */
  // NULLIF'('scalar_expression',' scalar_expression')'
  public static boolean nullif_expression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "nullif_expression")) return false;
    if (!nextTokenIs(b, NULLIF)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, NULLIF, LPAREN);
    r = r && scalar_expression(b, l + 1);
    r = r && consumeToken(b, COMMA);
    r = r && scalar_expression(b, l + 1);
    r = r && consumeToken(b, RPAREN);
    exit_section_(b, m, NULLIF_EXPRESSION, r);
    return r;
  }

  /* ********************************************************** */
  // numeric_literal | input_parameter_expression
  static boolean numeric_or_input_parameter(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "numeric_or_input_parameter")) return false;
    if (!nextTokenIs(b, "", COLON, NUMERIC)) return false;
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
  // identifier [alias_declaration]
  public static boolean range_variable_declaration(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "range_variable_declaration")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, RANGE_VARIABLE_DECLARATION, "<range variable declaration>");
    r = identifier(b, l + 1);
    r = r && range_variable_declaration_1(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // [alias_declaration]
  private static boolean range_variable_declaration_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "range_variable_declaration_1")) return false;
    alias_declaration(b, l + 1);
    return true;
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
  //     | reference_expression
  //     | input_parameter_expression
  //     | case_expression
  //     | datetime_expression
  //     | literal_group
  //     | boolean_expression
  //     | entity_type_expression
  static boolean scalar_expression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "scalar_expression")) return false;
    boolean r;
    r = simple_arithmetic_expression(b, l + 1);
    if (!r) r = string_expression(b, l + 1);
    if (!r) r = reference_expression(b, l + 1);
    if (!r) r = input_parameter_expression(b, l + 1);
    if (!r) r = case_expression(b, l + 1);
    if (!r) r = datetime_expression(b, l + 1);
    if (!r) r = expression(b, l + 1, 4);
    if (!r) r = boolean_expression(b, l + 1);
    if (!r) r = entity_type_expression(b, l + 1);
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
  //     constructor_expression |
  //     case_expression
  static boolean select_expression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "select_expression")) return false;
    boolean r;
    r = reference_expression(b, l + 1);
    if (!r) r = scalar_expression(b, l + 1);
    if (!r) r = expression(b, l + 1, 1);
    if (!r) r = aggregate_expression(b, l + 1);
    if (!r) r = object_expression(b, l + 1);
    if (!r) r = constructor_expression(b, l + 1);
    if (!r) r = case_expression(b, l + 1);
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
  // {<<isHql>> [select_clause] | select_clause}
  //     from_clause
  //     [where_clause]
  //     [groupby_clause]
  //     [having_clause]
  //     [orderby_clause]
  //     [limit_clause]
  //     [offset_clause]
  //     [fetch_clause]
  public static boolean select_statement(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "select_statement")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, SELECT_STATEMENT, "<select statement>");
    r = select_statement_0(b, l + 1);
    r = r && from_clause(b, l + 1);
    r = r && select_statement_2(b, l + 1);
    r = r && select_statement_3(b, l + 1);
    r = r && select_statement_4(b, l + 1);
    r = r && select_statement_5(b, l + 1);
    r = r && select_statement_6(b, l + 1);
    r = r && select_statement_7(b, l + 1);
    r = r && select_statement_8(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // <<isHql>> [select_clause] | select_clause
  private static boolean select_statement_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "select_statement_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = select_statement_0_0(b, l + 1);
    if (!r) r = select_clause(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // <<isHql>> [select_clause]
  private static boolean select_statement_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "select_statement_0_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = isHql(b, l + 1);
    r = r && select_statement_0_0_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // [select_clause]
  private static boolean select_statement_0_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "select_statement_0_0_1")) return false;
    select_clause(b, l + 1);
    return true;
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

  // [fetch_clause]
  private static boolean select_statement_8(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "select_statement_8")) return false;
    fetch_clause(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // <<isUnitTestMode>> QL_statement { [';'] QL_statement }* [';']
  static boolean semicolon_delimited_statements(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "semicolon_delimited_statements")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = isUnitTestMode(b, l + 1);
    r = r && QL_statement(b, l + 1);
    r = r && semicolon_delimited_statements_2(b, l + 1);
    r = r && semicolon_delimited_statements_3(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // { [';'] QL_statement }*
  private static boolean semicolon_delimited_statements_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "semicolon_delimited_statements_2")) return false;
    while (true) {
      int c = current_position_(b);
      if (!semicolon_delimited_statements_2_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "semicolon_delimited_statements_2", c)) break;
    }
    return true;
  }

  // [';'] QL_statement
  private static boolean semicolon_delimited_statements_2_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "semicolon_delimited_statements_2_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = semicolon_delimited_statements_2_0_0(b, l + 1);
    r = r && QL_statement(b, l + 1);
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
  // CASE case_operand simple_when_clause {simple_when_clause}*
  //     ELSE scalar_expression
  //     END
  public static boolean simple_case_expression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "simple_case_expression")) return false;
    if (!nextTokenIs(b, CASE)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, CASE);
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

  /* ********************************************************** */
  // identifier
  //     | input_parameter_expression
  //     | literal
  public static boolean simple_entity_or_value_expression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "simple_entity_or_value_expression")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _COLLAPSE_, SIMPLE_ENTITY_OR_VALUE_EXPRESSION, "<simple entity or value expression>");
    r = identifier(b, l + 1);
    if (!r) r = input_parameter_expression(b, l + 1);
    if (!r) r = literal(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
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
  // reference_expression
  //     | string_literal
  //     | input_parameter_expression
  //     | string_function_expression
  //     | aggregate_expression
  //     | case_expression
  //     | function_invocation_expression
  static boolean string_expression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "string_expression")) return false;
    boolean r;
    r = reference_expression(b, l + 1);
    if (!r) r = string_literal(b, l + 1);
    if (!r) r = input_parameter_expression(b, l + 1);
    if (!r) r = string_function_expression(b, l + 1);
    if (!r) r = aggregate_expression(b, l + 1);
    if (!r) r = case_expression(b, l + 1);
    if (!r) r = function_invocation_expression(b, l + 1);
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
  public static boolean subselect_identification_variable_declaration(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "subselect_identification_variable_declaration")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, SUBSELECT_IDENTIFICATION_VARIABLE_DECLARATION, "<subselect identification variable declaration>");
    r = identification_variable_declaration(b, l + 1);
    if (!r) r = subselect_identification_variable_declaration_1(b, l + 1);
    if (!r) r = derived_collection_member_declaration(b, l + 1);
    exit_section_(b, l, m, r, false, null);
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
  // TYPE'('reference_expression | input_parameter_expression')' | type_literal
  public static boolean type_expression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "type_expression")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, TYPE_EXPRESSION, "<type expression>");
    r = type_expression_0(b, l + 1);
    if (!r) r = type_expression_1(b, l + 1);
    if (!r) r = type_literal(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // TYPE'('reference_expression
  private static boolean type_expression_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "type_expression_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, TYPE, LPAREN);
    r = r && reference_expression(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // input_parameter_expression')'
  private static boolean type_expression_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "type_expression_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = input_parameter_expression(b, l + 1);
    r = r && consumeToken(b, RPAREN);
    exit_section_(b, m, null, r);
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
  // UPDATE identifier [alias_declaration] SET update_item {',' update_item}*
  public static boolean update_clause(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "update_clause")) return false;
    if (!nextTokenIs(b, UPDATE)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, UPDATE);
    r = r && identifier(b, l + 1);
    r = r && update_clause_2(b, l + 1);
    r = r && consumeToken(b, SET);
    r = r && update_item(b, l + 1);
    r = r && update_clause_5(b, l + 1);
    exit_section_(b, m, UPDATE_CLAUSE, r);
    return r;
  }

  // [alias_declaration]
  private static boolean update_clause_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "update_clause_2")) return false;
    alias_declaration(b, l + 1);
    return true;
  }

  // {',' update_item}*
  private static boolean update_clause_5(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "update_clause_5")) return false;
    while (true) {
      int c = current_position_(b);
      if (!update_clause_5_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "update_clause_5", c)) break;
    }
    return true;
  }

  // ',' update_item
  private static boolean update_clause_5_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "update_clause_5_0")) return false;
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
  // WHERE { conditional_group | comparison_expression }
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

  // conditional_group | comparison_expression
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
  //    ATOM(null_comparison_expression) POSTFIX(empty_collection_comparison_expression) BINARY(collection_member_expression) ATOM(exists_expression)
  // 2: BINARY(additive_expression) BINARY(multiplicative_expression) PREFIX(unary_arithmetic_expression) ATOM(functions_returning_numerics_expression)
  //    PREFIX(function_invocation_expression)
  // 3: ATOM(all_or_any_expression)
  // 4: ATOM(reference_expression)
  // 5: ATOM(string_literal) ATOM(boolean_literal) ATOM(datetime_literal) ATOM(numeric_literal)
  // 6: ATOM(input_parameter_expression)
  // 7: ATOM(subquery_expression)
  // 8: ATOM(string_function_expression)
  // 9: ATOM(aggregate_expression)
  // 10: ATOM(case_expression)
  // 11: ATOM(datetime_function_expression)
  // 12: PREFIX(paren_expression)
  public static boolean expression(PsiBuilder b, int l, int g) {
    if (!recursion_guard_(b, l, "expression")) return false;
    addVariant(b, "<expression>");
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, "<expression>");
    r = conditional_not_expression(b, l + 1);
    if (!r) r = in_expression(b, l + 1);
    if (!r) r = null_comparison_expression(b, l + 1);
    if (!r) r = exists_expression(b, l + 1);
    if (!r) r = unary_arithmetic_expression(b, l + 1);
    if (!r) r = functions_returning_numerics_expression(b, l + 1);
    if (!r) r = function_invocation_expression(b, l + 1);
    if (!r) r = all_or_any_expression(b, l + 1);
    if (!r) r = reference_expression(b, l + 1);
    if (!r) r = string_literal(b, l + 1);
    if (!r) r = boolean_literal(b, l + 1);
    if (!r) r = datetime_literal(b, l + 1);
    if (!r) r = numeric_literal(b, l + 1);
    if (!r) r = input_parameter_expression(b, l + 1);
    if (!r) r = subquery_expression(b, l + 1);
    if (!r) r = string_function_expression(b, l + 1);
    if (!r) r = aggregate_expression(b, l + 1);
    if (!r) r = case_expression(b, l + 1);
    if (!r) r = datetime_function_expression(b, l + 1);
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
      }
      else if (g < 0 && consumeTokenSmart(b, AND)) {
        r = expression(b, l, 0);
        exit_section_(b, l, m, CONDITIONAL_AND_EXPRESSION, r, true, null);
      }
      else if (g < 1 && comparison_operator(b, l + 1)) {
        r = expression(b, l, 1);
        exit_section_(b, l, m, COMPARISON_EXPRESSION, r, true, null);
      }
      else if (g < 1 && between_expression_0(b, l + 1)) {
        r = report_error_(b, expression(b, l, 1));
        r = between_expression_1(b, l + 1) && r;
        exit_section_(b, l, m, BETWEEN_EXPRESSION, r, true, null);
      }
      else if (g < 1 && like_expression_0(b, l + 1)) {
        r = true;
        exit_section_(b, l, m, LIKE_EXPRESSION, r, true, null);
      }
      else if (g < 1 && leftMarkerIs(b, REFERENCE_EXPRESSION) && empty_collection_comparison_expression_0(b, l + 1)) {
        r = true;
        exit_section_(b, l, m, EMPTY_COLLECTION_COMPARISON_EXPRESSION, r, true, null);
      }
      else if (g < 1 && leftMarkerIs(b, ENTITY_OR_VALUE_EXPRESSION) && collection_member_expression_0(b, l + 1)) {
        r = expression(b, l, 3);
        exit_section_(b, l, m, COLLECTION_MEMBER_EXPRESSION, r, true, null);
      }
      else if (g < 2 && additive_expression_0(b, l + 1)) {
        r = expression(b, l, 1);
        exit_section_(b, l, m, ADDITIVE_EXPRESSION, r, true, null);
      }
      else if (g < 2 && multiplicative_expression_0(b, l + 1)) {
        r = expression(b, l, 2);
        exit_section_(b, l, m, MULTIPLICATIVE_EXPRESSION, r, true, null);
      }
      else {
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

  // {reference_expression | type_expression} [NOT] IN
  //     { '(' in_item {',' in_item}* ')' | '('subquery')' | input_parameter_expression }
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

  // reference_expression | type_expression
  private static boolean in_expression_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "in_expression_0")) return false;
    boolean r;
    r = reference_expression(b, l + 1);
    if (!r) r = type_expression(b, l + 1);
    return r;
  }

  // [NOT]
  private static boolean in_expression_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "in_expression_1")) return false;
    consumeTokenSmart(b, NOT);
    return true;
  }

  // '(' in_item {',' in_item}* ')' | '('subquery')' | input_parameter_expression
  private static boolean in_expression_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "in_expression_3")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = in_expression_3_0(b, l + 1);
    if (!r) r = in_expression_3_1(b, l + 1);
    if (!r) r = input_parameter_expression(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // '(' in_item {',' in_item}* ')'
  private static boolean in_expression_3_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "in_expression_3_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokenSmart(b, LPAREN);
    r = r && in_item(b, l + 1);
    r = r && in_expression_3_0_2(b, l + 1);
    r = r && consumeToken(b, RPAREN);
    exit_section_(b, m, null, r);
    return r;
  }

  // {',' in_item}*
  private static boolean in_expression_3_0_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "in_expression_3_0_2")) return false;
    while (true) {
      int c = current_position_(b);
      if (!in_expression_3_0_2_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "in_expression_3_0_2", c)) break;
    }
    return true;
  }

  // ',' in_item
  private static boolean in_expression_3_0_2_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "in_expression_3_0_2_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokenSmart(b, COMMA);
    r = r && in_item(b, l + 1);
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

  // [NOT] LIKE {reference_or_parameter | string_literal | string_function_expression } [ESCAPE {string_literal | string_function_expression | input_parameter_expression}]
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

  // reference_or_parameter | string_literal | string_function_expression
  private static boolean like_expression_0_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "like_expression_0_2")) return false;
    boolean r;
    r = reference_or_parameter(b, l + 1);
    if (!r) r = string_literal(b, l + 1);
    if (!r) r = string_function_expression(b, l + 1);
    return r;
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

  // {reference_or_parameter} IS [NOT] NULL
  public static boolean null_comparison_expression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "null_comparison_expression")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _COLLAPSE_, NULL_COMPARISON_EXPRESSION, "<null comparison expression>");
    r = null_comparison_expression_0(b, l + 1);
    r = r && consumeToken(b, IS);
    r = r && null_comparison_expression_2(b, l + 1);
    r = r && consumeToken(b, NULL);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // {reference_or_parameter}
  private static boolean null_comparison_expression_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "null_comparison_expression_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = reference_or_parameter(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // [NOT]
  private static boolean null_comparison_expression_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "null_comparison_expression_2")) return false;
    consumeTokenSmart(b, NOT);
    return true;
  }

  // IS [NOT] EMPTY
  private static boolean empty_collection_comparison_expression_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "empty_collection_comparison_expression_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokenSmart(b, IS);
    r = r && empty_collection_comparison_expression_0_1(b, l + 1);
    r = r && consumeToken(b, EMPTY);
    exit_section_(b, m, null, r);
    return r;
  }

  // [NOT]
  private static boolean empty_collection_comparison_expression_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "empty_collection_comparison_expression_0_1")) return false;
    consumeTokenSmart(b, NOT);
    return true;
  }

  // [NOT] MEMBER [OF]
  private static boolean collection_member_expression_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "collection_member_expression_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = collection_member_expression_0_0(b, l + 1);
    r = r && consumeToken(b, MEMBER);
    r = r && collection_member_expression_0_2(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // [NOT]
  private static boolean collection_member_expression_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "collection_member_expression_0_0")) return false;
    consumeTokenSmart(b, NOT);
    return true;
  }

  // [OF]
  private static boolean collection_member_expression_0_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "collection_member_expression_0_2")) return false;
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
    r = p && expression(b, l, 4);
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

  // { ALL | ANY | SOME} '('subquery')'
  public static boolean all_or_any_expression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "all_or_any_expression")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, ALL_OR_ANY_EXPRESSION, "<all or any expression>");
    r = all_or_any_expression_0(b, l + 1);
    r = r && consumeToken(b, LPAREN);
    r = r && subquery(b, l + 1);
    r = r && consumeToken(b, RPAREN);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // ALL | ANY | SOME
  private static boolean all_or_any_expression_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "all_or_any_expression_0")) return false;
    boolean r;
    r = consumeTokenSmart(b, ALL);
    if (!r) r = consumeTokenSmart(b, ANY);
    if (!r) r = consumeTokenSmart(b, SOME);
    return r;
  }

  // identifier {'.' identifier}* | map_based_reference_expression
  public static boolean reference_expression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "reference_expression")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _COLLAPSE_, REFERENCE_EXPRESSION, "<reference expression>");
    r = reference_expression_0(b, l + 1);
    if (!r) r = map_based_reference_expression(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // identifier {'.' identifier}*
  private static boolean reference_expression_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "reference_expression_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = identifier(b, l + 1);
    r = r && reference_expression_0_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // {'.' identifier}*
  private static boolean reference_expression_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "reference_expression_0_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!reference_expression_0_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "reference_expression_0_1", c)) break;
    }
    return true;
  }

  // '.' identifier
  private static boolean reference_expression_0_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "reference_expression_0_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokenSmart(b, DOT);
    r = r && identifier(b, l + 1);
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

  // ':'identifier
  public static boolean input_parameter_expression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "input_parameter_expression")) return false;
    if (!nextTokenIsSmart(b, COLON)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokenSmart(b, COLON);
    r = r && identifier(b, l + 1);
    exit_section_(b, m, INPUT_PARAMETER_EXPRESSION, r);
    return r;
  }

  // '(' subquery ')'
  public static boolean subquery_expression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "subquery_expression")) return false;
    if (!nextTokenIsSmart(b, LPAREN)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokenSmart(b, LPAREN);
    r = r && subquery(b, l + 1);
    r = r && consumeToken(b, RPAREN);
    exit_section_(b, m, SUBQUERY_EXPRESSION, r);
    return r;
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

  // { AVG | MAX | MIN | SUM | COUNT } '('[DISTINCT] reference_expression')' | function_invocation_expression
  public static boolean aggregate_expression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "aggregate_expression")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _COLLAPSE_, AGGREGATE_EXPRESSION, "<aggregate expression>");
    r = aggregate_expression_0(b, l + 1);
    if (!r) r = function_invocation_expression(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // { AVG | MAX | MIN | SUM | COUNT } '('[DISTINCT] reference_expression')'
  private static boolean aggregate_expression_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "aggregate_expression_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = aggregate_expression_0_0(b, l + 1);
    r = r && consumeToken(b, LPAREN);
    r = r && aggregate_expression_0_2(b, l + 1);
    r = r && reference_expression(b, l + 1);
    r = r && consumeToken(b, RPAREN);
    exit_section_(b, m, null, r);
    return r;
  }

  // AVG | MAX | MIN | SUM | COUNT
  private static boolean aggregate_expression_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "aggregate_expression_0_0")) return false;
    boolean r;
    r = consumeTokenSmart(b, AVG);
    if (!r) r = consumeTokenSmart(b, MAX);
    if (!r) r = consumeTokenSmart(b, MIN);
    if (!r) r = consumeTokenSmart(b, SUM);
    if (!r) r = consumeTokenSmart(b, COUNT);
    return r;
  }

  // [DISTINCT]
  private static boolean aggregate_expression_0_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "aggregate_expression_0_2")) return false;
    consumeTokenSmart(b, DISTINCT);
    return true;
  }

  // general_case_expression
  //     | simple_case_expression
  //     | coalesce_expression
  //     | nullif_expression
  public static boolean case_expression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "case_expression")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _COLLAPSE_, CASE_EXPRESSION, "<case expression>");
    r = general_case_expression(b, l + 1);
    if (!r) r = simple_case_expression(b, l + 1);
    if (!r) r = coalesce_expression(b, l + 1);
    if (!r) r = nullif_expression(b, l + 1);
    exit_section_(b, l, m, r, false, null);
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
