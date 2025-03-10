/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 *
 * Modifications Copyright OpenSearch Contributors. See
 * GitHub history for details.
 */

/*
 *   Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License").
 *   You may not use this file except in compliance with the License.
 *   A copy of the License is located at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   or in the "license" file accompanying this file. This file is distributed
 *   on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *   express or implied. See the License for the specific language governing
 *   permissions and limitations under the License.
 */


parser grammar OpenSearchPPLParser;
options { tokenVocab=OpenSearchPPLLexer; }

root
    : pplStatement? EOF
    ;

/** statement */
pplStatement
    : searchCommand (PIPE commands)*
    ;

/** commands */
commands
    : whereCommand | fieldsCommand | renameCommand | statsCommand | dedupCommand | sortCommand | evalCommand | headCommand
    | topCommand | rareCommand;

searchCommand
    : (SEARCH)? fromClause                                          #searchFrom
    | (SEARCH)? fromClause logicalExpression                        #searchFromFilter
    | (SEARCH)? logicalExpression fromClause                        #searchFilterFrom
    ;

whereCommand
    : WHERE logicalExpression
    ;

fieldsCommand
    : FIELDS (PLUS | MINUS)? fieldList
    ;

renameCommand
    : RENAME renameClasue (COMMA renameClasue)*
    ;

statsCommand
    : STATS
    (PARTITIONS EQUAL partitions=integerLiteral)?
    (ALLNUM EQUAL allnum=booleanLiteral)?
    (DELIM EQUAL delim=stringLiteral)?
    statsAggTerm (COMMA statsAggTerm)*
    (byClause)?
    (DEDUP_SPLITVALUES EQUAL dedupsplit=booleanLiteral)?
    ;

dedupCommand
    : DEDUP
    (number=integerLiteral)?
    fieldList
    (KEEPEMPTY EQUAL keepempty=booleanLiteral)?
    (CONSECUTIVE EQUAL consecutive=booleanLiteral)?
    ;

sortCommand
    : SORT sortbyClause
    ;

evalCommand
    : EVAL evalClause (COMMA evalClause)*
    ;

headCommand
    : HEAD
    (number=integerLiteral)?
    ;
    
topCommand
    : TOP
    (number=integerLiteral)?
    fieldList
    (byClause)?
    ;

rareCommand
    : RARE
    fieldList
    (byClause)?
    ;

/** clauses */
fromClause
    : SOURCE EQUAL tableSource
    | INDEX EQUAL tableSource
    ;

renameClasue
    : orignalField=wcFieldExpression AS renamedField=wcFieldExpression
    ;

byClause
    : BY fieldList
    ;

sortbyClause
    : sortField (COMMA sortField)*
    ;

evalClause
    : fieldExpression EQUAL expression
    ;

/** aggregation terms */
statsAggTerm
    : statsFunction (AS alias=wcFieldExpression)?
    ;

/** aggregation functions */
statsFunction
    : statsFunctionName LT_PRTHS valueExpression RT_PRTHS           #statsFunctionCall
    | COUNT LT_PRTHS RT_PRTHS                                       #countAllFunctionCall
    | (DISTINCT_COUNT | DC) LT_PRTHS valueExpression RT_PRTHS       #distinctCountFunctionCall
    | percentileAggFunction                                         #percentileAggFunctionCall
    ;

statsFunctionName
    : AVG | COUNT | SUM | MIN | MAX | VAR_SAMP | VAR_POP | STDDEV_SAMP | STDDEV_POP
    ;

percentileAggFunction
    : PERCENTILE '<' value=integerLiteral '>' LT_PRTHS aggField=fieldExpression RT_PRTHS
    ;

/** expressions */
expression
    : logicalExpression
    | comparisonExpression
    | valueExpression
    ;

logicalExpression
    : comparisonExpression                                          #comparsion
    | NOT logicalExpression                                         #logicalNot
    | left=logicalExpression OR right=logicalExpression             #logicalOr
    | left=logicalExpression (AND)? right=logicalExpression         #logicalAnd
    | left=logicalExpression XOR right=logicalExpression            #logicalXor
    | booleanExpression                                             #booleanExpr
    | relevanceExpression                                           #relevanceExpr
    ;

comparisonExpression
    : left=valueExpression comparisonOperator right=valueExpression #compareExpr
    | valueExpression IN valueList                                  #inExpr
    ;

valueExpression
    : left=valueExpression binaryOperator right=valueExpression     #binaryArithmetic
    | LT_PRTHS left=valueExpression binaryOperator
    right=valueExpression RT_PRTHS                                  #parentheticBinaryArithmetic
    | primaryExpression                                             #valueExpressionDefault
    ;

primaryExpression
    : evalFunctionCall
    | fieldExpression
    | literalValue
    ;

booleanExpression
    : booleanFunctionCall
    ;

relevanceExpression
    : relevanceFunctionName LT_PRTHS
        field=relevanceArgValue COMMA query=relevanceArgValue
        (COMMA relevanceArg)* RT_PRTHS
    ;

/** tables */
tableSource
    : qualifiedName
    | ID_DATE_SUFFIX
    ;

/** fields */
fieldList
    : fieldExpression (COMMA fieldExpression)*
    ;

wcFieldList
    : wcFieldExpression (COMMA wcFieldExpression)*
    ;

sortField
    : (PLUS | MINUS)? sortFieldExpression
    ;

