package com.esprito.jpql;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;

import static com.intellij.psi.TokenType.BAD_CHARACTER;
import static com.intellij.psi.TokenType.WHITE_SPACE;
import static com.esprito.jpql.psi.JpqlTypes.*;

%%

%{
  public _JpqlLexer() {
    this((java.io.Reader)null);
  }
%}

%public
%class _JpqlLexer
%implements FlexLexer
%function advance
%type IElementType
%unicode

EOL=\R
WHITE_SPACE=\s+

WHITESPACE=[ \n\r\t\f]
BOOLEAN_LITERAL=(TRUE|FALSE)
ID=[a-zA-Z][a-zA-Z0-9]*
NUMERIC_LITERAL=[0-9]+(\.[0-9]+)*
STRING_LITERAL='([^']|'')*'
DATETIME_LITERAL=\{\s*[dt]s?\s+'([^']|'')*'\s*\}

%%
<YYINITIAL> {
  {WHITE_SPACE}           { return WHITE_SPACE; }

  ","                     { return COMMA; }
  "."                     { return DOT; }
  "("                     { return LPAREN; }
  ")"                     { return RPAREN; }
  ":"                     { return COLON; }
  "+"                     { return PLUS; }
  "-"                     { return MINUS; }
  "*"                     { return MUL; }
  "/"                     { return DIV; }
  "="                     { return EQ; }
  "<>"                    { return NEQ; }
  ">="                    { return GTE; }
  ">"                     { return GT; }
  "<="                    { return LTE; }
  "<"                     { return LT; }
  ";"                     { return SEMICOLON; }
  "}"                     { return RBRACE; }
  "FROM"                  { return FROM; }
  "AS"                    { return AS; }
  "FETCH"                 { return FETCH; }
  "LEFT"                  { return LEFT; }
  "OUTER"                 { return OUTER; }
  "INNER"                 { return INNER; }
  "JOIN"                  { return JOIN; }
  "IN"                    { return IN; }
  "UPDATE"                { return UPDATE; }
  "SET"                   { return SET; }
  "NULL"                  { return NULL; }
  "DELETE"                { return DELETE; }
  "SELECT"                { return SELECT; }
  "DISTINCT"              { return DISTINCT; }
  "OBJECT"                { return OBJECT; }
  "NEW"                   { return NEW; }
  "AVG"                   { return AVG; }
  "MAX"                   { return MAX; }
  "MIN"                   { return MIN; }
  "SUM"                   { return SUM; }
  "COUNT"                 { return COUNT; }
  "WHERE"                 { return WHERE; }
  "GROUP"                 { return GROUP; }
  "BY"                    { return BY; }
  "HAVING"                { return HAVING; }
  "ORDER"                 { return ORDER; }
  "ON"                    { return ON; }
  "ASC"                   { return ASC; }
  "DESC"                  { return DESC; }
  "LIMIT"                 { return LIMIT; }
  "OFFSET"                { return OFFSET; }
  "FIRST"                 { return FIRST; }
  "NEXT"                  { return NEXT; }
  "ROW"                   { return ROW; }
  "ROWS"                  { return ROWS; }
  "ONLY"                  { return ONLY; }
  "WITH"                  { return WITH; }
  "TIES"                  { return TIES; }
  "PERCENT"               { return PERCENT; }
  "OR"                    { return OR; }
  "AND"                   { return AND; }
  "NOT"                   { return NOT; }
  "LIKE"                  { return LIKE; }
  "ESCAPE"                { return ESCAPE; }
  "IS"                    { return IS; }
  "EMPTY"                 { return EMPTY; }
  "MEMBER"                { return MEMBER; }
  "OF"                    { return OF; }
  "EXISTS"                { return EXISTS; }
  "ALL"                   { return ALL; }
  "ANY"                   { return ANY; }
  "SOME"                  { return SOME; }
  "BETWEEN"               { return BETWEEN; }
  "INTEGER"               { return INTEGER; }
  "STRING"                { return STRING; }
  "DATE"                  { return DATE; }
  "TIME"                  { return TIME; }
  "TIMESTAMP"             { return TIMESTAMP; }
  "BOOLEAN"               { return BOOLEAN; }
  "TYPE"                  { return TYPE; }
  "LENGTH"                { return LENGTH; }
  "LOCATE"                { return LOCATE; }
  "ABS"                   { return ABS; }
  "SQRT"                  { return SQRT; }
  "MOD"                   { return MOD; }
  "SIZE"                  { return SIZE; }
  "INDEX"                 { return INDEX; }
  "CURRENT_DATE"          { return CURRENT_DATE; }
  "CURRENT_TIME"          { return CURRENT_TIME; }
  "CURRENT_TIMESTAMP"     { return CURRENT_TIMESTAMP; }
  "CONCAT"                { return CONCAT; }
  "SUBSTRING"             { return SUBSTRING; }
  "TRIM"                  { return TRIM; }
  "LOWER"                 { return LOWER; }
  "UPPER"                 { return UPPER; }
  "LEADING"               { return LEADING; }
  "TRAILING"              { return TRAILING; }
  "BOTH"                  { return BOTH; }
  "CASE"                  { return CASE; }
  "ELSE"                  { return ELSE; }
  "END"                   { return END; }
  "WHEN"                  { return WHEN; }
  "THEN"                  { return THEN; }
  "COALESCE"              { return COALESCE; }
  "NULLIF"                { return NULLIF; }
  "KEY"                   { return KEY; }
  "VALUE"                 { return VALUE; }
  "ENTRY"                 { return ENTRY; }

  {WHITESPACE}            { return WHITESPACE; }
  {BOOLEAN_LITERAL}       { return BOOLEAN_LITERAL; }
  {ID}                    { return ID; }
  {NUMERIC_LITERAL}       { return NUMERIC_LITERAL; }
  {STRING_LITERAL}        { return STRING_LITERAL; }
  {DATETIME_LITERAL}      { return DATETIME_LITERAL; }

}

[^] { return BAD_CHARACTER; }
