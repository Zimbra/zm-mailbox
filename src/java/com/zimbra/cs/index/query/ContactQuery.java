/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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
package com.zimbra.cs.index.query;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MultiPhraseQuery;
import org.apache.lucene.search.PrefixQuery;

import com.google.common.base.CharMatcher;
import com.google.common.base.Joiner;
import com.google.common.io.Closeables;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.index.LuceneFields;
import com.zimbra.cs.index.LuceneQueryOperation;
import com.zimbra.cs.index.NoTermQueryOperation;
import com.zimbra.cs.index.QueryOperation;
import com.zimbra.cs.index.analysis.AddrCharTokenizer;
import com.zimbra.cs.index.analysis.ContactTokenFilter;
import com.zimbra.cs.mailbox.Mailbox;

/**
 * Special text query to search contacts.
 *
 * @author ysasaki
 */
public final class ContactQuery extends Query {
    private final List<String> tokens = new ArrayList<String>();

    public ContactQuery(String text) {
        TokenStream stream = new ContactTokenFilter(new AddrCharTokenizer(new StringReader(text)));
        CharTermAttribute termAttr = stream.addAttribute(CharTermAttribute.class);
        try {
            stream.reset();
            while (stream.incrementToken()) {
                tokens.add(CharMatcher.is('*').trimTrailingFrom(termAttr)); // remove trailing wildcard characters
            }
            stream.end();
            stream.close();
        } catch (IOException e) { // should never happen
            ZimbraLog.search.error("Failed to tokenize text=%s", text);
        }
    }

    @Override
    public boolean hasTextOperation() {
        return true;
    }

    @Override
    public QueryOperation compile(Mailbox mbox, boolean bool) throws ServiceException {
        if (tokens.isEmpty()) {
            return new NoTermQueryOperation();
        }
        LuceneQueryOperation op = new LuceneQueryOperation();
        if (tokens.size() == 1) {
            PrefixQuery query = new PrefixQuery(new Term(LuceneFields.L_CONTACT_DATA, tokens.get(0)));
            op.addClause("contact:" +  tokens.get(0), query, evalBool(bool));
        } else {
            MultiPhraseQuery query = new MultiPhraseQuery();
            int max = mbox.index.getMaxWildcardTerms();
            IndexSearcher searcher = null;
            try {
                searcher = mbox.index.getIndexStore().openSearcher();
                for (String token : tokens) {
                    TermEnum itr = searcher.getIndexReader().terms(new Term(LuceneFields.L_CONTACT_DATA, token));
                    List<Term> terms = new ArrayList<Term>();
                    do {
                        Term term = itr.term();
                        if (term != null && term.field().equals(LuceneFields.L_CONTACT_DATA) &&
                                term.text().startsWith(token)) {
                            terms.add(term);
                            if (terms.size() >= max) { // too many terms expanded
                                break;
                            }
                        } else {
                            break;
                        }
                    } while (itr.next());
                    itr.close();
                    if (terms.isEmpty()) {
                        return new NoTermQueryOperation();
                    }
                    query.add(terms.toArray(new Term[terms.size()]));
                }
                op.addClause("contact:\"" + Joiner.on(' ').join(tokens) + "\"", query, evalBool(bool));
            } catch (IOException e) {
                throw ServiceException.FAILURE("Failed to expand wildcard", e);
            } finally {
                Closeables.closeQuietly(searcher);
            }
        }
        return op;
    }

    @Override
    void dump(StringBuilder out) {
        out.append("CONTACT:");
        Joiner.on(',').appendTo(out, tokens);
    }

}
