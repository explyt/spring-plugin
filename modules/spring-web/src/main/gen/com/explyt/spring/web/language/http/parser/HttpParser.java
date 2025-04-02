// This is a generated file. Not intended for manual editing.
package com.explyt.spring.web.language.http.parser;

import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilder.Marker;
import static com.explyt.spring.web.language.http.psi.HttpTypes.*;
import static com.intellij.lang.parser.GeneratedParserUtilBase.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.lang.ASTNode;
import com.intellij.psi.tree.TokenSet;
import com.intellij.lang.PsiParser;
import com.intellij.lang.LightPsiParser;

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
  // request_block | dummy_request_block
  public static boolean any_request_block(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "any_request_block")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, ANY_REQUEST_BLOCK, "<any request block>");
    r = request_block(b, l + 1);
    if (!r) r = dummy_request_block(b, l + 1);
    exit_section_(b, l, m, r, false, HttpParser::recover_request);
    return r;
  }

  /* ********************************************************** */
  // COMMENT_LINE | tag_comment_line
  public static boolean comment(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "comment")) return false;
    if (!nextTokenIsFast(b, COMMENT_LINE, COMMENT_SEPARATOR)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, COMMENT, "<comment>");
    r = consumeTokenFast(b, COMMENT_LINE);
    if (!r) r = tag_comment_line(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // CRLF* ( request_definer | comment )? CRLF+
  //                         ( comment CRLF* )*
  public static boolean dummy_request_block(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "dummy_request_block")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, DUMMY_REQUEST_BLOCK, "<dummy request block>");
    r = dummy_request_block_0(b, l + 1);
    r = r && dummy_request_block_1(b, l + 1);
    r = r && dummy_request_block_2(b, l + 1);
    p = r; // pin = 3
    r = r && dummy_request_block_3(b, l + 1);
    exit_section_(b, l, m, r, p, HttpParser::recover_request);
    return r || p;
  }

  // CRLF*
  private static boolean dummy_request_block_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "dummy_request_block_0")) return false;
    while (true) {
      int c = current_position_(b);
      if (!consumeTokenFast(b, CRLF)) break;
      if (!empty_element_parsed_guard_(b, "dummy_request_block_0", c)) break;
    }
    return true;
  }

  // ( request_definer | comment )?
  private static boolean dummy_request_block_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "dummy_request_block_1")) return false;
    dummy_request_block_1_0(b, l + 1);
    return true;
  }

  // request_definer | comment
  private static boolean dummy_request_block_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "dummy_request_block_1_0")) return false;
    boolean r;
    r = request_definer(b, l + 1);
    if (!r) r = comment(b, l + 1);
    return r;
  }

  // CRLF+
  private static boolean dummy_request_block_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "dummy_request_block_2")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokenFast(b, CRLF);
    while (r) {
      int c = current_position_(b);
      if (!consumeTokenFast(b, CRLF)) break;
      if (!empty_element_parsed_guard_(b, "dummy_request_block_2", c)) break;
    }
    exit_section_(b, m, null, r);
    return r;
  }

  // ( comment CRLF* )*
  private static boolean dummy_request_block_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "dummy_request_block_3")) return false;
    while (true) {
      int c = current_position_(b);
      if (!dummy_request_block_3_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "dummy_request_block_3", c)) break;
    }
    return true;
  }

  // comment CRLF*
  private static boolean dummy_request_block_3_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "dummy_request_block_3_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = comment(b, l + 1);
    r = r && dummy_request_block_3_0_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // CRLF*
  private static boolean dummy_request_block_3_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "dummy_request_block_3_0_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!consumeTokenFast(b, CRLF)) break;
      if (!empty_element_parsed_guard_(b, "dummy_request_block_3_0_1", c)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // [ COMMENT_LINE ] CRLF
  static boolean expanded_crlf(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expanded_crlf")) return false;
    if (!nextTokenIsFast(b, COMMENT_LINE, CRLF)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = expanded_crlf_0(b, l + 1);
    r = r && consumeToken(b, CRLF);
    exit_section_(b, m, null, r);
    return r;
  }

  // [ COMMENT_LINE ]
  private static boolean expanded_crlf_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expanded_crlf_0")) return false;
    consumeTokenFast(b, COMMENT_LINE);
    return true;
  }

  /* ********************************************************** */
  // FIELD_CONTENT_TOKEN | variable
  static boolean field_content(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "field_content")) return false;
    if (!nextTokenIsFast(b, FIELD_CONTENT_TOKEN, LBRACES)) return false;
    boolean r;
    r = consumeTokenFast(b, FIELD_CONTENT_TOKEN);
    if (!r) r = variable(b, l + 1);
    return r;
  }

  /* ********************************************************** */
  // field_name ":" [OWS] field_value [OWS]
  public static boolean field_line(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "field_line")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, FIELD_LINE, "<field line>");
    r = field_name(b, l + 1);
    r = r && consumeToken(b, COLON);
    p = r; // pin = 2
    r = r && report_error_(b, field_line_2(b, l + 1));
    r = p && report_error_(b, field_value(b, l + 1)) && r;
    r = p && field_line_4(b, l + 1) && r;
    exit_section_(b, l, m, r, p, HttpParser::recover_line);
    return r || p;
  }

  // [OWS]
  private static boolean field_line_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "field_line_2")) return false;
    consumeTokenFast(b, OWS);
    return true;
  }

  // [OWS]
  private static boolean field_line_4(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "field_line_4")) return false;
    consumeTokenFast(b, OWS);
    return true;
  }

  /* ********************************************************** */
  // (HTTP_TOKEN | variable )+
  public static boolean field_name(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "field_name")) return false;
    if (!nextTokenIsFast(b, HTTP_TOKEN, LBRACES)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, FIELD_NAME, "<field name>");
    r = field_name_0(b, l + 1);
    while (r) {
      int c = current_position_(b);
      if (!field_name_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "field_name", c)) break;
    }
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // HTTP_TOKEN | variable
  private static boolean field_name_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "field_name_0")) return false;
    boolean r;
    r = consumeTokenFast(b, HTTP_TOKEN);
    if (!r) r = variable(b, l + 1);
    return r;
  }

  /* ********************************************************** */
  // field_content ( [ OWS ] field_content )*
  public static boolean field_value(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "field_value")) return false;
    if (!nextTokenIsFast(b, FIELD_CONTENT_TOKEN, LBRACES)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, FIELD_VALUE, "<field value>");
    r = field_content(b, l + 1);
    r = r && field_value_1(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // ( [ OWS ] field_content )*
  private static boolean field_value_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "field_value_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!field_value_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "field_value_1", c)) break;
    }
    return true;
  }

  // [ OWS ] field_content
  private static boolean field_value_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "field_value_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = field_value_1_0_0(b, l + 1);
    r = r && field_content(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // [ OWS ]
  private static boolean field_value_1_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "field_value_1_0_0")) return false;
    consumeTokenFast(b, OWS);
    return true;
  }

  /* ********************************************************** */
  // requests
  static boolean http_file(PsiBuilder b, int l) {
    return requests(b, l + 1);
  }

  /* ********************************************************** */
  // HTTP_TOKEN
  public static boolean method(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "method")) return false;
    if (!nextTokenIsFast(b, HTTP_TOKEN)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokenFast(b, HTTP_TOKEN);
    exit_section_(b, m, METHOD, r);
    return r;
  }

  /* ********************************************************** */
  // !(COMMENT_LINE | CRLF)
  static boolean recover_line(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "recover_line")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_);
    r = !recover_line_0(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // COMMENT_LINE | CRLF
  private static boolean recover_line_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "recover_line_0")) return false;
    boolean r;
    r = consumeTokenFast(b, COMMENT_LINE);
    if (!r) r = consumeTokenFast(b, CRLF);
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
  // !(RBRACES | CRLF)
  static boolean recover_variable(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "recover_variable")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_);
    r = !recover_variable_0(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // RBRACES | CRLF
  private static boolean recover_variable_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "recover_variable_0")) return false;
    boolean r;
    r = consumeTokenFast(b, RBRACES);
    if (!r) r = consumeTokenFast(b, CRLF);
    return r;
  }

  /* ********************************************************** */
  // LBRACES IDENTIFIER
  static boolean recoverable_variable(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "recoverable_variable")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_);
    r = consumeTokens(b, 1, LBRACES, IDENTIFIER);
    p = r; // pin = 1
    exit_section_(b, l, m, r, p, HttpParser::recover_variable);
    return r || p;
  }

  /* ********************************************************** */
  // request_line [ expanded_crlf
  //             ( field_line [ expanded_crlf ] )*
  //             [ expanded_crlf [ request_body ] ] ]
  public static boolean request(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "request")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, REQUEST, "<request>");
    r = request_line(b, l + 1);
    p = r; // pin = 1
    r = r && request_1(b, l + 1);
    exit_section_(b, l, m, r, p, HttpParser::recover_request);
    return r || p;
  }

  // [ expanded_crlf
  //             ( field_line [ expanded_crlf ] )*
  //             [ expanded_crlf [ request_body ] ] ]
  private static boolean request_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "request_1")) return false;
    request_1_0(b, l + 1);
    return true;
  }

  // expanded_crlf
  //             ( field_line [ expanded_crlf ] )*
  //             [ expanded_crlf [ request_body ] ]
  private static boolean request_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "request_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = expanded_crlf(b, l + 1);
    r = r && request_1_0_1(b, l + 1);
    r = r && request_1_0_2(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // ( field_line [ expanded_crlf ] )*
  private static boolean request_1_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "request_1_0_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!request_1_0_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "request_1_0_1", c)) break;
    }
    return true;
  }

  // field_line [ expanded_crlf ]
  private static boolean request_1_0_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "request_1_0_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = field_line(b, l + 1);
    r = r && request_1_0_1_0_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // [ expanded_crlf ]
  private static boolean request_1_0_1_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "request_1_0_1_0_1")) return false;
    expanded_crlf(b, l + 1);
    return true;
  }

  // [ expanded_crlf [ request_body ] ]
  private static boolean request_1_0_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "request_1_0_2")) return false;
    request_1_0_2_0(b, l + 1);
    return true;
  }

  // expanded_crlf [ request_body ]
  private static boolean request_1_0_2_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "request_1_0_2_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = expanded_crlf(b, l + 1);
    r = r && request_1_0_2_0_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // [ request_body ]
  private static boolean request_1_0_2_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "request_1_0_2_0_1")) return false;
    request_body(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // CRLF* request_definer? CRLF*
  //                   ( comment CRLF* )*
  //                   request
  public static boolean request_block(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "request_block")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, REQUEST_BLOCK, "<request block>");
    r = request_block_0(b, l + 1);
    r = r && request_block_1(b, l + 1);
    r = r && request_block_2(b, l + 1);
    r = r && request_block_3(b, l + 1);
    r = r && request(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // CRLF*
  private static boolean request_block_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "request_block_0")) return false;
    while (true) {
      int c = current_position_(b);
      if (!consumeTokenFast(b, CRLF)) break;
      if (!empty_element_parsed_guard_(b, "request_block_0", c)) break;
    }
    return true;
  }

  // request_definer?
  private static boolean request_block_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "request_block_1")) return false;
    request_definer(b, l + 1);
    return true;
  }

  // CRLF*
  private static boolean request_block_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "request_block_2")) return false;
    while (true) {
      int c = current_position_(b);
      if (!consumeTokenFast(b, CRLF)) break;
      if (!empty_element_parsed_guard_(b, "request_block_2", c)) break;
    }
    return true;
  }

  // ( comment CRLF* )*
  private static boolean request_block_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "request_block_3")) return false;
    while (true) {
      int c = current_position_(b);
      if (!request_block_3_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "request_block_3", c)) break;
    }
    return true;
  }

  // comment CRLF*
  private static boolean request_block_3_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "request_block_3_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = comment(b, l + 1);
    r = r && request_block_3_0_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // CRLF*
  private static boolean request_block_3_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "request_block_3_0_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!consumeTokenFast(b, CRLF)) break;
      if (!empty_element_parsed_guard_(b, "request_block_3_0_1", c)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // ( REQUEST_BODY_VALUE | COMMENT_LINE | variable )+
  public static boolean request_body(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "request_body")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, REQUEST_BODY, "<request body>");
    r = request_body_0(b, l + 1);
    while (r) {
      int c = current_position_(b);
      if (!request_body_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "request_body", c)) break;
    }
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // REQUEST_BODY_VALUE | COMMENT_LINE | variable
  private static boolean request_body_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "request_body_0")) return false;
    boolean r;
    r = consumeTokenFast(b, REQUEST_BODY_VALUE);
    if (!r) r = consumeTokenFast(b, COMMENT_LINE);
    if (!r) r = variable(b, l + 1);
    return r;
  }

  /* ********************************************************** */
  // REQUEST_SEPARATOR ( META_TOKEN | variable )*
  public static boolean request_definer(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "request_definer")) return false;
    if (!nextTokenIsFast(b, REQUEST_SEPARATOR)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokenFast(b, REQUEST_SEPARATOR);
    r = r && request_definer_1(b, l + 1);
    exit_section_(b, m, REQUEST_DEFINER, r);
    return r;
  }

  // ( META_TOKEN | variable )*
  private static boolean request_definer_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "request_definer_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!request_definer_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "request_definer_1", c)) break;
    }
    return true;
  }

  // META_TOKEN | variable
  private static boolean request_definer_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "request_definer_1_0")) return false;
    boolean r;
    r = consumeTokenFast(b, META_TOKEN);
    if (!r) r = variable(b, l + 1);
    return r;
  }

  /* ********************************************************** */
  // [ method SP ] request_target [ SP HTTP_VERSION ]
  public static boolean request_line(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "request_line")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, REQUEST_LINE, "<request line>");
    r = request_line_0(b, l + 1);
    r = r && request_target(b, l + 1);
    p = r; // pin = 2
    r = r && request_line_2(b, l + 1);
    exit_section_(b, l, m, r, p, HttpParser::recover_line);
    return r || p;
  }

  // [ method SP ]
  private static boolean request_line_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "request_line_0")) return false;
    request_line_0_0(b, l + 1);
    return true;
  }

  // method SP
  private static boolean request_line_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "request_line_0_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = method(b, l + 1);
    r = r && consumeToken(b, SP);
    exit_section_(b, m, null, r);
    return r;
  }

  // [ SP HTTP_VERSION ]
  private static boolean request_line_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "request_line_2")) return false;
    request_line_2_0(b, l + 1);
    return true;
  }

  // SP HTTP_VERSION
  private static boolean request_line_2_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "request_line_2_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, SP, HTTP_VERSION);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // ( REQUEST_TARGET_VALUE | variable )+
  public static boolean request_target(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "request_target")) return false;
    if (!nextTokenIsFast(b, LBRACES, REQUEST_TARGET_VALUE)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, REQUEST_TARGET, "<request target>");
    r = request_target_0(b, l + 1);
    while (r) {
      int c = current_position_(b);
      if (!request_target_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "request_target", c)) break;
    }
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // REQUEST_TARGET_VALUE | variable
  private static boolean request_target_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "request_target_0")) return false;
    boolean r;
    r = consumeTokenFast(b, REQUEST_TARGET_VALUE);
    if (!r) r = variable(b, l + 1);
    return r;
  }

  /* ********************************************************** */
  // any_request_block*
  public static boolean requests(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "requests")) return false;
    Marker m = enter_section_(b, l, _NONE_, REQUESTS, "<requests>");
    while (true) {
      int c = current_position_(b);
      if (!any_request_block(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "requests", c)) break;
    }
    exit_section_(b, l, m, true, false, null);
    return true;
  }

  /* ********************************************************** */
  // COMMENT_SEPARATOR TAG_TOKEN ( META_TOKEN | variable )*
  static boolean tag_comment_line(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "tag_comment_line")) return false;
    if (!nextTokenIsFast(b, COMMENT_SEPARATOR)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, COMMENT_SEPARATOR, TAG_TOKEN);
    r = r && tag_comment_line_2(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // ( META_TOKEN | variable )*
  private static boolean tag_comment_line_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "tag_comment_line_2")) return false;
    while (true) {
      int c = current_position_(b);
      if (!tag_comment_line_2_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "tag_comment_line_2", c)) break;
    }
    return true;
  }

  // META_TOKEN | variable
  private static boolean tag_comment_line_2_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "tag_comment_line_2_0")) return false;
    boolean r;
    r = consumeTokenFast(b, META_TOKEN);
    if (!r) r = variable(b, l + 1);
    return r;
  }

  /* ********************************************************** */
  // recoverable_variable RBRACES
  public static boolean variable(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "variable")) return false;
    if (!nextTokenIsFast(b, LBRACES)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = recoverable_variable(b, l + 1);
    r = r && consumeToken(b, RBRACES);
    exit_section_(b, m, VARIABLE, r);
    return r;
  }

}
