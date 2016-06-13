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

import java.io.IOException;
import java.io.Reader;

import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;

/**
 * A grammar-based tokenizer using JFlex.
 * <p>
 * The implementation is based on {@code StandardAnalyzer} extending an ability
 * of tokenizing CJK unicode blocks where bigram tokenization is applied.
 *
 * @author ysasaki
 */
final class UniversalTokenizer extends Tokenizer {

    enum TokenType {
        ALNUM, APOSTROPHE, ACRONYM, COMPANY, EMAIL, HOST, NUM, CJK;
    }

    private final UniversalLexer lexer;
    private final CharTermAttribute termAttr = addAttribute(CharTermAttribute.class);
    private final TypeAttribute typeAttr = addAttribute(TypeAttribute.class);
    private final OffsetAttribute offsetAttr = addAttribute(OffsetAttribute.class);
    private final PositionIncrementAttribute posIncAttr = addAttribute(PositionIncrementAttribute.class);
    private int cjk = -1;

    UniversalTokenizer(Reader in) {
        super(in);
        lexer = new UniversalLexer(in);
    }

    @Override
    public boolean incrementToken() throws IOException {
        clearAttributes();

        if (cjk >= 0) { // more to process CJK
            if (cjk + 1 < lexer.yylength()) {
                lexer.getTerm(termAttr, cjk, 2); // bigram
                setOffset(lexer.yychar() + cjk, 2);
                posIncAttr.setPositionIncrement(1);
                typeAttr.setType(TokenType.CJK.name());
                cjk++;
                return true;
            } else { // end of CJK
                cjk = -1;
            }
        }

        while (true) {
            TokenType type = lexer.next();
            if (type == null) { // EOF
                return false;
            }

            if (type == TokenType.CJK) {
                if (lexer.yylength() == 1) {
                    lexer.getTerm(termAttr, 0, 1);
                } else {
                    lexer.getTerm(termAttr, 0, 2);
                    cjk = 1;
                }
                setOffset(lexer.yychar(), termAttr.length());
                posIncAttr.setPositionIncrement(1);
                typeAttr.setType(type.name());
            } else {
                lexer.getTerm(termAttr);
                setOffset(lexer.yychar(), termAttr.length());
                posIncAttr.setPositionIncrement(1);
                typeAttr.setType(type.name());
            }
            return true;
        }
    }

    @Override
    public final void end() {
      // set final offset
      int offset = lexer.yychar() + lexer.yylength();
      offsetAttr.setOffset(offset, offset);
    }

    @Override
    public void reset() throws IOException {
        super.reset();
        lexer.yyreset(input);
    }

    @Override
    public void reset(Reader reader) throws IOException {
        super.reset(reader);
        reset();
    }

    private void setOffset(int start, int len) {
        offsetAttr.setOffset(start, start + len);
    }

}
