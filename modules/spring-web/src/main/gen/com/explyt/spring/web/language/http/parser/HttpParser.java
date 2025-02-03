// This is a generated file. Not intended for manual editing.
package com.explyt.spring.web.language.http.parser;

import com.intellij.lang.ASTNode;
import com.intellij.lang.LightPsiParser;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilder.Marker;
import com.intellij.lang.PsiParser;
import com.intellij.psi.tree.IElementType;

import static com.explyt.spring.web.language.http.psi.HttpTypes.*;
import static com.intellij.lang.parser.GeneratedParserUtilBase.*;

@SuppressWarnings({"SimplifiableIfStatement", "UnusedAssignment"})
public class HttpParser implements PsiParser, LightPsiParser {

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
    return http_file(b, l + 1);
  }

  /* ********************************************************** */
  // requests
  static boolean http_file(PsiBuilder b, int l) {
    return requests(b, l + 1);
  }

  /* ********************************************************** */
  // GET | POST | PUT | DELETE | PATCH | HEAD | OPTIONS
  public static boolean method(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "method")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, METHOD, "<method>");
    r = consumeTokenFast(b, GET);
    if (!r) r = consumeTokenFast(b, POST);
    if (!r) r = consumeTokenFast(b, PUT);
    if (!r) r = consumeTokenFast(b, DELETE);
    if (!r) r = consumeTokenFast(b, PATCH);
    if (!r) r = consumeTokenFast(b, HEAD);
    if (!r) r = consumeTokenFast(b, OPTIONS);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // !(REQUEST_SEPARATOR)
  static boolean recover_request(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "recover_request")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_);
    r = !recover_request_0(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // (REQUEST_SEPARATOR)
  private static boolean recover_request_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "recover_request_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokenFast(b, REQUEST_SEPARATOR);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // method? url
  public static boolean request(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "request")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, REQUEST, "<request>");
    r = request_0(b, l + 1);
    r = r && url(b, l + 1);
    exit_section_(b, l, m, r, false, HttpParser::recover_request);
    return r;
  }

  // method?
  private static boolean request_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "request_0")) return false;
    method(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // request_without_separator | request_with_separator
  public static boolean request_block(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "request_block")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, REQUEST_BLOCK, "<request block>");
    r = request_without_separator(b, l + 1);
    if (!r) r = request_with_separator(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // REQUEST_SEPARATOR+ request
  static boolean request_with_separator(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "request_with_separator")) return false;
    if (!nextTokenIsFast(b, REQUEST_SEPARATOR)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = request_with_separator_0(b, l + 1);
    r = r && request(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // REQUEST_SEPARATOR+
  private static boolean request_with_separator_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "request_with_separator_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokenFast(b, REQUEST_SEPARATOR);
    while (r) {
      int c = current_position_(b);
      if (!consumeTokenFast(b, REQUEST_SEPARATOR)) break;
      if (!empty_element_parsed_guard_(b, "request_with_separator_0", c)) break;
    }
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // REQUEST_SEPARATOR* request
  static boolean request_without_separator(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "request_without_separator")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_);
    r = request_without_separator_0(b, l + 1);
    r = r && request(b, l + 1);
    exit_section_(b, l, m, r, false, HttpParser::recover_request);
    return r;
  }

  // REQUEST_SEPARATOR*
  private static boolean request_without_separator_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "request_without_separator_0")) return false;
    while (true) {
      int c = current_position_(b);
      if (!consumeTokenFast(b, REQUEST_SEPARATOR)) break;
      if (!empty_element_parsed_guard_(b, "request_without_separator_0", c)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // request_block*
  public static boolean requests(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "requests")) return false;
    Marker m = enter_section_(b, l, _NONE_, REQUESTS, "<requests>");
    while (true) {
      int c = current_position_(b);
      if (!request_block(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "requests", c)) break;
    }
    exit_section_(b, l, m, true, false, null);
    return true;
  }

  /* ********************************************************** */
  // HTTP | HTTPS | variable
  public static boolean url(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "url")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, URL, "<url>");
    r = consumeTokenFast(b, HTTP);
    if (!r) r = consumeTokenFast(b, HTTPS);
    if (!r) r = variable(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // LBRACES IDENTIFIER RBRACES
  public static boolean variable(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "variable")) return false;
    if (!nextTokenIsFast(b, LBRACES)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, LBRACES, IDENTIFIER, RBRACES);
    exit_section_(b, m, VARIABLE, r);
    return r;
  }

}
