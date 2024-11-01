package com.explyt.spring.core.language.profiles;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;

import static com.intellij.psi.TokenType.BAD_CHARACTER;
import static com.intellij.psi.TokenType.WHITE_SPACE;
import static com.explyt.spring.core.language.profiles.psi.ProfilesTypes.*;

%%

%{
  public _ProfilesLexer() {
    this((java.io.Reader)null);
  }
%}

%public
%class _ProfilesLexer
%implements FlexLexer
%function advance
%type IElementType
%unicode

EOL=\R
WHITE_SPACE=\s+

WHITESPACE=[ \t\n\x0B\f\r]
VALUE=[\p{L}_0-9]*

%%
<YYINITIAL> {
  {WHITE_SPACE}       { return WHITE_SPACE; }

  "!"                 { return NOT; }
  "&"                 { return AND; }
  "|"                 { return OR; }
  "("                 { return LPAREN; }
  ")"                 { return RPAREN; }

  {WHITESPACE}        { return WHITESPACE; }
  {VALUE}             { return VALUE; }

}

[^] { return BAD_CHARACTER; }
