/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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

package com.zimbra.cs.index;

import java.io.Reader;
import java.io.StringReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.KeywordTokenizer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.index.analysis.AddrCharTokenizer;
import com.zimbra.cs.index.analysis.CSVTokenizer;
import com.zimbra.cs.index.analysis.ContactTokenFilter;
import com.zimbra.cs.index.analysis.FieldTokenizer;
import com.zimbra.cs.index.analysis.FilenameTokenizer;
import com.zimbra.cs.index.analysis.MimeTypeTokenFilter;
import com.zimbra.cs.index.analysis.NumberTokenizer;
import com.zimbra.cs.index.analysis.UniversalAnalyzer;

/***
 * Global analyzer wrapper for Zimbra Indexer.
 * <p>
 * You DO NOT need to instantiate multiple copies of this class -- just call
 * {@link #getInstance()} whenever you need an instance of this class.
 *
 * @since Apr 26, 2004
 * @author tim
 * @author ysasaki
 */
public class ZimbraAnalyzer extends Analyzer {
    private static final ZimbraAnalyzer SINGLETON = new ZimbraAnalyzer();
    private static final Map<String, Analyzer> sAnalyzerMap =
        new HashMap<String, Analyzer>();

    private final Analyzer defaultAnalyzer = new UniversalAnalyzer();

    protected ZimbraAnalyzer() {
    }

    /***
     * Extension analyzers.
     * <p>
     * Extension analyzers must call {@link #registerAnalyzer(String, Analyzer)}
     * on startup.
     *
     * @param name
     * @return analyzer
     */
    public static Analyzer getAnalyzer(String name) {
        Analyzer toRet = sAnalyzerMap.get(name);
        if (toRet == null) {
            return getInstance();
        }
        return toRet;
    }

    /**
     * We maintain a single global instance for our default analyzer, since it
     * is completely thread safe.
     *
     * @return singleton
     */
    public static Analyzer getInstance() {
        return SINGLETON;
    }

    /**
     * A custom Lucene Analyzer is registered with this API, usually by a Zimbra
     * Extension.
     * <p>
     * Accounts are configured to use a particular analyzer by setting the
     * "zimbraTextAnalyzer" key in the Account or COS setting.
     *
     * The custom analyzer is assumed to be a stateless single instance
     * (although it can and probably should return a new TokenStream instance
     * from it's APIs)
     *
     * @param name a unique name identifying the Analyzer, it is referenced by
     *  Account or COS settings in LDAP.
     * @param analyzer a Lucene analyzer instance which can be used by accounts
     *  that are so configured.
     * @throws ServiceException
     */
    public static void registerAnalyzer(String name, Analyzer analyzer)
        throws ServiceException {

        if (sAnalyzerMap.containsKey(name)) {
            throw ServiceException.FAILURE("Cannot register analyzer: " +
                    name + " because there is one already registered with that name.",
                    null);
        }

        sAnalyzerMap.put(name, analyzer);
    }

    /**
     * Remove a previously-registered custom Analyzer from the system.
     *
     * @param name
     */
    public static void unregisterAnalyzer(String name) {
        sAnalyzerMap.remove(name);
    }

    public static String getAllTokensConcatenated(String fieldName, String text) {
        return getAllTokensConcatenated(fieldName, new StringReader(text));
    }

    public static String getAllTokensConcatenated(String fieldName, Reader reader) {
        StringBuilder toReturn = new StringBuilder();

        TokenStream stream = SINGLETON.tokenStream(fieldName, reader);
        TermAttribute term = stream.addAttribute(TermAttribute.class);

        try {
            stream.reset();
            while (stream.incrementToken()) {
                toReturn.append(term.term());
                toReturn.append(" ");
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
        if (field.equals(LuceneFields.L_H_MESSAGE_ID)) {
            return new KeywordTokenizer(reader);
        } else if (field.equals(LuceneFields.L_FIELD)) {
            return new FieldTokenizer(reader);
        } else if (field.equals(LuceneFields.L_ATTACHMENTS) ||
                field.equals(LuceneFields.L_MIMETYPE)) {
            return new MimeTypeTokenFilter(new CSVTokenizer(reader));
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
            return defaultAnalyzer.tokenStream(field, reader);
        }
    }

    @Override
    public TokenStream reusableTokenStream(String field, Reader reader) {
        return tokenStream(field, reader);
    }

}
