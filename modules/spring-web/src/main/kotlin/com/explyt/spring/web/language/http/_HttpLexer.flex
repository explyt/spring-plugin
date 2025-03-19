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

  public static final ArrayList<Integer> zzStateStack = new ArrayList<Integer>();

  public final void yysetupstack() {
    zzStateStack.clear();
    zzStateStack.add(zzLexicalState);
  }

  public final void yystackpop() {
    zzLexicalState = zzStateStack.removeLast();
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
REQUEST_TARGET_VALUE=[^\s{}]*
HTTP_VERSION=HTTP"/"[0-9]\.[0-9]
HTTP_TOKEN=[!#$%&'*+\-.\^_`|~\p{Alnum}}]+
FIELD_CONTENT_TOKEN=([!\"$-.0-\[\]-z|~\x80-\xff]|\\\\|\\"/"|\\#|\\\{|\\})*
REQUEST_BODY_VALUE=([^\\/#{}]|\\\\|\\"/"|\\#|\\\{|\\})*

%state VARIABLE_STATE

%state REQUEST_NAME_STATE
%state TAG_COMMENT_STATE

%state PRE_REQUEST_TARGET_STATE
%state REQUEST_TARGET_STATE
%state HTTP_VERSION_STATE

%state FIELD_NAME_STATE
%state FIELD_VALUE_STATE

%state MESSAGE_BODY_STATE

%%
////////////////////////////////////////////////////// VARIABLES ///////////////////////////////////////////////////////

"{{"                      { yysetupstack(); yybegin(VARIABLE_STATE); return LBRACES; }

<VARIABLE_STATE> {
  {IDENTIFIER}              { return IDENTIFIER; }
  "}}"                      { yystackpop(); return RBRACES; }
}

//////////////////////////////////////////////////// REQUEST START /////////////////////////////////////////////////////

<YYINITIAL, FIELD_NAME_STATE, MESSAGE_BODY_STATE> {
  {BODY_REQUEST_SEPARATOR}  { yypushback(yylength() - 3); yybegin(REQUEST_NAME_STATE); return REQUEST_SEPARATOR; }
}

<YYINITIAL> {
  {TAG_COMMENT_LINE_1}      { yypushback(yylength() - 1); yybegin(TAG_COMMENT_STATE); return COMMENT_SEPARATOR; }
  {TAG_COMMENT_LINE_2}      { yypushback(yylength() - 2); yybegin(TAG_COMMENT_STATE); return COMMENT_SEPARATOR; }
  {HTTP_TOKEN}              { yybegin(PRE_REQUEST_TARGET_STATE); return HTTP_TOKEN; }
}

///////////////////////////////////////////////// PRE-REQUEST COMMENTS /////////////////////////////////////////////////

<TAG_COMMENT_STATE> {
  {TAG_TOKEN}               { return TAG_TOKEN; }
}

<REQUEST_NAME_STATE, TAG_COMMENT_STATE> {
  {META_TOKEN}              { return META_TOKEN; }
  {WHITE_SPACE}             { return WHITE_SPACE; }
  {CRLF}                    { yybegin(YYINITIAL); return CRLF; }
}

///////////////////////////////////////////////////// REQUEST LINE /////////////////////////////////////////////////////

<PRE_REQUEST_TARGET_STATE> {
  " "                       { yybegin(REQUEST_TARGET_STATE); return SP; }
}

<REQUEST_TARGET_STATE> {
  {REQUEST_TARGET_VALUE}    { return REQUEST_TARGET_VALUE; }
  " "                       { yybegin(HTTP_VERSION_STATE); return SP; }
}

<HTTP_VERSION_STATE> {
  {HTTP_VERSION}            { return HTTP_VERSION; }
}

<PRE_REQUEST_TARGET_STATE, REQUEST_TARGET_STATE, HTTP_VERSION_STATE> {
  {CRLF}                    { yybegin(FIELD_NAME_STATE); return CRLF; }
}

////////////////////////////////////////////////////// FIELD LINE //////////////////////////////////////////////////////

<FIELD_NAME_STATE> {
// REMEMBER: there is still {BODY_REQUEST_SEPARATOR}
  {HTTP_TOKEN}              { return HTTP_TOKEN; }
  ":"                       { yybegin(FIELD_VALUE_STATE); return COLON; }
  {CRLF}                    { yybegin(MESSAGE_BODY_STATE); return CRLF; }
}

<FIELD_VALUE_STATE> {
  {FIELD_CONTENT_TOKEN}     { return FIELD_CONTENT_TOKEN; }
  {OWS}                     { return OWS; }
  {CRLF}                    { yybegin(FIELD_NAME_STATE); return CRLF; }
}

///////////////////////////////////////////////////// MESSAGE BODY /////////////////////////////////////////////////////

<MESSAGE_BODY_STATE> {
// REMEMBER: there is still {BODY_REQUEST_SEPARATOR}
  {REQUEST_BODY_VALUE}    { return REQUEST_BODY_VALUE; }
}

///////////////////////////////////////////////////////// CORE /////////////////////////////////////////////////////////

{WHITE_SPACE}             { return WHITE_SPACE; }
{CRLF}                    { return CRLF; }

{COMMENT_LINE}            { return COMMENT_LINE; }

[^] { return BAD_CHARACTER; }
