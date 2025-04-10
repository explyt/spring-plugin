package com.explyt.spring.web.language.http;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;

import java.util.ArrayList;

import static com.intellij.psi.TokenType.BAD_CHARACTER;
import static com.intellij.psi.TokenType.WHITE_SPACE;
import static com.explyt.spring.web.language.http.psi.HttpTypes.*;

%%

%{
  public _HttpLexer() {
    this((java.io.Reader)null);
  }

  private int zzSavedState = YYINITIAL;

  public final void yysavestate() {
    zzSavedState = zzLexicalState;
  }

  public final void yyloadstate() {
    zzLexicalState = zzSavedState;
  }
%}

%public
%class _HttpLexer
%implements FlexLexer
%function advance
%type IElementType
%unicode

// EOL=\R
WHITE_SPACE=[ \t\x0B\f]+

CRLF=\r?\n
OWS=[ \t]*
BODY_REQUEST_SEPARATOR=###.*
COMMENT_SEPARATOR="//"|#
COMMENT_LINE=("//"|#).*
TAG_COMMENT_LINE_1=#[ \t\n\x0B\f\r]*@.+
TAG_COMMENT_LINE_2="//"[ \t\n\x0B\f\r]*@.+
META_TOKEN=([^\\{}\s]|\\\\|\\\{|\\})*
TAG_TOKEN=@([^\\{}\s]|\\\\|\\\{|\\})*
IDENTIFIER=\$?[\w\-_.\[\]]+
FULL_REQUEST_LINE=[!#$%&'*+\-.\^_`|~\p{Alnum}]+" "[^ \t\n\x0B\f\r]+(" HTTP/"[0-9]\.[0-9])?
GET_OMMITED_REQUEST_LINE=[^ \t\n\x0B\f\r]+(" HTTP/"[0-9]\.[0-9])?
REQUEST_TARGET_VALUE=[^\s{}]*
HTTP_VERSION=HTTP"/"[0-9]\.[0-9]
HTTP_TOKEN=[!#$%&'*+\-.\^_`|~\p{Alnum}]+
FIELD_CONTENT_TOKEN=([!\"$-.0-\[\]-z|~\x80-\xff]|\\\\|\\"/"|\\#|\\\{|\\})*
REQUEST_BODY_VALUE=([^\\/#{}]|\\\\|\\"/"|\\#|\\\{|\\})*

%xstate VARIABLE_STATE

%state REQUEST_NAME_STATE
%state TAG_COMMENT_STATE

%state PRE_REQUEST_TARGET_STATE
%xstate REQUEST_TARGET_STATE
%state HTTP_VERSION_STATE

%state FIELD_NAME_STATE
%state FIELD_VALUE_STATE

%state MESSAGE_BODY_STATE

%%

//////////////////////////////////////////////////// REQUEST START /////////////////////////////////////////////////////

<YYINITIAL, FIELD_NAME_STATE, MESSAGE_BODY_STATE> {
  {BODY_REQUEST_SEPARATOR}         { yypushback(yylength() - 3); yybegin(REQUEST_NAME_STATE); return REQUEST_SEPARATOR; }
}

<YYINITIAL> {
  {TAG_COMMENT_LINE_1}             { yypushback(yylength() - 1); yybegin(TAG_COMMENT_STATE); return COMMENT_SEPARATOR; }
  {TAG_COMMENT_LINE_2}             { yypushback(yylength() - 2); yybegin(TAG_COMMENT_STATE); return COMMENT_SEPARATOR; }

  // Duplicated for correct behavior of request lines:
  {COMMENT_LINE}                   { return COMMENT_LINE; }

  {FULL_REQUEST_LINE}              {
          yypushback(yylength() - yytext().toString().indexOf(" "));
          yybegin(PRE_REQUEST_TARGET_STATE);
          return HTTP_TOKEN;
      }
  {GET_OMMITED_REQUEST_LINE}       {
          yybegin(REQUEST_TARGET_STATE);

          String yyText = yytext().toString();

          if (yyText.indexOf("{{") == 0) {
              yypushback(yylength() - 2);
              yysavestate();
              yybegin(VARIABLE_STATE);
              return LBRACES;
          }

          int cutPlace = yylength();

          int tmpPlace = yyText.indexOf(" ");
          if (tmpPlace >= 0)
              cutPlace = tmpPlace;

          tmpPlace = yyText.indexOf("{");
          if (tmpPlace == 0) {
              yypushback(yylength() - 1);
              return BAD_CHARACTER;
          } else if (tmpPlace > 0)
              cutPlace = Integer.min(cutPlace, tmpPlace);

          tmpPlace = yyText.indexOf("}");
          if (tmpPlace == 0) {
              yypushback(yylength() - 1);
              return BAD_CHARACTER;
          } else if (tmpPlace > 0)
              cutPlace = Integer.min(cutPlace, tmpPlace);

          if (cutPlace > 0 && cutPlace < yylength())
              yypushback(yylength() - cutPlace);
          return REQUEST_TARGET_VALUE;
      }
}

////////////////////////////////////////////////////// VARIABLES ///////////////////////////////////////////////////////

"{{"                             { yysavestate(); yybegin(VARIABLE_STATE); return LBRACES; }

<VARIABLE_STATE> { // THIS IS AN EXCLUSIVE STATE!
  {IDENTIFIER}                     { return IDENTIFIER; }
  "}}"                             { yyloadstate(); return RBRACES; }

  {WHITE_SPACE}                    { return WHITE_SPACE; }
  {CRLF}                           { yypushback(yylength()); yyloadstate(); break; }

  [^] { return BAD_CHARACTER; }
}

///////////////////////////////////////////////// PRE-REQUEST COMMENTS /////////////////////////////////////////////////

<TAG_COMMENT_STATE> {
  {TAG_TOKEN}                      { return TAG_TOKEN; }
}

<REQUEST_NAME_STATE, TAG_COMMENT_STATE> {
  {META_TOKEN}                     { return META_TOKEN; }
  {WHITE_SPACE}                    { return WHITE_SPACE; }
  {CRLF}                           { yybegin(YYINITIAL); return CRLF; }
}

///////////////////////////////////////////////////// REQUEST LINE /////////////////////////////////////////////////////

<PRE_REQUEST_TARGET_STATE> {
  " "                              { yybegin(REQUEST_TARGET_STATE); return SP; }
}

<REQUEST_TARGET_STATE> { // THIS IS AN EXCLUSIVE STATE!
  {REQUEST_TARGET_VALUE}           { return REQUEST_TARGET_VALUE; }
  "{{"                             { yysavestate(); yybegin(VARIABLE_STATE); return LBRACES; }

  " "                              { yybegin(HTTP_VERSION_STATE); return SP; }
  {CRLF}                           { yybegin(FIELD_NAME_STATE); return CRLF; }

  [^] { return BAD_CHARACTER; }
}

<HTTP_VERSION_STATE> {
  {HTTP_VERSION}                   { return HTTP_VERSION; }
}

<PRE_REQUEST_TARGET_STATE, REQUEST_TARGET_STATE, HTTP_VERSION_STATE> {
  {CRLF}                           { yybegin(FIELD_NAME_STATE); return CRLF; }
}

////////////////////////////////////////////////////// FIELD LINE //////////////////////////////////////////////////////

<FIELD_NAME_STATE> {
// REMEMBER: there is still {BODY_REQUEST_SEPARATOR}
  {HTTP_TOKEN}                     { return HTTP_TOKEN; }
  ":"                              { yybegin(FIELD_VALUE_STATE); return COLON; }
  {CRLF}                           { yybegin(MESSAGE_BODY_STATE); return CRLF; }
}

<FIELD_VALUE_STATE> {
  {FIELD_CONTENT_TOKEN}            { return FIELD_CONTENT_TOKEN; }
  {OWS}                            { return OWS; }
  {CRLF}                           { yybegin(FIELD_NAME_STATE); return CRLF; }
}

///////////////////////////////////////////////////// MESSAGE BODY /////////////////////////////////////////////////////

<MESSAGE_BODY_STATE> {
// REMEMBER: there is still {BODY_REQUEST_SEPARATOR}
  {REQUEST_BODY_VALUE}           { return REQUEST_BODY_VALUE; }
}

///////////////////////////////////////////////////////// CORE /////////////////////////////////////////////////////////

{WHITE_SPACE}                    { return WHITE_SPACE; }
{CRLF}                           { return CRLF; }

{COMMENT_LINE}                   { return COMMENT_LINE; }

[^] { return BAD_CHARACTER; }