sortFieldExpression
    : fieldExpression
    | AUTO LT_PRTHS fieldExpression RT_PRTHS
    | STR LT_PRTHS fieldExpression RT_PRTHS
    | IP LT_PRTHS fieldExpression RT_PRTHS
    | NUM LT_PRTHS fieldExpression RT_PRTHS
    ;

fieldExpression
    : qualifiedName
    ;

wcFieldExpression
    : wcQualifiedName
    ;

/** functions */
evalFunctionCall
    : evalFunctionName LT_PRTHS functionArgs RT_PRTHS
    ;

/** boolean functions */
booleanFunctionCall
    : conditionFunctionBase LT_PRTHS functionArgs RT_PRTHS
    ;

evalFunctionName
    : mathematicalFunctionBase
    | dateAndTimeFunctionBase
    | textFunctionBase
    | conditionFunctionBase
    ;

functionArgs
    : (functionArg (COMMA functionArg)*)?
    ;

functionArg
    : valueExpression
    ;

relevanceArg
    : relevanceArgName EQUAL relevanceArgValue
    ;

relevanceArgName
    : ANALYZER | FUZZINESS | AUTO_GENERATE_SYNONYMS_PHRASE_QUERY | MAX_EXPANSIONS | PREFIX_LENGTH
    | FUZZY_TRANSPOSITIONS | FUZZY_REWRITE | LENIENT | OPERATOR | MINIMUM_SHOULD_MATCH | ZERO_TERMS_QUERY
    | BOOST
    ;

relevanceArgValue
    : qualifiedName
    | literalValue
    ;

mathematicalFunctionBase
    : ABS | CEIL | CEILING | CONV | CRC32 | E | EXP | FLOOR | LN | LOG | LOG10 | LOG2 | MOD | PI |POW | POWER
    | RAND | ROUND | SIGN | SQRT | TRUNCATE
    | trigonometricFunctionName
    ;

trigonometricFunctionName
    : ACOS | ASIN | ATAN | ATAN2 | COS | COT | DEGREES | RADIANS | SIN | TAN
    ;

dateAndTimeFunctionBase
    : ADDDATE | DATE | DATE_ADD | DATE_SUB | DAY | DAYNAME | DAYOFMONTH | DAYOFWEEK | DAYOFYEAR | FROM_DAYS
    | HOUR | MICROSECOND | MINUTE | MONTH | MONTHNAME | QUARTER | SECOND | SUBDATE | TIME | TIME_TO_SEC
    | TIMESTAMP | TO_DAYS | YEAR | WEEK | DATE_FORMAT
    ;

/** condition function return boolean value */
conditionFunctionBase
    : LIKE
    | IF | ISNULL | ISNOTNULL | IFNULL | NULLIF
    ;

textFunctionBase
    : SUBSTR | SUBSTRING | TRIM | LTRIM | RTRIM | LOWER | UPPER | CONCAT | CONCAT_WS | LENGTH | STRCMP
    | RIGHT | LEFT | ASCII | LOCATE | REPLACE
    ;

/** operators */
comparisonOperator
    : EQUAL | NOT_EQUAL | LESS | NOT_LESS | GREATER | NOT_GREATER | REGEXP
    ;

binaryOperator
    : PLUS | MINUS | STAR | DIVIDE | MODULE
    ;

relevanceFunctionName
    : MATCH
    ;

/** literals and values*/
literalValue
    : intervalLiteral
    | stringLiteral
    | integerLiteral
    | decimalLiteral
    | booleanLiteral
    ;

intervalLiteral
    : INTERVAL valueExpression intervalUnit
    ;

stringLiteral
    : DQUOTA_STRING | SQUOTA_STRING
    ;

integerLiteral
    : (PLUS | MINUS)? INTEGER_LITERAL
    ;

decimalLiteral
    : (PLUS | MINUS)? DECIMAL_LITERAL
    ;

booleanLiteral
    : TRUE | FALSE
    ;

intervalUnit
    : MICROSECOND | SECOND | MINUTE | HOUR | DAY | WEEK | MONTH | QUARTER | YEAR | SECOND_MICROSECOND
    | MINUTE_MICROSECOND | MINUTE_SECOND | HOUR_MICROSECOND | HOUR_SECOND | HOUR_MINUTE | DAY_MICROSECOND
    | DAY_SECOND | DAY_MINUTE | DAY_HOUR | YEAR_MONTH
    ;


valueList
    : LT_PRTHS literalValue (COMMA literalValue)* RT_PRTHS
    ;

qualifiedName
    : ident (DOT ident)*                             #identsAsQualifiedName
    ;

wcQualifiedName
    : wildcard (DOT wildcard)*                       #identsAsWildcardQualifiedName
    ;

ident
    : (DOT)? ID
    | BACKTICK ident BACKTICK
    | BQUOTA_STRING
    | keywordsCanBeId
    ;

wildcard
    : ident (MODULE ident)* (MODULE)?
    | SINGLE_QUOTE wildcard SINGLE_QUOTE
    | DOUBLE_QUOTE wildcard DOUBLE_QUOTE
    | BACKTICK wildcard BACKTICK
    ;

keywordsCanBeId
    : D // OD SQL and ODBC special
    | statsFunctionName
    | TIMESTAMP | DATE | TIME
    | FIRST | LAST
    ;
