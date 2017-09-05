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
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharReader;
import org.apache.lucene.analysis.StopAnalyzer;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;

import com.google.common.base.CharMatcher;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.index.LuceneIndex;

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
        Set stopWords = StopAnalyzer.ENGLISH_STOP_WORDS_SET;
        try {
        	stopWords = Provisioning.getInstance().getConfig().getMultiAttrSet(Provisioning.A_zimbraDefaultAnalyzerStopWords);
        } catch (ServiceException e) {
        	ZimbraLog.index.error("Failed to retrieve stop words from LDAP", e);
        }
        // disable position increment for backward compatibility
        result = new StopFilter(LuceneIndex.VERSION, result, stopWords);
        return result;
    }

    static final class UniversalTokenFilter extends TokenFilter {
        private CharTermAttribute termAttr = addAttribute(CharTermAttribute.class);
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
                // endsWith "'s"
                int len = termAttr.length();
                if (len >= 2 && termAttr.charAt(len - 1) == 's' && termAttr.charAt(len - 2) == '\'') {
                    // remove 's from possessions
                    termAttr.setLength(len - 2);
                }
            } else if (type == UniversalTokenizer.TokenType.ACRONYM.name()) {
                // remove dots from acronyms
                String replace = CharMatcher.is('.').removeFrom(termAttr);
                termAttr.setEmpty().append(replace);
            }

            return true;
        }
    }

}
