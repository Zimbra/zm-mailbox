/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2015, 2016, 2017 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.index.query;

import java.util.LinkedList;
import java.util.List;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.TermQuery;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.index.LuceneFields;
import com.zimbra.cs.index.LuceneQueryOperation;
import com.zimbra.cs.index.NoTermQueryOperation;
import com.zimbra.cs.index.QueryOperation;
import com.zimbra.cs.index.solr.SolrUtils;
import com.zimbra.cs.mailbox.Mailbox;

/**
 * Special text query to search contacts.
 *
 * @author ysasaki
 */
public final class ContactQuery extends Query {
    private final String originalQueryString;
    private final String queryString;
    private final boolean isPhraseQuery;
    private boolean isWildcardQuery;

    public ContactQuery(String queryString) {
        this(queryString, false);
    }

    public ContactQuery(String queryString, boolean isPhraseQuery) {
        this.originalQueryString = queryString;
        this.queryString = rewriteQueryString(originalQueryString);
        this.isPhraseQuery = isPhraseQuery;
    }

    private String rewriteQueryString(String queryString) {
        List<String> tokens = new LinkedList<String>();
        boolean hasWildcardToken = false;
        for (String token: queryString.split("\\s")) {
            if (SolrUtils.shouldSearchEdgeNgrams(token)) {
                token = token.replace("*", ""); // in case a wildcard was part of the query
                if (token.length() > 0) {
                    tokens.add(token); // no need for wildcard, will match edge n-grams
                }
            } else {
                //  "keyword"  -->  "keyword*"
                // "*keyword*" -->  "keyword*"
                // "*keyword"  --> "*keyword"
                //  "keyword*" -->  "keyword*"
                if (!token.startsWith("*") && !token.endsWith("*")) {
                    tokens.add(token + "*");
                } else if (token.startsWith("*") && token.endsWith("*")) {
                    tokens.add(token.substring(1, token.length()));
                } else {
                    tokens.add(token);
                }
                hasWildcardToken = true;
            }
        }
        isWildcardQuery = hasWildcardToken;
        return Joiner.on(" ").join(tokens);
    }

    @Override
    public boolean hasTextOperation() {
        return true;
    }

    @Override
    public QueryOperation compile(Mailbox mbox, boolean bool) throws ServiceException {
        if (queryString.length() == 0) {
            return new NoTermQueryOperation();
        }
        LuceneQueryOperation op = new LuceneQueryOperation();
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        /*
         * To get efficient prefix query behavior, we use edge n-gram tokenized fields here,
         * suffixed with "_ngrams" in the solr schema.
         */
        String ngramContactField = SolrUtils.getNgramFieldName(LuceneFields.L_CONTACT_DATA);
        String ngramToField = SolrUtils.getNgramFieldName(LuceneFields.L_H_TO);
        org.apache.lucene.search.Query query;
        if (isWildcardQuery) {
            query = new ZimbraWildcardQuery(queryString, ngramContactField, ngramToField);
        } else {
            org.apache.lucene.search.Query contactFieldClause;
            org.apache.lucene.search.Query toFieldClause;
            if (isPhraseQuery) {
                contactFieldClause = new PhraseQuery(ngramContactField, queryString);
                toFieldClause = new PhraseQuery(ngramToField, queryString);
            } else {
                contactFieldClause = new TermQuery(new Term(ngramContactField, queryString));
                toFieldClause = new TermQuery(new Term(ngramToField, queryString));
            }
            builder.add(contactFieldClause, Occur.SHOULD);
            builder.add(toFieldClause, Occur.SHOULD);
            query = builder.build();
        }
        String contactFieldSearchClause = toQueryString(LuceneFields.L_CONTACT_DATA, originalQueryString);
        op.addClause(contactFieldSearchClause, query, evalBool(bool));
        return op;
    }

    @Override
    void dump(StringBuilder out) {
        out.append("CONTACT:").append(originalQueryString);
    }

    @Override
    void sanitizedDump(StringBuilder out) {
        int numWordsInQuery = originalQueryString.split("\\s").length;
        out.append("CONTACT:").append(originalQueryString);
        out.append(":");
        out.append(Strings.repeat("$TEXT,", numWordsInQuery));
        if (out.charAt(out.length()-1) == ',') {
            out.deleteCharAt(out.length()-1);
        }
    }
}
