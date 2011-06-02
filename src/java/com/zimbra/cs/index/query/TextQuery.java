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
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MultiPhraseQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.TermQuery;

import com.google.common.base.Joiner;
import com.google.common.io.Closeables;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.index.LuceneFields;
import com.zimbra.cs.index.LuceneQueryOperation;
import com.zimbra.cs.index.NoResultsQueryOperation;
import com.zimbra.cs.index.NoTermQueryOperation;
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
    private final List<String> tokens = new ArrayList<String>();
    private final String field;
    private final String text;

    /**
     * A single search term. If text has multiple words, it is treated as a phrase (full exact match required) text may
     * end in a *, which wildcards the last term.
     */
    public TextQuery(Analyzer analyzer, String field, String text) {
        this(analyzer.tokenStream(field, new StringReader(text)), field, text);
    }

    protected TextQuery(TokenStream stream, String field, String text) {
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
        if (text.endsWith("*")) { // wildcard, must look at original text here b/c analyzer strips *'s
            // only the last token is allowed to have a wildcard in it
            String prefix = tokens.isEmpty() ? text : tokens.remove(tokens.size() - 1);
            prefix = prefix.replace("*", "");

            int max = mbox.index.getMaxWildcardTerms();
            List<Term> terms = new ArrayList<Term>();
            boolean overflow = false;
            IndexSearcher searcher = null;
            try {
                searcher = mbox.index.getIndexStore().openSearcher();
                TermEnum itr = searcher.getIndexReader().terms(new Term(field, prefix));
                do {
                    Term term = itr.term();
                    if (term != null && term.field().equals(field) && term.text().startsWith(prefix)) {
                        if (terms.size() >= max) {
                            overflow = true;
                            break;
                        }
                        terms.add(term);
                    } else {
                        break;
                    }
                } while (itr.next());
                itr.close();
            } catch (IOException e) {
                throw ServiceException.FAILURE("Failed to expand wildcard", e);
            } finally {
                Closeables.closeQuietly(searcher);
            }

            if (terms.isEmpty()) {
                return new NoResultsQueryOperation();
            } else {
                MultiPhraseQuery query = new MultiPhraseQuery();
                for (String token : tokens) {
                    query.add(new Term(field, token));
                }
                query.add(terms.toArray(new Term[terms.size()]));
                LuceneQueryOperation op = new LuceneQueryOperation();
                op.addQueryInfo(new WildcardExpansionQueryInfo(prefix + "*", terms.size(), !overflow));
                op.addClause(toQueryString(field, text), query, evalBool(bool));
                return op;
            }
        } else if (tokens.isEmpty()) {
            // if we have no tokens, that is usually because the analyzer removed them. The user probably queried for
            // a stop word like "a" or "an" or "the".
            return new NoTermQueryOperation();
        } else if (tokens.size() == 1) {
            String token = tokens.get(0);
            LuceneQueryOperation op = new LuceneQueryOperation();
            op.addClause(toQueryString(field, text), new TermQuery(new Term(field, token)), evalBool(bool));
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
        if (text.endsWith("*")) {
            out.append("[*]");
        }
    }
}
