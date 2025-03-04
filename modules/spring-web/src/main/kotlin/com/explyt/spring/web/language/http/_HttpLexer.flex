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

// EOL=\R
WHITE_SPACE=[ \t\x0B\f]+

CRLF=\r?\n
OWS=[ \t]*
BODY_REQUEST_SEPARATOR=###.*
COMMENT_SEPARATOR="//"|#
COMMENT_LINE=("//"|#).*
TAG_COMMENT_LINE_1=#[ \t\n\x0B\f\r]+@.+
TAG_COMMENT_LINE_2="//"[ \t\n\x0B\f\r]+@.+
ANY_TOKEN=.*
TAG_TOKEN=@.*
REQUEST_TARGET=[^ \t\n\x0B\f\r]*
HTTP_VERSION=HTTP"/"[0-9]\.[0-9]
FIELD_CONTENT=[\p{Graph}\x80-\xff]([ \t]*[\p{Graph}\x80-\xff])*
HTTP_TOKEN=[!#$%&'*+\-.\^_`|~\p{Alnum}}]+

%state REQUEST_NAME_STATE
%state TAG_COMMENT_STATE

%state REQUEST_TARGET_STATE
%state HTTP_VERSION_STATE

%state FIELD_NAME_STATE
%state FIELD_VALUE_STATE

%state MESSAGE_BODY_STATE

%state FAKE_STATE

%%
//////////////////////////////////////////////////// REQUEST START /////////////////////////////////////////////////////

<YYINITIAL, FIELD_NAME_STATE, MESSAGE_BODY_STATE> {
  {BODY_REQUEST_SEPARATOR}  { yypushback(yylength() - 3); yybegin(REQUEST_NAME_STATE); return REQUEST_SEPARATOR; }
}

<YYINITIAL> {
  {TAG_COMMENT_LINE_1}      { yypushback(yylength() - 1); yybegin(TAG_COMMENT_STATE); return COMMENT_SEPARATOR; }
  {TAG_COMMENT_LINE_2}      { yypushback(yylength() - 2); yybegin(TAG_COMMENT_STATE); return COMMENT_SEPARATOR; }
  {HTTP_TOKEN}              { yybegin(REQUEST_TARGET_STATE); return HTTP_TOKEN; }
}

///////////////////////////////////////////////// PRE-REQUEST COMMENTS /////////////////////////////////////////////////

<REQUEST_NAME_STATE> {
  {ANY_TOKEN}               { return ANY_TOKEN; }
  {CRLF}                    { yybegin(YYINITIAL); return CRLF; }
}

<TAG_COMMENT_STATE> {
  {TAG_TOKEN}               { return TAG_TOKEN; }
  {CRLF}                    { yybegin(YYINITIAL); return CRLF; }
  {WHITE_SPACE}             { return WHITE_SPACE; }
}

///////////////////////////////////////////////////// REQUEST LINE /////////////////////////////////////////////////////

<REQUEST_TARGET_STATE> {
  {REQUEST_TARGET}          { yybegin(HTTP_VERSION_STATE); return REQUEST_TARGET; }
}

<HTTP_VERSION_STATE> {
  {HTTP_VERSION}            { return HTTP_VERSION; }
}

<REQUEST_TARGET_STATE, HTTP_VERSION_STATE> {
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
  {FIELD_CONTENT}           { return FIELD_CONTENT; }
  {OWS}                     { return OWS; }
  {CRLF}                    { yybegin(FIELD_NAME_STATE); return CRLF; }
}

///////////////////////////////////////////////////// MESSAGE BODY /////////////////////////////////////////////////////

<MESSAGE_BODY_STATE> {
// REMEMBER: there is still {BODY_REQUEST_SEPARATOR}
  {ANY_TOKEN}               { return ANY_TOKEN; }
}

///////////////////////////////////////////////////////// CORE /////////////////////////////////////////////////////////

" "                       { return SP; }
{CRLF}                    { return CRLF; }

{COMMENT_LINE}            { return COMMENT_LINE; }

[^] { return BAD_CHARACTER; }
