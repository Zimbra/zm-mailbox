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
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
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
import com.zimbra.cs.index.AllQueryOperation;
import com.zimbra.cs.index.LuceneFields;
import com.zimbra.cs.index.LuceneQueryOperation;
import com.zimbra.cs.index.NoResultsQueryOperation;
import com.zimbra.cs.index.NoTermQueryOperation;
import com.zimbra.cs.index.QueryOperation;
import com.zimbra.cs.index.SuggestQueryInfo;
import com.zimbra.cs.index.WildcardExpansionQueryInfo;
import com.zimbra.cs.mailbox.Mailbox;

/**
 * Query by text.
 *
 * @author tim
 * @author ysasaki
 */
public class TextQuery extends Query {
    private final List<Token> tokens = new ArrayList<Token>();
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
            OffsetAttribute offsetAttr = stream.addAttribute(OffsetAttribute.class);
            stream.reset();
            while (stream.incrementToken()) {
                tokens.add(new Token(termAttr.toString(), offsetAttr.startOffset()));
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
            Token last = tokens.isEmpty() ? new Token(text, 0) : tokens.remove(tokens.size() - 1);
            String prefix = last.term.replace("*", "");

            int max = mbox.index.getMaxWildcardTerms();
            List<Term> terms = new ArrayList<Term>();
            boolean overflow = false;
            IndexSearcher searcher = null;
            TopTerm top = new TopTerm();
            try {
                searcher = mbox.index.getIndexStore().openSearcher();
                TermEnum tenum = searcher.getIndexReader().terms(new Term(field, prefix));
                do {
                    Term term = tenum.term();
                    if (term != null && term.field().equals(field) && term.text().startsWith(prefix)) {
                        if (terms.size() >= max) {
                            overflow = true;
                            break;
                        }
                        terms.add(term);
                        top.update(tenum);
                    } else {
                        break;
                    }
                } while (tenum.next());
                tenum.close();
            } catch (IOException e) {
                throw ServiceException.FAILURE("Failed to expand wildcard", e);
            } finally {
                Closeables.closeQuietly(searcher);
            }

            if (terms.isEmpty()) {
                return bool ? new NoResultsQueryOperation() : new AllQueryOperation();
            } else {
                MultiPhraseQuery query = new MultiPhraseQuery();
                for (Token token : tokens) {
                    query.add(new Term(field, token.term));
                }
                query.add(terms.toArray(new Term[terms.size()]));
                LuceneQueryOperation op = new LuceneQueryOperation();
                op.addQueryInfo(new WildcardExpansionQueryInfo(prefix + '*', terms.size(), !overflow));
                if (quick) {
                    op.addQueryInfo(new SuggestQueryInfo(text.substring(0, last.start) + top.getTerm().text()));
                }
                op.addClause(toQueryString(field, text), query, evalBool(bool));
                return op;
            }
        } else if (tokens.isEmpty()) {
            // if we have no tokens, that is usually because the analyzer removed them. The user probably queried for
            // a stop word like "a" or "an" or "the".
            return new NoTermQueryOperation();
        } else if (tokens.size() == 1) {
            LuceneQueryOperation op = new LuceneQueryOperation();
            op.addClause(toQueryString(field, text),
                    new TermQuery(new Term(field, tokens.get(0).term)), evalBool(bool));
            return op;
        } else {
            assert tokens.size() > 1 : tokens.size();
            PhraseQuery query = new PhraseQuery();
            for (Token token : tokens) {
                query.add(new Term(field, token.term));
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

    private static final class Token {
        final String term;
        final int start;

        Token(String term, int start) {
            this.term = term;
            this.start = start;
        }

        @Override
        public String toString() {
            return term;
        }
    }

    private static final class TopTerm {
        private Term term;
        private int freq = 0;

        void update(TermEnum tenum) {
            if (freq < tenum.docFreq()) {
                freq = tenum.docFreq();
                term = tenum.term();
            }
        }

        Term getTerm() {
            return term;
        }
    }
}
