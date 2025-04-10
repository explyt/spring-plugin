// This is a generated file. Not intended for manual editing.
package com.explyt.spring.web.language.http.psi;

import com.intellij.psi.tree.IElementType;
import com.intellij.psi.PsiElement;
import com.intellij.lang.ASTNode;
import generated.psi.impl.*;

public interface HttpTypes {

  IElementType ANY_REQUEST_BLOCK = new HttpElementType("ANY_REQUEST_BLOCK");
  IElementType COMMENT = new HttpElementType("COMMENT");
  IElementType DUMMY_REQUEST_BLOCK = new HttpElementType("DUMMY_REQUEST_BLOCK");
  IElementType FIELD_LINE = new HttpElementType("FIELD_LINE");
  IElementType FIELD_NAME = new HttpElementType("FIELD_NAME");
  IElementType FIELD_VALUE = new HttpElementType("FIELD_VALUE");
  IElementType METHOD = new HttpElementType("METHOD");
  IElementType REQUEST = new HttpElementType("REQUEST");
  IElementType REQUESTS = new HttpElementType("REQUESTS");
  IElementType REQUEST_BLOCK = new HttpElementType("REQUEST_BLOCK");
  IElementType REQUEST_BODY = new HttpElementType("REQUEST_BODY");
  IElementType REQUEST_DEFINER = new HttpElementType("REQUEST_DEFINER");
  IElementType REQUEST_LINE = new HttpElementType("REQUEST_LINE");
  IElementType REQUEST_TARGET = new HttpElementType("REQUEST_TARGET");
  IElementType VARIABLE = new HttpElementType("VARIABLE");

  IElementType BODY_REQUEST_SEPARATOR = new HttpTokenType("BODY_REQUEST_SEPARATOR");
  IElementType COLON = new HttpTokenType(":");
  IElementType COMMENT_LINE = new HttpTokenType("COMMENT_LINE");
  IElementType COMMENT_SEPARATOR = new HttpTokenType("COMMENT_SEPARATOR");
  IElementType CRLF = new HttpTokenType("CRLF");
  IElementType FIELD_CONTENT_TOKEN = new HttpTokenType("FIELD_CONTENT_TOKEN");
  IElementType FULL_REQUEST_LINE = new HttpTokenType("FULL_REQUEST_LINE");
  IElementType GET_OMMITED_REQUEST_LINE = new HttpTokenType("GET_OMMITED_REQUEST_LINE");
  IElementType HTTP_TOKEN = new HttpTokenType("HTTP_TOKEN");
  IElementType HTTP_VERSION = new HttpTokenType("HTTP_VERSION");
  IElementType IDENTIFIER = new HttpTokenType("IDENTIFIER");
  IElementType LBRACES = new HttpTokenType("{{");
  IElementType META_TOKEN = new HttpTokenType("META_TOKEN");
  IElementType OWS = new HttpTokenType("OWS");
  IElementType RBRACES = new HttpTokenType("}}");
  IElementType REQUEST_BODY_VALUE = new HttpTokenType("REQUEST_BODY_VALUE");
  IElementType REQUEST_SEPARATOR = new HttpTokenType("###");
  IElementType REQUEST_TARGET_VALUE = new HttpTokenType("REQUEST_TARGET_VALUE");
  IElementType SP = new HttpTokenType(" ");
  IElementType TAG_COMMENT_LINE_1 = new HttpTokenType("TAG_COMMENT_LINE_1");
  IElementType TAG_COMMENT_LINE_2 = new HttpTokenType("TAG_COMMENT_LINE_2");
  IElementType TAG_TOKEN = new HttpTokenType("TAG_TOKEN");

  class Factory {
    public static PsiElement createElement(ASTNode node) {
      IElementType type = node.getElementType();
      if (type == ANY_REQUEST_BLOCK) {
        return new HttpAnyRequestBlockImpl(node);
      }
      else if (type == COMMENT) {
        return new HttpCommentImpl(node);
      }
      else if (type == DUMMY_REQUEST_BLOCK) {
        return new HttpDummyRequestBlockImpl(node);
      }
      else if (type == FIELD_LINE) {
        return new HttpFieldLineImpl(node);
      }
      else if (type == FIELD_NAME) {
        return new HttpFieldNameImpl(node);
      }
      else if (type == FIELD_VALUE) {
        return new HttpFieldValueImpl(node);
      }
      else if (type == METHOD) {
        return new HttpMethodImpl(node);
      }
      else if (type == REQUEST) {
        return new HttpRequestImpl(node);
      }
      else if (type == REQUESTS) {
        return new HttpRequestsImpl(node);
      }
      else if (type == REQUEST_BLOCK) {
        return new HttpRequestBlockImpl(node);
      }
      else if (type == REQUEST_BODY) {
        return new HttpRequestBodyImpl(node);
      }
      else if (type == REQUEST_DEFINER) {
        return new HttpRequestDefinerImpl(node);
      }
      else if (type == REQUEST_LINE) {
        return new HttpRequestLineImpl(node);
      }
      else if (type == REQUEST_TARGET) {
        return new HttpRequestTargetImpl(node);
      }
      else if (type == VARIABLE) {
        return new HttpVariableImpl(node);
      }
      throw new AssertionError("Unknown element type: " + type);
    }
  }
}
