/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.index.analysis;

import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

%%

%class UniversalLexer
%final
%unicode
%type UniversalTokenizer.TokenType
%function next
%pack
%char

%{

int yychar() { return yychar; }

void getTerm(CharTermAttribute t) {
    t.copyBuffer(zzBuffer, zzStartRead, zzMarkedPos - zzStartRead);
}

void getTerm(CharTermAttribute t, int offset, int len) {
    t.copyBuffer(zzBuffer, zzStartRead + offset, len);
}

%}

CJK_LETTER = [\u2E80-\u2FFF\u3040-\u9FFF\uAC00-\uD7FF\uFF00-\uFFEF]
/*
 * 2E80-2EFF CJK Radicals Supplement
 * 2F00-2FDF Kangxi Radicals
 * 2FF0-2FFF Ideographic Description Characters
 *------------------------------------------------------------------------------
 * 3000-303F [EXCLUDE] CJK Symbols and Punctuation
 *------------------------------------------------------------------------------
 * 3040-309F Hiragana
 * 30A0-30FF Katakana
 * 3100-312F Bopomofo
 * 3130-318F Hangul Compatibility Jamo
 * 3190-319F Kanbun
 * 31A0-31BF Bopomofo Extended
 * 31C0-31EF CJK Strokes
 * 31F0-31FF Katakana Phonetic Extensions
 * 3200-32FF Enclosed CJK Letters and Months
 * 3300-33FF CJK Compatibility
 * 3400-4DBF CJK Unified Ideographs Extension A
 * 4DC0-4DFF Yijing Hexagram Symbols
 * 4E00-9FFF CJK Unified Ideographs
 *------------------------------------------------------------------------------
 * AC00-D7AF Hangul Syllables
 * D7B0-D7FF Hangul Jamo Extended-B
 *------------------------------------------------------------------------------
 * FF00-FFEF Halfwidth and Fullwidth Forms
 */

CJK = {CJK_LETTER}+
LETTER = !(![:letter:]|{CJK_LETTER})
WSPACE = \r\n | [ \r\n\t\f]
ALPHA = {LETTER}+
ALNUM = ({LETTER}|[:digit:])+
APOSTROPHE =  {ALPHA} ("'" {ALPHA})+
PUNC = "_"|"-"|"/"|"."|","
ACRONYM = {LETTER} "." ({LETTER} ".")+
COMPANY = {ALPHA} ("&"|"@") {ALPHA}
EMAIL = {ALNUM} (("."|"-"|"_") {ALNUM})* "@" {ALNUM} (("."|"-") {ALNUM})+
HOST = {ALNUM} ("." {ALNUM})+
HAS_DIGIT = ({LETTER}|[:digit:])* [:digit:] ({LETTER}|[:digit:])*
// floating point, serial, model numbers, ip addresses, etc.
// every other segment must have at least one digit
NUM = ({ALNUM} {PUNC} {HAS_DIGIT}
    |  {HAS_DIGIT} {PUNC} {ALNUM}
    |  {ALNUM} ({PUNC} {HAS_DIGIT} {PUNC} {ALNUM})+
    |  {HAS_DIGIT} ({PUNC} {ALNUM} {PUNC} {HAS_DIGIT})+
    |  {ALNUM} {PUNC} {HAS_DIGIT} ({PUNC} {ALNUM} {PUNC} {HAS_DIGIT})+
    |  {HAS_DIGIT} {PUNC} {ALNUM} ({PUNC} {HAS_DIGIT} {PUNC} {ALNUM})+)

%%

{ALNUM}      { return UniversalTokenizer.TokenType.ALNUM; }
{APOSTROPHE} { return UniversalTokenizer.TokenType.APOSTROPHE; }
{ACRONYM}    { return UniversalTokenizer.TokenType.ACRONYM; }
{COMPANY}    { return UniversalTokenizer.TokenType.COMPANY; }
{EMAIL}      { return UniversalTokenizer.TokenType.EMAIL; }
{HOST}       { return UniversalTokenizer.TokenType.HOST; }
{NUM}        { return UniversalTokenizer.TokenType.NUM; }
{CJK}        { return UniversalTokenizer.TokenType.CJK; }
. | {WSPACE} { /* ignore */ }
