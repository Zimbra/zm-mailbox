/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2013, 2014, 2015, 2016 Synacor, Inc.
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

package com.zimbra.cs.index;

import java.io.Reader;
import java.io.StringReader;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.KeywordTokenizer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import com.google.common.base.Strings;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.index.analysis.AddrCharTokenizer;
import com.zimbra.cs.index.analysis.ContactTokenFilter;
import com.zimbra.cs.index.analysis.FilenameTokenizer;
import com.zimbra.cs.index.analysis.HalfwidthKanaVoicedMappingFilter;
import com.zimbra.cs.index.analysis.NumberTokenizer;
import com.zimbra.cs.index.analysis.UniversalAnalyzer;

/***
 * Global analyzer wrapper for Zimbra Indexer.
 * <p>
 * You DO NOT need to instantiate multiple copies of this class -- just call {@link #getInstance()} whenever you need
 * an instance of this class.
 *
 * @since Apr 26, 2004
 * @author tim
 * @author ysasaki
 */
public final class ZimbraAnalyzer extends Analyzer {
    private static final ZimbraAnalyzer SINGLETON = new ZimbraAnalyzer();
    private static final Map<String, Analyzer> ANALYZERS = new ConcurrentHashMap<String, Analyzer>();
    static {
        ANALYZERS.put("StandardAnalyzer", new ForwardingAnalyzer(new StandardAnalyzer(LuceneIndex.VERSION)));
    }

    private final UniversalAnalyzer defaultAnalyzer = new UniversalAnalyzer();


    private ZimbraAnalyzer() {
    }

    /***
     * Extension analyzers.
     * <p>
     * Extension analyzers must call {@link #registerAnalyzer(String, Analyzer)} on startup.
     */
    public static Analyzer getAnalyzer(String name) {
        if (Strings.isNullOrEmpty(name)) {
            return SINGLETON;
        }
        Analyzer result = ANALYZERS.get(name);
        return result != null ? result : SINGLETON;
    }

    /**
     * We maintain a single global instance for our default analyzer, since it is completely thread safe.
     *
     * @return singleton
     */
    public static Analyzer getInstance() {
        return SINGLETON;
    }

    /**
     * A custom Lucene Analyzer is registered with this API, usually by a Zimbra Extension.
     * <p>
     * Accounts are configured to use a particular analyzer by setting the "zimbraTextAnalyzer" key in the Account or
     * COS setting.
     *
     * The custom analyzer is assumed to be a stateless single instance (although it can and probably should return a
     * new TokenStream instance from it's APIs)
     *
     * @param name a unique name identifying the Analyzer, it is referenced by Account or COS settings in LDAP
     * @param analyzer a Lucene analyzer instance which can be used by accounts that are so configured.
     */
    public static void registerAnalyzer(String name, Analyzer analyzer) throws ServiceException {
        if (ANALYZERS.containsKey(name)) {
            throw ServiceException.FAILURE("Cannot register analyzer: " + name +
                    " because there is one already registered with that name.", null);
        }
        ANALYZERS.put(name, analyzer);
    }

    /**
     * Remove a previously-registered custom Analyzer from the system.
     */
    public static void unregisterAnalyzer(String name) {
        ANALYZERS.remove(name);
    }

    public static String getAllTokensConcatenated(String fieldName, String text) {
        return getAllTokensConcatenated(fieldName, new StringReader(text));
    }

    public static String getAllTokensConcatenated(String fieldName, Reader reader) {
        StringBuilder toReturn = new StringBuilder();

        TokenStream stream = SINGLETON.tokenStream(fieldName, reader);
        CharTermAttribute term = stream.addAttribute(CharTermAttribute.class);

        try {
            stream.reset();
            while (stream.incrementToken()) {
                toReturn.append(term);
                toReturn.append(' ');
            }
            stream.end();
            stream.close();
        } catch (IOException e) {
            e.printStackTrace(); //otherwise eat it
        }

        return toReturn.toString();
    }

    @Override
    public TokenStream tokenStream(String field, Reader reader) {
        return tokenStream(field, reader, defaultAnalyzer);
    }

    private TokenStream tokenStream(String field, Reader reader, Analyzer analyzer) {
        if (field.equals(LuceneFields.L_H_MESSAGE_ID) || field.equals(LuceneFields.L_SEARCH_EXACT)) {
            return new KeywordTokenizer(reader);
        } else if (field.equals(LuceneFields.L_ATTACHMENTS) || field.equals(LuceneFields.L_MIMETYPE)) {
            throw new IllegalArgumentException("Use MimeTypeTokenStream");
        } else if (field.equals(LuceneFields.L_SORT_SIZE)) {
            return new NumberTokenizer(reader);
        } else if (field.equals(LuceneFields.L_H_FROM)
                || field.equals(LuceneFields.L_H_TO)
                || field.equals(LuceneFields.L_H_CC)
                || field.equals(LuceneFields.L_H_X_ENV_FROM)
                || field.equals(LuceneFields.L_H_X_ENV_TO)) {
            // This is only for search. We don't need address-aware tokenization
            // because we put all possible forms of address while indexing.
            // Use RFC822AddressTokenStream for indexing.
            return new AddrCharTokenizer(reader);
        } else if (field.equals(LuceneFields.L_CONTACT_DATA)) {
            return new ContactTokenFilter(new AddrCharTokenizer(reader)); // for bug 48146
        } else if (field.equals(LuceneFields.L_FILENAME)) {
            return new FilenameTokenizer(reader);
        } else {
            return analyzer.tokenStream(field, new HalfwidthKanaVoicedMappingFilter((reader)));
        }
    }

    public static final TokenStream getTokenStream(String field, Reader reader) {
        return SINGLETON.tokenStream(field, reader);
    }

    @Override
    public TokenStream reusableTokenStream(String field, Reader reader) {
        return tokenStream(field, reader);
    }

    private static final class ForwardingAnalyzer extends Analyzer {
        private final Analyzer forwarding;

        ForwardingAnalyzer(Analyzer analyzer) {
            forwarding = analyzer;
        }

        @Override
        public TokenStream tokenStream(String field, Reader reader) {
            return SINGLETON.tokenStream(field, reader, forwarding);
        }
    }

}
