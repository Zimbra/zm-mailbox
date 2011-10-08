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
package com.zimbra.cs.index.query;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.TermQuery;

import com.google.common.base.CharMatcher;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.index.LuceneFields;
import com.zimbra.cs.index.LuceneQueryOperation;
import com.zimbra.cs.index.NoTermQueryOperation;
import com.zimbra.cs.index.QueryOperation;
import com.zimbra.cs.mailbox.Mailbox;

/**
 * Query by text.
 *
 * @author tim
 * @author ysasaki
 */
public class TextQuery extends Query {
    private final List<String> tokens = Lists.newArrayList();
    private final String field;
    private final String text;
    private boolean quick = false;

    /**
     * A single search term. If text has multiple words, it is treated as a phrase (full exact match required) text may
     * end in a *, which wildcards the last term.
     */
    public TextQuery(Analyzer analyzer, String field, String text) {
        this(analyzer.tokenStream(field, new StringReader(text)), field, text);
    }

    TextQuery(TokenStream stream, String field, String text) {
        this.field = field;
        this.text = text;

        try {
            CharTermAttribute termAttr = stream.addAttribute(CharTermAttribute.class);
            stream.reset();
            while (stream.incrementToken()) {
                tokens.add(termAttr.toString());
            }
            stream.end();
            stream.close();
        } catch (IOException e) { // should never happen
            ZimbraLog.search.error("Failed to tokenize text=%s", text);
        }
    }

    /**
     * Enables quick search.
     * <p>
     * Makes this a wildcard query and gives a query suggestion by auto-completing the last term with the top term,
     * which is the most frequent term among the wildcard-expanded terms.
     *
     * TODO: The current query suggestion implementation can't auto-complete a phrase query as a phrase. It simply
     * auto-completes the last term as if it's a single term query.
     */
    public void setQuick(boolean value) {
        quick = value;
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
    public boolean hasTextOperation() {
        return true;
    }

    @Override
    public QueryOperation compile(Mailbox mbox, boolean bool) throws ServiceException {
        if (quick || text.endsWith("*")) { // wildcard, must look at original text here b/c analyzer strips *'s
            // only the last token is allowed to have a wildcard in it
            String last = tokens.isEmpty() ? text : tokens.remove(tokens.size() - 1);
            LuceneQueryOperation.LazyMultiPhraseQuery query = new LuceneQueryOperation.LazyMultiPhraseQuery();
            for (String token : tokens) {
                query.add(new Term(field, token));
            }
            query.expand(new Term(field, CharMatcher.is('*').trimTrailingFrom(last))); // expand later

            LuceneQueryOperation op = new LuceneQueryOperation();
            op.addClause(toQueryString(field, text), query, evalBool(bool));
            return op;
        } else if (tokens.isEmpty()) {
            // if we have no tokens, that is usually because the analyzer removed them. The user probably queried for
            // a stop word like "a" or "an" or "the".
            return new NoTermQueryOperation();
        } else if (tokens.size() == 1) {
            LuceneQueryOperation op = new LuceneQueryOperation();
            op.addClause(toQueryString(field, text), new TermQuery(new Term(field, tokens.get(0))), evalBool(bool));
            return op;
        } else {
            assert tokens.size() > 1 : tokens.size();
            PhraseQuery query = new PhraseQuery();
            for (String token : tokens) {
                query.add(new Term(field, token));
            }
            LuceneQueryOperation op = new LuceneQueryOperation();
            op.addClause(toQueryString(field, text), query, evalBool(bool));
            return op;
        }
    }

    @Override
    public void dump(StringBuilder out) {
        out.append(field);
        out.append(':');
        Joiner.on(',').appendTo(out, tokens);
        if (quick || text.endsWith("*")) {
            out.append("[*]");
        }
    }

}
