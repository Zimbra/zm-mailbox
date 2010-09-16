/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010 Zimbra, Inc.
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
import java.util.LinkedList;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.BooleanClause.Occur;

import com.google.common.base.Preconditions;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.index.LuceneFields;
import com.zimbra.cs.index.LuceneQueryOperation;
import com.zimbra.cs.index.MailboxIndex;
import com.zimbra.cs.index.NoTermQueryOperation;
import com.zimbra.cs.index.QueryInfo;
import com.zimbra.cs.index.QueryOperation;
import com.zimbra.cs.index.WildcardExpansionQueryInfo;
import com.zimbra.cs.mailbox.Mailbox;

/**
 * Query by text.
 *
 * @author tim
 * @author ysasaki
 */
public final class TextQuery extends Query {
    private List<String> mTokens;
    private int mSlop;  // sloppiness for PhraseQuery
    private final String field;
    private final String term;
    private List<String> mOredTokens;
    private String mWildcardTerm;
    private List<QueryInfo> mQueryInfo = new ArrayList<QueryInfo>();
    private Mailbox mMailbox;

    private static final int MAX_WILDCARD_TERMS =
        LC.zimbra_index_wildcard_max_terms_expanded.intValue();

    /**
     * A single search term. If text has multiple words, it is treated as a
     * phrase (full exact match required) text may end in a *, which wildcards
     * the last term.
     */
    public TextQuery(Mailbox mbox, Analyzer analyzer, String field, String text)
        throws ServiceException {

        mMailbox = Preconditions.checkNotNull(mbox);
        this.field = field;
        this.term = text;
        mOredTokens = new LinkedList<String>();

        // The set of tokens from the user's query. The way the parser
        // works, the token set should generally only be one element.
        mTokens = new ArrayList<String>(1);
        mWildcardTerm = null;

        TokenStream stream = analyzer.tokenStream(field, new StringReader(text));
        try {
            TermAttribute termAttr = stream.addAttribute(
                    TermAttribute.class);
            while (stream.incrementToken()) {
                mTokens.add(termAttr.term());
            }
            stream.end();
            stream.close();
        } catch (IOException ignore) {
        }

        // okay, quite a bit of hackery here....basically, if they're doing a contact:
        // search AND they haven't manually specified a phrase query (expands to more than one term)
        // then lets hack their search and make it a * search.
        // for bug:17232 -- if the search string is ".", don't auto-wildcard it, because . is
        // supposed to match everything by default.
        if (LuceneFields.L_CONTACT_DATA.equals(field) &&
                mTokens.size() <= 1 && text.length() > 0 &&
                text.charAt(text.length() - 1) != '*' && !text.equals(".")) {
            text = text + '*';
        }

        // must look at original text here b/c analyzer strips *'s
        if (text.length() > 0 && text.charAt(text.length() - 1) == '*') {
            // wildcard query!
            String wcToken;

            // only the last token is allowed to have a wildcard in it
            if (mTokens.size() > 0) {
                wcToken = mTokens.remove(mTokens.size() - 1);
            } else {
                wcToken = text;
            }

            if (wcToken.charAt(wcToken.length() - 1) == '*') {
                wcToken = wcToken.substring(0, wcToken.length()-1);
            }

            if (wcToken.length() > 0) {
                mWildcardTerm = wcToken;
                MailboxIndex mbidx = mbox.getMailboxIndex();
                List<String> expandedTokens = new ArrayList<String>(100);
                boolean expandedAllTokens = false;
                if (mbidx != null) {
                    expandedAllTokens = mbidx.expandWildcardToken(
                            expandedTokens, field, wcToken, MAX_WILDCARD_TERMS);
                }

                mQueryInfo.add(new WildcardExpansionQueryInfo(wcToken + "*",
                        expandedTokens.size(), expandedAllTokens));
                //
                // By design, we interpret *zero* tokens to mean "ignore this search term"
                // therefore if the wildcard expands to no terms, we need to stick something
                // in right here, just so we don't get confused when we go to execute the
                // query later
                //
                if (expandedTokens.size() == 0 || !expandedAllTokens) {
                    mTokens.add(wcToken);
                } else {
                    for (String token : expandedTokens) {
                        mOredTokens.add(token);
                    }
                }
            }
        }
    }

    /**
     * Returns the Lucene field.
     *
     * @see LuceneFields
     * @return lucene field
     */
    public String getField() {
        return field;
    }

    @Override
    public QueryOperation getQueryOperation(boolean bool) {
        if (mTokens.size() <= 0 && mOredTokens.size() <= 0) {
            // if we have no tokens, that is usually because the analyzer removed them
            // -- the user probably queried for a stop word like "a" or "an" or "the"
            //
            // By design: interpret *zero* tokens to mean "ignore this search term"
            //
            // We can't simply skip this term in the generated parse tree -- we have to put a null
            // query into the query list, otherwise conjunctions will get confused...so
            // we pass NULL to addClause which will add a blank clause for us...
            return new NoTermQueryOperation();
        } else {
            // indexing is disabled
            if (mMailbox.getMailboxIndex() == null)
                return new NoTermQueryOperation();

            LuceneQueryOperation op = new LuceneQueryOperation();

            for (QueryInfo inf : mQueryInfo) {
                op.addQueryInfo(inf);
            }

            if (mTokens.size() == 0) {
                op.setQueryString(toQueryString(field, term));
            } else if (mTokens.size() == 1) {
                TermQuery query = new TermQuery(new Term(field, mTokens.get(0)));
                op.addClause(toQueryString(field, term), query, evalBool(bool));
            } else if (mTokens.size() > 1) {
                PhraseQuery phrase = new PhraseQuery();
                phrase.setSlop(mSlop); // TODO configurable?
                for (String token : mTokens) {
                    phrase.add(new Term(field, token));
                }
                op.addClause(toQueryString(field, term), phrase, evalBool(bool));
            }

            if (mOredTokens.size() > 0) {
                // probably don't need to do this here...can probably just call addClause
                BooleanQuery orQuery = new BooleanQuery();
                for (String token : mOredTokens) {
                    orQuery.add(new TermQuery(new Term(field, token)), Occur.SHOULD);
                }
                op.addClause("", orQuery, evalBool(bool));
            }

            return op;
        }
    }

    @Override
    public void dump(StringBuilder out) {
        out.append(field);
        for (String token : mTokens) {
            out.append(',');
            out.append(token);
        }
        if (mWildcardTerm != null) {
            out.append(" WILDCARD=");
            out.append(mWildcardTerm);
            out.append(" [");
            out.append(mOredTokens.size());
            out.append(" terms]");
        }
    }
}
