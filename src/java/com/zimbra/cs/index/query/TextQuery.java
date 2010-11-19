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
public class TextQuery extends Query {
    private List<String> tokens;
    private int slop;  // sloppiness for PhraseQuery
    private final String field;
    private final String term;
    private List<String> oredTokens;
    private String wildcardTerm;
    private List<QueryInfo> queryInfo = new ArrayList<QueryInfo>();
    private Mailbox mailbox;

    private static final int MAX_WILDCARD_TERMS =
        LC.zimbra_index_wildcard_max_terms_expanded.intValue();

    /**
     * A single search term. If text has multiple words, it is treated as a phrase (full exact match required) text may
     * end in a *, which wildcards the last term.
     */
    public TextQuery(Mailbox mbox, Analyzer analyzer, String field, String text) throws ServiceException {
        this(mbox, analyzer.tokenStream(field, new StringReader(text)), field, text);
    }

    protected TextQuery(Mailbox mbox, TokenStream stream, String field, String text) throws ServiceException {
        mailbox = Preconditions.checkNotNull(mbox);
        this.field = field;
        this.term = text;
        oredTokens = new LinkedList<String>();

        // The set of tokens from the user's query. The way the parser
        // works, the token set should generally only be one element.
        tokens = new ArrayList<String>(1);
        wildcardTerm = null;

        try {
            TermAttribute termAttr = stream.addAttribute(TermAttribute.class);
            while (stream.incrementToken()) {
                tokens.add(termAttr.term());
            }
            stream.end();
            stream.close();
        } catch (IOException ignore) {
        }

        // must look at original text here b/c analyzer strips *'s
        if (text.endsWith("*")) {
            // wildcard query!
            String wcToken;

            // only the last token is allowed to have a wildcard in it
            if (tokens.size() > 0) {
                wcToken = tokens.remove(tokens.size() - 1);
            } else {
                wcToken = text;
            }

            if (wcToken.endsWith("*")) {
                wcToken = wcToken.substring(0, wcToken.length() - 1);
            }

            if (wcToken.length() > 0) {
                wildcardTerm = wcToken;
                MailboxIndex mbidx = mbox.index.getMailboxIndex();
                List<String> expandedTokens = new ArrayList<String>(100);
                boolean expandedAllTokens = false;
                if (mbidx != null) {
                    try {
                        expandedAllTokens = mbidx.expandWildcardToken(
                                expandedTokens, field, wcToken, MAX_WILDCARD_TERMS);
                    } catch (IOException e) {
                        throw ServiceException.FAILURE("Failed to expand wildcard", e);
                    }
                }

                queryInfo.add(new WildcardExpansionQueryInfo(wcToken + "*",
                        expandedTokens.size(), expandedAllTokens));
                //
                // By design, we interpret *zero* tokens to mean "ignore this search term"
                // therefore if the wildcard expands to no terms, we need to stick something
                // in right here, just so we don't get confused when we go to execute the
                // query later
                //
                if (expandedTokens.size() == 0 || !expandedAllTokens) {
                    tokens.add(wcToken);
                } else {
                    for (String token : expandedTokens) {
                        oredTokens.add(token);
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
        if (tokens.size() <= 0 && oredTokens.size() <= 0) {
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
            if (mailbox.index.getMailboxIndex() == null)
                return new NoTermQueryOperation();

            LuceneQueryOperation op = new LuceneQueryOperation();

            for (QueryInfo inf : queryInfo) {
                op.addQueryInfo(inf);
            }

            if (tokens.size() == 0) {
                op.setQueryString(toQueryString(field, term));
            } else if (tokens.size() == 1) {
                TermQuery query = new TermQuery(new Term(field, tokens.get(0)));
                op.addClause(toQueryString(field, term), query, evalBool(bool));
            } else if (tokens.size() > 1) {
                PhraseQuery phrase = new PhraseQuery();
                phrase.setSlop(slop); // TODO configurable?
                for (String token : tokens) {
                    phrase.add(new Term(field, token));
                }
                op.addClause(toQueryString(field, term), phrase, evalBool(bool));
            }

            if (oredTokens.size() > 0) {
                // probably don't need to do this here...can probably just call addClause
                BooleanQuery orQuery = new BooleanQuery();
                for (String token : oredTokens) {
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
        for (String token : tokens) {
            out.append(',');
            out.append(token);
        }
        if (wildcardTerm != null) {
            out.append(" *=");
            out.append(wildcardTerm);
            out.append(" [");
            out.append(oredTokens.size());
            out.append(" terms]");
        }
    }
}
