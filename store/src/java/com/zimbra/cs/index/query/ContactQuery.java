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

import com.google.common.base.Strings;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.index.LuceneFields;
import com.zimbra.cs.index.LuceneQueryOperation;
import com.zimbra.cs.index.NoTermQueryOperation;
import com.zimbra.cs.index.QueryOperation;
import com.zimbra.cs.index.solr.SolrUtils;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxIndex.IndexType;

/**
 * Special text query to search contacts.
 *
 * @author ysasaki
 */
public final class ContactQuery extends Query {
    private final String queryString;

    public ContactQuery(String queryString) {
        this(queryString, false);
    }

    public ContactQuery(String queryString, boolean isPhraseQuery) {
        this.queryString = queryString;
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
        /*
         * To get efficient prefix query behavior, we use edge n-gram tokenized fields here,
         * suffixed with "_ngrams" in the solr schema.
         * The zimbra wildcard query parser is passed 'expandAll' flag so that
         * every token is treated as prefix match (pre zimbra-x, this would happen locally).
         * The 'maxNgramSize' parameter makes the query parser aware of when to actually use
         * a wildcard for prefix matches.
         */
        String ngramContactField = SolrUtils.getNgramFieldName(LuceneFields.L_CONTACT_DATA);
        String ngramToField = SolrUtils.getNgramFieldName(LuceneFields.L_H_TO);
        ZimbraWildcardQuery query = new ZimbraWildcardQuery(queryString, ngramContactField, ngramToField);
        query.setExpandAll(true);
        int maxNgramSize = LC.contact_search_min_chars_for_wildcard_query.intValue() - 1;
        query.setMaxNgramSize(maxNgramSize);
        String contactFieldSearchClause = toQueryString(LuceneFields.L_CONTACT_DATA, queryString);
        op.addClause(contactFieldSearchClause, query, evalBool(bool), IndexType.CONTACTS);
        return op;
    }

    @Override
    void dump(StringBuilder out) {
        out.append("CONTACT:").append(queryString);
    }

    @Override
    void sanitizedDump(StringBuilder out) {
        int numWordsInQuery = queryString.split("\\s").length;
        out.append("CONTACT:").append(queryString);
        out.append(":");
        out.append(Strings.repeat("$TEXT,", numWordsInQuery));
        if (out.charAt(out.length()-1) == ',') {
            out.deleteCharAt(out.length()-1);
        }
    }
}
