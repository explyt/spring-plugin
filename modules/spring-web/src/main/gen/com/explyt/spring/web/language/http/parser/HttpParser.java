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
  // ( request_block | dummy_request_block )*
  public static boolean any_request_block(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "any_request_block")) return false;
    Marker m = enter_section_(b, l, _NONE_, ANY_REQUEST_BLOCK, "<any request block>");
    while (true) {
      int c = current_position_(b);
      if (!any_request_block_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "any_request_block", c)) break;
    }
    exit_section_(b, l, m, true, false, HttpParser::recover_request);
    return true;
  }

  // request_block | dummy_request_block
  private static boolean any_request_block_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "any_request_block_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = request_block(b, l + 1);
    if (!r) r = dummy_request_block(b, l + 1);
    exit_section_(b, m, null, r);
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
  // CRLF* request_definer? CRLF*
  //                         ( comment CRLF* )*
  public static boolean dummy_request_block(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "dummy_request_block")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, DUMMY_REQUEST_BLOCK, "<dummy request block>");
    r = dummy_request_block_0(b, l + 1);
    r = r && dummy_request_block_1(b, l + 1);
    p = r; // pin = 2
    r = r && report_error_(b, dummy_request_block_2(b, l + 1));
    r = p && dummy_request_block_3(b, l + 1) && r;
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

  // request_definer?
  private static boolean dummy_request_block_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "dummy_request_block_1")) return false;
    request_definer(b, l + 1);
    return true;
  }

  // CRLF*
  private static boolean dummy_request_block_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "dummy_request_block_2")) return false;
    while (true) {
      int c = current_position_(b);
      if (!consumeTokenFast(b, CRLF)) break;
      if (!empty_element_parsed_guard_(b, "dummy_request_block_2", c)) break;
    }
    return true;
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
  // HTTP_TOKEN
  public static boolean field_name(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "field_name")) return false;
    if (!nextTokenIsFast(b, HTTP_TOKEN)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokenFast(b, HTTP_TOKEN);
    exit_section_(b, m, FIELD_NAME, r);
    return r;
  }

  /* ********************************************************** */
  // FIELD_CONTENT
  public static boolean field_value(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "field_value")) return false;
    if (!nextTokenIsFast(b, FIELD_CONTENT)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokenFast(b, FIELD_CONTENT);
    exit_section_(b, m, FIELD_VALUE, r);
    return r;
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
  // !(CRLF)
  static boolean recover_line(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "recover_line")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_);
    r = !recover_line_0(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // (CRLF)
  private static boolean recover_line_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "recover_line_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokenFast(b, CRLF);
    exit_section_(b, m, null, r);
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
  // request_line [ CRLF
  //             ( field_line [ CRLF ] )*
  //             [ CRLF REQUEST_BODY ] ]
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

  // [ CRLF
  //             ( field_line [ CRLF ] )*
  //             [ CRLF REQUEST_BODY ] ]
  private static boolean request_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "request_1")) return false;
    request_1_0(b, l + 1);
    return true;
  }

  // CRLF
  //             ( field_line [ CRLF ] )*
  //             [ CRLF REQUEST_BODY ]
  private static boolean request_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "request_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokenFast(b, CRLF);
    r = r && request_1_0_1(b, l + 1);
    r = r && request_1_0_2(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // ( field_line [ CRLF ] )*
  private static boolean request_1_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "request_1_0_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!request_1_0_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "request_1_0_1", c)) break;
    }
    return true;
  }

  // field_line [ CRLF ]
  private static boolean request_1_0_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "request_1_0_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = field_line(b, l + 1);
    r = r && request_1_0_1_0_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // [ CRLF ]
  private static boolean request_1_0_1_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "request_1_0_1_0_1")) return false;
    consumeTokenFast(b, CRLF);
    return true;
  }

  // [ CRLF REQUEST_BODY ]
  private static boolean request_1_0_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "request_1_0_2")) return false;
    request_1_0_2_0(b, l + 1);
    return true;
  }

  // CRLF REQUEST_BODY
  private static boolean request_1_0_2_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "request_1_0_2_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, CRLF, REQUEST_BODY);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // CRLF* request_definer? CRLF*
  //                   ( comment CRLF* )*
  //                   request CRLF*
  public static boolean request_block(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "request_block")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, REQUEST_BLOCK, "<request block>");
    r = request_block_0(b, l + 1);
    r = r && request_block_1(b, l + 1);
    r = r && request_block_2(b, l + 1);
    r = r && request_block_3(b, l + 1);
    r = r && request(b, l + 1);
    p = r; // pin = 5
    r = r && request_block_5(b, l + 1);
    exit_section_(b, l, m, r, p, null);
    return r || p;
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

  // CRLF*
  private static boolean request_block_5(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "request_block_5")) return false;
    while (true) {
      int c = current_position_(b);
      if (!consumeTokenFast(b, CRLF)) break;
      if (!empty_element_parsed_guard_(b, "request_block_5", c)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // REQUEST_SEPARATOR [ ANY_TOKEN ]
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

  // [ ANY_TOKEN ]
  private static boolean request_definer_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "request_definer_1")) return false;
    consumeTokenFast(b, ANY_TOKEN);
    return true;
  }

  /* ********************************************************** */
  // method SP REQUEST_TARGET [ SP HTTP_VERSION ]
  public static boolean request_line(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "request_line")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, REQUEST_LINE, "<request line>");
    r = method(b, l + 1);
    p = r; // pin = 1
    r = r && report_error_(b, consumeTokens(b, -1, SP, REQUEST_TARGET));
    r = p && request_line_3(b, l + 1) && r;
    exit_section_(b, l, m, r, p, HttpParser::recover_line);
    return r || p;
  }

  // [ SP HTTP_VERSION ]
  private static boolean request_line_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "request_line_3")) return false;
    request_line_3_0(b, l + 1);
    return true;
  }

  // SP HTTP_VERSION
  private static boolean request_line_3_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "request_line_3_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, SP, HTTP_VERSION);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // any_request_block
  public static boolean requests(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "requests")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, REQUESTS, "<requests>");
    r = any_request_block(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // COMMENT_SEPARATOR [ WHITE_SPACE ] TAG_TOKEN
  public static boolean tag_comment_line(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "tag_comment_line")) return false;
    if (!nextTokenIsFast(b, COMMENT_SEPARATOR)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokenFast(b, COMMENT_SEPARATOR);
    r = r && tag_comment_line_1(b, l + 1);
    r = r && consumeToken(b, TAG_TOKEN);
    exit_section_(b, m, TAG_COMMENT_LINE, r);
    return r;
  }

  // [ WHITE_SPACE ]
  private static boolean tag_comment_line_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "tag_comment_line_1")) return false;
    consumeTokenFast(b, WHITE_SPACE);
    return true;
  }

}
