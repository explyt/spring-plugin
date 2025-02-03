
package com.explyt.spring.web.language.http;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;

import static com.intellij.psi.TokenType.BAD_CHARACTER;
import static com.intellij.psi.TokenType.WHITE_SPACE;
import static com.explyt.spring.web.language.http.psi.HttpTypes.*;

%%

%{
  public _HttpLexer() {
    this((java.io.Reader)null);
  }
%}

%public
%class _HttpLexer
%implements FlexLexer
%function advance
%type IElementType
%unicode

EOL=\R
WHITE_SPACE=\s+

REQUEST_SEPARATOR=###.*
LINE_COMMENT=(\/\/|#).*
WHITESPACE=[ \t\n\x0B\f\r]+

%state VARIABLE_STATE

%%
<YYINITIAL> {
  {WHITE_SPACE}             { return WHITE_SPACE; }

  "{{"                      { yybegin(VARIABLE_STATE); return LBRACES; }
  "}}"                      { return RBRACES; }
  "http://"                 { return HTTP; }
  "https://"                { return HTTPS; }
  "GET"                     { return GET; }
  "POST"                    { return POST; }
  "PUT"                     { return PUT; }
  "DELETE"                  { return DELETE; }
  "PATCH"                   { return PATCH; }
  "HEAD"                    { return HEAD; }
  "OPTIONS"                 { return OPTIONS; }

  {REQUEST_SEPARATOR}       { return REQUEST_SEPARATOR; }
  {LINE_COMMENT}            { return LINE_COMMENT; }
  {WHITESPACE}              { return WHITESPACE; }

}

<VARIABLE_STATE> {
  \$?[\p{Alnum}]+           { yybegin(YYINITIAL); return IDENTIFIER; }
}

[^] { return BAD_CHARACTER; }
