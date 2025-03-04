// This is a generated file. Not intended for manual editing.
package com.explyt.spring.web.language.http.psi;

import com.intellij.psi.tree.IElementType;
import com.intellij.psi.PsiElement;
import com.intellij.lang.ASTNode;
import generated.psi.impl.*;

public interface HttpTypes {

  IElementType COMMENT = new HttpElementType("COMMENT");
  IElementType FIELD_LINE = new HttpElementType("FIELD_LINE");
  IElementType FIELD_NAME = new HttpElementType("FIELD_NAME");
  IElementType FIELD_VALUE = new HttpElementType("FIELD_VALUE");
  IElementType MESSAGE_BODY = new HttpElementType("MESSAGE_BODY");
  IElementType MESSAGE_LINE = new HttpElementType("MESSAGE_LINE");
  IElementType METHOD = new HttpElementType("METHOD");
  IElementType REQUEST = new HttpElementType("REQUEST");
  IElementType REQUESTS = new HttpElementType("REQUESTS");
  IElementType REQUEST_BLOCK = new HttpElementType("REQUEST_BLOCK");
  IElementType REQUEST_DEFINER = new HttpElementType("REQUEST_DEFINER");
  IElementType REQUEST_LINE = new HttpElementType("REQUEST_LINE");
  IElementType TAG_COMMENT_LINE = new HttpElementType("TAG_COMMENT_LINE");

  IElementType ANY_TOKEN = new HttpTokenType("ANY_TOKEN");
  IElementType BODY_REQUEST_SEPARATOR = new HttpTokenType("BODY_REQUEST_SEPARATOR");
  IElementType COLON = new HttpTokenType(":");
  IElementType COMMENT_LINE = new HttpTokenType("COMMENT_LINE");
  IElementType COMMENT_SEPARATOR = new HttpTokenType("COMMENT_SEPARATOR");
  IElementType CRLF = new HttpTokenType("CRLF");
  IElementType FIELD_CONTENT = new HttpTokenType("FIELD_CONTENT");
  IElementType HTTP_TOKEN = new HttpTokenType("HTTP_TOKEN");
  IElementType HTTP_VERSION = new HttpTokenType("HTTP_VERSION");
  IElementType OWS = new HttpTokenType("OWS");
  IElementType REQUEST_SEPARATOR = new HttpTokenType("###");
  IElementType REQUEST_TARGET = new HttpTokenType("REQUEST_TARGET");
  IElementType SP = new HttpTokenType(" ");
  IElementType TAG_COMMENT_LINE_1 = new HttpTokenType("TAG_COMMENT_LINE_1");
  IElementType TAG_COMMENT_LINE_2 = new HttpTokenType("TAG_COMMENT_LINE_2");
  IElementType TAG_TOKEN = new HttpTokenType("TAG_TOKEN");
  IElementType WHITE_SPACE = new HttpTokenType("WHITE_SPACE");

  class Factory {
    public static PsiElement createElement(ASTNode node) {
      IElementType type = node.getElementType();
      if (type == COMMENT) {
        return new HttpCommentImpl(node);
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
      else if (type == MESSAGE_BODY) {
        return new HttpMessageBodyImpl(node);
      }
      else if (type == MESSAGE_LINE) {
        return new HttpMessageLineImpl(node);
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
      else if (type == REQUEST_DEFINER) {
        return new HttpRequestDefinerImpl(node);
      }
      else if (type == REQUEST_LINE) {
        return new HttpRequestLineImpl(node);
      }
      else if (type == TAG_COMMENT_LINE) {
        return new HttpTagCommentLineImpl(node);
      }
      throw new AssertionError("Unknown element type: " + type);
    }
  }
}
