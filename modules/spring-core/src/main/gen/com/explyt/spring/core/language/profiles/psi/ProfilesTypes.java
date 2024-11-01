// This is a generated file. Not intended for manual editing.
package com.explyt.spring.core.language.profiles.psi;

import com.intellij.psi.tree.IElementType;
import com.intellij.psi.PsiElement;
import com.intellij.lang.ASTNode;
import com.explyt.spring.core.language.profiles.psi.impl.*;

public interface ProfilesTypes {

  IElementType AND_EXPRESSION = new ProfilesElementType("AND_EXPRESSION");
  IElementType NOT_EXPRESSION = new ProfilesElementType("NOT_EXPRESSION");
  IElementType OR_EXPRESSION = new ProfilesElementType("OR_EXPRESSION");
  IElementType PROFILE = new ProfilesElementType("PROFILE");

  IElementType AND = new ProfilesTokenType("&");
  IElementType LPAREN = new ProfilesTokenType("(");
  IElementType NOT = new ProfilesTokenType("!");
  IElementType OR = new ProfilesTokenType("|");
  IElementType RPAREN = new ProfilesTokenType(")");
  IElementType VALUE = new ProfilesTokenType("value");

  class Factory {
    public static PsiElement createElement(ASTNode node) {
      IElementType type = node.getElementType();
      if (type == AND_EXPRESSION) {
        return new ProfilesAndExpressionImpl(node);
      }
      else if (type == NOT_EXPRESSION) {
        return new ProfilesNotExpressionImpl(node);
      }
      else if (type == OR_EXPRESSION) {
        return new ProfilesOrExpressionImpl(node);
      }
      else if (type == PROFILE) {
        return new ProfilesProfileImpl(node);
      }
      throw new AssertionError("Unknown element type: " + type);
    }
  }
}
