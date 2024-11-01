// This is a generated file. Not intended for manual editing.
package com.explyt.spring.core.language.profiles.parser;

import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilder.Marker;

import static com.explyt.spring.core.language.profiles.psi.ProfilesTypes.*;
import static com.intellij.lang.parser.GeneratedParserUtilBase.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.lang.ASTNode;
import com.intellij.psi.tree.TokenSet;
import com.intellij.lang.PsiParser;
import com.intellij.lang.LightPsiParser;

@SuppressWarnings({"SimplifiableIfStatement", "UnusedAssignment"})
public class ProfilesParser implements PsiParser, LightPsiParser {

  public ASTNode parse(IElementType t, PsiBuilder b) {
    parseLight(t, b);
    return b.getTreeBuilt();
  }

  public void parseLight(IElementType t, PsiBuilder b) {
    boolean r;
    b = adapt_builder_(t, b, this, null);
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

  /* ********************************************************** */
  // (profile|not_expression|nested_expr_) (AND (profile|not_expression|nested_expr_))+
  public static boolean and_expression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "and_expression")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, AND_EXPRESSION, "<and expression>");
    r = and_expression_0(b, l + 1);
    r = r && and_expression_1(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // profile|not_expression|nested_expr_
  private static boolean and_expression_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "and_expression_0")) return false;
    boolean r;
    r = profile(b, l + 1);
    if (!r) r = not_expression(b, l + 1);
    if (!r) r = nested_expr_(b, l + 1);
    return r;
  }

  // (AND (profile|not_expression|nested_expr_))+
  private static boolean and_expression_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "and_expression_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = and_expression_1_0(b, l + 1);
    while (r) {
      int c = current_position_(b);
      if (!and_expression_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "and_expression_1", c)) break;
    }
    exit_section_(b, m, null, r);
    return r;
  }

  // AND (profile|not_expression|nested_expr_)
  private static boolean and_expression_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "and_expression_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, AND);
    r = r && and_expression_1_0_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // profile|not_expression|nested_expr_
  private static boolean and_expression_1_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "and_expression_1_0_1")) return false;
    boolean r;
    r = profile(b, l + 1);
    if (!r) r = not_expression(b, l + 1);
    if (!r) r = nested_expr_(b, l + 1);
    return r;
  }

  /* ********************************************************** */
  // profile_expr_ | nested_expr_
  static boolean file(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "file")) return false;
    boolean r;
    r = profile_expr_(b, l + 1);
    if (!r) r = nested_expr_(b, l + 1);
    return r;
  }

  /* ********************************************************** */
  // LPAREN profile_expr_ RPAREN
  static boolean nested_expr_(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "nested_expr_")) return false;
    if (!nextTokenIs(b, LPAREN)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, LPAREN);
    r = r && profile_expr_(b, l + 1);
    r = r && consumeToken(b, RPAREN);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // NOT (profile|nested_expr_)
  public static boolean not_expression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "not_expression")) return false;
    if (!nextTokenIs(b, NOT)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, NOT);
    r = r && not_expression_1(b, l + 1);
    exit_section_(b, m, NOT_EXPRESSION, r);
    return r;
  }

  // profile|nested_expr_
  private static boolean not_expression_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "not_expression_1")) return false;
    boolean r;
    r = profile(b, l + 1);
    if (!r) r = nested_expr_(b, l + 1);
    return r;
  }

  /* ********************************************************** */
  // (profile|not_expression|nested_expr_) (OR (profile|not_expression|nested_expr_))+
  public static boolean or_expression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "or_expression")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, OR_EXPRESSION, "<or expression>");
    r = or_expression_0(b, l + 1);
    r = r && or_expression_1(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // profile|not_expression|nested_expr_
  private static boolean or_expression_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "or_expression_0")) return false;
    boolean r;
    r = profile(b, l + 1);
    if (!r) r = not_expression(b, l + 1);
    if (!r) r = nested_expr_(b, l + 1);
    return r;
  }

  // (OR (profile|not_expression|nested_expr_))+
  private static boolean or_expression_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "or_expression_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = or_expression_1_0(b, l + 1);
    while (r) {
      int c = current_position_(b);
      if (!or_expression_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "or_expression_1", c)) break;
    }
    exit_section_(b, m, null, r);
    return r;
  }

  // OR (profile|not_expression|nested_expr_)
  private static boolean or_expression_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "or_expression_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, OR);
    r = r && or_expression_1_0_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // profile|not_expression|nested_expr_
  private static boolean or_expression_1_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "or_expression_1_0_1")) return false;
    boolean r;
    r = profile(b, l + 1);
    if (!r) r = not_expression(b, l + 1);
    if (!r) r = nested_expr_(b, l + 1);
    return r;
  }

  /* ********************************************************** */
  // value
  public static boolean profile(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "profile")) return false;
    if (!nextTokenIs(b, VALUE)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, VALUE);
    exit_section_(b, m, PROFILE, r);
    return r;
  }

  /* ********************************************************** */
  // and_expression
  //     | or_expression
  //     | not_expression
  //     | profile
  static boolean profile_expr_(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "profile_expr_")) return false;
    boolean r;
    r = and_expression(b, l + 1);
    if (!r) r = or_expression(b, l + 1);
    if (!r) r = not_expression(b, l + 1);
    if (!r) r = profile(b, l + 1);
    return r;
  }

}
