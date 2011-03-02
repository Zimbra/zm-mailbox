/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011 Zimbra, Inc.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.index.analysis;

import java.io.IOException;
import java.io.Reader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharReader;
import org.apache.lucene.analysis.StopAnalyzer;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;

/**
 * Hybrid {@link Analyzer} of {@code StandardAnalyzer} and {@code CJKAnalyzer}.
 * <p>
 * This {@link Analyzer} may not be perfect compared to ones that are optimized
 * for a specific language, which requires to switch analyzers depending on the
 * language, but does a decent job for most languages and even mixed text just
 * by this single analyzer. The implementation is based on {@code StandardAnalyzer},
 * and applies bigram tokenization to CJK unicode blocks.
 *
 * @author ysasaki
 */
public final class UniversalAnalyzer extends Analyzer {

    private Tokenizer savedTokenizer;
    private TokenStream savedTokenStream;

    @Override
    public TokenStream tokenStream(String field, Reader in) {
        return createTokenStream(createTokenizer(in));
    }

    @Override
    public final TokenStream reusableTokenStream(String field, Reader in)
        throws IOException {

        if (savedTokenizer != null && savedTokenStream != null) {
            savedTokenizer.reset(new NormalizeTokenFilter(CharReader.get(in)));
        } else {
            savedTokenizer = createTokenizer(in);
            savedTokenStream = createTokenStream(savedTokenizer);
        }
        return savedTokenStream;
    }

    private Tokenizer createTokenizer(Reader in) {
        return new UniversalTokenizer(new NormalizeTokenFilter(CharReader.get(in)));
    }

    private TokenStream createTokenStream(Tokenizer tokenizer) {
        TokenStream result = new UniversalTokenFilter(tokenizer);
        // disable position increment for backward compatibility
        result = new StopFilter(false, result, StopAnalyzer.ENGLISH_STOP_WORDS_SET);
        return result;
    }

    private static class UniversalTokenFilter extends TokenFilter {
        private TermAttribute termAttr = addAttribute(TermAttribute.class);
        private TypeAttribute typeAttr = addAttribute(TypeAttribute.class);

        UniversalTokenFilter(TokenStream in) {
            super(in);
        }

        @Override
        public boolean incrementToken() throws IOException {
            if (!input.incrementToken()) {
                return false;
            }

            String type = typeAttr.type();
            if (type == UniversalTokenizer.TokenType.APOSTROPHE.name()) {
                if (termAttr.term().endsWith("'s")) {
                    // remove 's from possessions
                    termAttr.setTermLength(termAttr.termLength() - 2);
                }
            } else if (type == UniversalTokenizer.TokenType.ACRONYM.name()) {
                // remove dots from acronyms
                termAttr.setTermBuffer(termAttr.term().replace(".", ""));
            }

            return true;
        }
    }

}
