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
  // ( message_line | CRLF )+
  public static boolean message_body(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "message_body")) return false;
    if (!nextTokenIsFast(b, ANY_TOKEN, CRLF)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, MESSAGE_BODY, "<message body>");
    r = message_body_0(b, l + 1);
    while (r) {
      int c = current_position_(b);
      if (!message_body_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "message_body", c)) break;
    }
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // message_line | CRLF
  private static boolean message_body_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "message_body_0")) return false;
    boolean r;
    r = message_line(b, l + 1);
    if (!r) r = consumeTokenFast(b, CRLF);
    return r;
  }

  /* ********************************************************** */
  // ANY_TOKEN
  public static boolean message_line(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "message_line")) return false;
    if (!nextTokenIsFast(b, ANY_TOKEN)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokenFast(b, ANY_TOKEN);
    exit_section_(b, m, MESSAGE_LINE, r);
    return r;
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
  //             [ CRLF message_body ] ]
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
  //             [ CRLF message_body ] ]
  private static boolean request_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "request_1")) return false;
    request_1_0(b, l + 1);
    return true;
  }

  // CRLF
  //             ( field_line [ CRLF ] )*
  //             [ CRLF message_body ]
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

  // [ CRLF message_body ]
  private static boolean request_1_0_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "request_1_0_2")) return false;
    request_1_0_2_0(b, l + 1);
    return true;
  }

  // CRLF message_body
  private static boolean request_1_0_2_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "request_1_0_2_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokenFast(b, CRLF);
    r = r && message_body(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // ( request_definer CRLF )?
  //                   ( comment CRLF )*
  //                   request
  public static boolean request_block(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "request_block")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, REQUEST_BLOCK, "<request block>");
    r = request_block_0(b, l + 1);
    r = r && request_block_1(b, l + 1);
    r = r && request(b, l + 1);
    exit_section_(b, l, m, r, false, HttpParser::recover_request);
    return r;
  }

  // ( request_definer CRLF )?
  private static boolean request_block_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "request_block_0")) return false;
    request_block_0_0(b, l + 1);
    return true;
  }

  // request_definer CRLF
  private static boolean request_block_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "request_block_0_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = request_definer(b, l + 1);
    r = r && consumeToken(b, CRLF);
    exit_section_(b, m, null, r);
    return r;
  }

  // ( comment CRLF )*
  private static boolean request_block_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "request_block_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!request_block_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "request_block_1", c)) break;
    }
    return true;
  }

  // comment CRLF
  private static boolean request_block_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "request_block_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = comment(b, l + 1);
    r = r && consumeToken(b, CRLF);
    exit_section_(b, m, null, r);
    return r;
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
  // ( request_block | request_definer | comment | CRLF )*
  public static boolean requests(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "requests")) return false;
    Marker m = enter_section_(b, l, _NONE_, REQUESTS, "<requests>");
    while (true) {
      int c = current_position_(b);
      if (!requests_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "requests", c)) break;
    }
    exit_section_(b, l, m, true, false, null);
    return true;
  }

  // request_block | request_definer | comment | CRLF
  private static boolean requests_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "requests_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = request_block(b, l + 1);
    if (!r) r = request_definer(b, l + 1);
    if (!r) r = comment(b, l + 1);
    if (!r) r = consumeTokenFast(b, CRLF);
    exit_section_(b, m, null, r);
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
