// This is a generated file. Not intended for manual editing.
package com.explyt.spring.web.language.http.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import generated.psi.impl.*;

public interface HttpTypes {

  IElementType METHOD = new HttpElementType("METHOD");
  IElementType REQUEST = new HttpElementType("REQUEST");
  IElementType REQUESTS = new HttpElementType("REQUESTS");
  IElementType REQUEST_BLOCK = new HttpElementType("REQUEST_BLOCK");
  IElementType URL = new HttpElementType("URL");
  IElementType VARIABLE = new HttpElementType("VARIABLE");

  IElementType DELETE = new HttpTokenType("DELETE");
  IElementType GET = new HttpTokenType("GET");
  IElementType HEAD = new HttpTokenType("HEAD");
  IElementType HTTP = new HttpTokenType("http://");
  IElementType HTTPS = new HttpTokenType("https://");
  IElementType IDENTIFIER = new HttpTokenType("IDENTIFIER");
  IElementType LBRACES = new HttpTokenType("{{");
  IElementType LINE_COMMENT = new HttpTokenType("line_comment");
  IElementType OPTIONS = new HttpTokenType("OPTIONS");
  IElementType PATCH = new HttpTokenType("PATCH");
  IElementType POST = new HttpTokenType("POST");
  IElementType PUT = new HttpTokenType("PUT");
  IElementType RBRACES = new HttpTokenType("}}");
  IElementType REQUEST_SEPARATOR = new HttpTokenType("REQUEST_SEPARATOR");

  class Factory {
    public static PsiElement createElement(ASTNode node) {
      IElementType type = node.getElementType();
      if (type == METHOD) {
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
      else if (type == URL) {
        return new HttpUrlImpl(node);
      }
      else if (type == VARIABLE) {
        return new HttpVariableImpl(node);
      }
      throw new AssertionError("Unknown element type: " + type);
    }
  }
}
