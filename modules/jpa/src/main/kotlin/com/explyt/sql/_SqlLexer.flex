package com.explyt.sql;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;

import static com.intellij.psi.TokenType.BAD_CHARACTER;
import static com.intellij.psi.TokenType.WHITE_SPACE;
import static com.explyt.sql.psi.SqlTypes.*;

%%

%{
  public _SqlLexer() {
    this((java.io.Reader)null);
  }
%}

%public
%class _SqlLexer
%implements FlexLexer
%function advance
%type IElementType
%unicode
%ignorecase

EOL=\R
WHITE_SPACE=\s+

WHITESPACE=[ \n\r\t\f]
BOOLEAN=(TRUE|FALSE)
ID=[a-zA-Z_][a-zA-Z0-9_]*
NUMERIC=[0-9]+(\.[0-9]+)*
STRING='([^']|'')*'
DATETIME=\{[ \t\n\x0B\f\r]*[dt]s?[ \t\n\x0B\f\r]+'([^']|'')*'[ \t\n\x0B\f\r]*\}
NAMED_INPUT_PARAMETER=\?|:[a-zA-Z_][a-zA-Z0-9_]*
NUMERIC_INPUT_PARAMETER=\?[0-9]+

%%
<YYINITIAL> {
  {WHITE_SPACE}                   { return WHITE_SPACE; }

  ","                             { return COMMA; }
  "."                             { return DOT; }
  "("                             { return LPAREN; }
  ")"                             { return RPAREN; }
  ":"                             { return COLON; }
  "+"                             { return PLUS; }
  "-"                             { return MINUS; }
  "*"                             { return MUL; }
  "/"                             { return DIV; }
  "="                             { return EQ; }
  "<>"                            { return NEQ; }
  ">="                            { return GTE; }
  ">"                             { return GT; }
  "<="                            { return LTE; }
  "<"                             { return LT; }
  ";"                             { return SEMICOLON; }
  "}"                             { return RBRACE; }
  "INSERT"                        { return INSERT; }
  "INTO"                          { return INTO; }
  "VALUES"                        { return VALUES; }
  "FROM"                          { return FROM; }
  "AS"                            { return AS; }
  "LEFT"                          { return LEFT; }
  "OUTER"                         { return OUTER; }
  "INNER"                         { return INNER; }
  "JOIN"                          { return JOIN; }
  "WHERE"                         { return WHERE; }
  "GROUP"                         { return GROUP; }
  "HAVING"                        { return HAVING; }
  "ON"                            { return ON; }
  "IN"                            { return IN; }
  "UPDATE"                        { return UPDATE; }
  "SET"                           { return SET; }
  "NULL"                          { return NULL; }
  "DELETE"                        { return DELETE; }
  "SELECT"                        { return SELECT; }
  "DISTINCT"                      { return DISTINCT; }
  "OBJECT"                        { return OBJECT; }
  "AVG"                           { return AVG; }
  "MAX"                           { return MAX; }
  "MIN"                           { return MIN; }
  "SUM"                           { return SUM; }
  "COUNT"                         { return COUNT; }
  "FUNCTION"                      { return FUNCTION; }
  "BY"                            { return BY; }
  "ORDER"                         { return ORDER; }
  "ASC"                           { return ASC; }
  "DESC"                          { return DESC; }
  "LIMIT"                         { return LIMIT; }
  "OFFSET"                        { return OFFSET; }
  "OR"                            { return OR; }
  "AND"                           { return AND; }
  "NOT"                           { return NOT; }
  "LIKE"                          { return LIKE; }
  "ESCAPE"                        { return ESCAPE; }
  "IS"                            { return IS; }
  "EMPTY"                         { return EMPTY; }
  "MEMBER"                        { return MEMBER; }
  "OF"                            { return OF; }
  "EXISTS"                        { return EXISTS; }
  "ALL"                           { return ALL; }
  "ANY"                           { return ANY; }
  "SOME"                          { return SOME; }
  "BETWEEN"                       { return BETWEEN; }
  "INTEGER"                       { return INTEGER; }
  "DATE"                          { return DATE; }
  "TIME"                          { return TIME; }
  "TIMESTAMP"                     { return TIMESTAMP; }
  "TYPE"                          { return TYPE; }
  "LENGTH"                        { return LENGTH; }
  "LOCATE"                        { return LOCATE; }
  "ABS"                           { return ABS; }
  "SQRT"                          { return SQRT; }
  "MOD"                           { return MOD; }
  "SIZE"                          { return SIZE; }
  "INDEX"                         { return INDEX; }
  "CURRENT_DATE"                  { return CURRENT_DATE; }
  "CURRENT_TIME"                  { return CURRENT_TIME; }
  "CURRENT_TIMESTAMP"             { return CURRENT_TIMESTAMP; }
  "CONCAT"                        { return CONCAT; }
  "SUBSTRING"                     { return SUBSTRING; }
  "TRIM"                          { return TRIM; }
  "LOWER"                         { return LOWER; }
  "UPPER"                         { return UPPER; }
  "LEADING"                       { return LEADING; }
  "TRAILING"                      { return TRAILING; }
  "BOTH"                          { return BOTH; }
  "CASE"                          { return CASE; }
  "ELSE"                          { return ELSE; }
  "END"                           { return END; }
  "WHEN"                          { return WHEN; }
  "THEN"                          { return THEN; }
  "COALESCE"                      { return COALESCE; }
  "NULLIF"                        { return NULLIF; }
  "VALUE"                         { return VALUE; }
  "KEY"                           { return KEY; }
  "ENTRY"                         { return ENTRY; }

  {WHITESPACE}                    { return WHITESPACE; }
  {BOOLEAN}                       { return BOOLEAN; }
  {ID}                            { return ID; }
  {NUMERIC}                       { return NUMERIC; }
  {STRING}                        { return STRING; }
  {DATETIME}                      { return DATETIME; }
  {NAMED_INPUT_PARAMETER}         { return NAMED_INPUT_PARAMETER; }
  {NUMERIC_INPUT_PARAMETER}       { return NUMERIC_INPUT_PARAMETER; }

}

[^] { return BAD_CHARACTER; }
