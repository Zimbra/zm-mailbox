package com.zimbra.cs.index.analysis;

import java.io.Reader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharReader;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;

/**
 * Like UniversalAnalyzer, except doesn't drop stop words
 */
public final class SearchHistoryQueryAnalyzer extends Analyzer {

    @Override
    public TokenStream tokenStream(String field, Reader in) {
        Tokenizer tokenizer = new UniversalTokenizer(new NormalizeTokenFilter(CharReader.get(in)));
        return new UniversalAnalyzer.UniversalTokenFilter(tokenizer);
    }

}
