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
import org.apache.lucene.search.TermQuery;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
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
    private final String text;
    private final String withWildcard;
    private boolean contactsSearchInfixQuerySupported;
    private boolean searchEdgeNgramFields;

	public ContactQuery(String text) {
	    contactsSearchInfixQuerySupported = true;
//	try {
//		contactsSearchInfixQuerySupported = Provisioning.getInstance().getConfig().isContactsSearchInfixQuerySupported();
//	} catch (ServiceException e) {
//		contactsSearchInfixQuerySupported = true;
//	}
    	this.text = text;
        this.searchEdgeNgramFields = shouldSearchEdgeNgrams(text);
        if (!searchEdgeNgramFields) {
            this.withWildcard = buildWildcardQuery(text);
        } else {
            this.withWildcard = null;
        }
    }

	private boolean shouldSearchEdgeNgrams(String text) {
	    return text.replace("*","").length() < LC.contact_search_min_chars_for_wildcard_query.intValue();
	}

	private String buildWildcardQuery(String text) {
		List<String> tokensWithWildcards = new LinkedList<String>();
		String[] tokens = text.split("\\s");
		for (int i = 0; i < tokens.length; i++) {
			String token = tokens[i];
			if (contactsSearchInfixQuerySupported) {
				//  "keyword"  --> "*keyword*"
				// "*keyword*" --> "*keyword*"
				// "*keyword"  --> "*keyword"
				//  "keyword*" -->  "keyword*"
				if (!token.startsWith("*") && !token.endsWith("*")) {
					tokensWithWildcards.add("*" + token + "*");
				} else {
					tokensWithWildcards.add(token);
				}
			} else {
				//  "keyword"  -->  "keyword*"
				// "*keyword*" -->  "keyword*"
				// "*keyword"  --> "*keyword"
				//  "keyword*" -->  "keyword*"
				if (!token.startsWith("*") && !token.endsWith("*")) {
					tokensWithWildcards.add(token + "*");
				} else if (token.startsWith("*") && token.endsWith("*")) {
					tokensWithWildcards.add(token.substring(1, token.length()));
				} else {
					tokensWithWildcards.add(token);
				}
			}
		}
		return Joiner.on(" ").join(tokensWithWildcards);
	}

	@Override
    public boolean hasTextOperation() {
        return true;
    }

    @Override
    public QueryOperation compile(Mailbox mbox, boolean bool) throws ServiceException {
    	if (text.length() == 0) {
    		return new NoTermQueryOperation();
    	}
        LuceneQueryOperation op = new LuceneQueryOperation();
        String contactFieldSearchClause = toQueryString(LuceneFields.L_CONTACT_DATA, withWildcard);
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        TermQuery contactFieldClause;
        TermQuery toFieldClause;
        if (searchEdgeNgramFields) {
            String ngramContactField = SolrUtils.getNgramFieldName(LuceneFields.L_CONTACT_DATA);
            String ngramToField = SolrUtils.getNgramFieldName(LuceneFields.L_H_TO);
            contactFieldClause = new TermQuery(new Term(ngramContactField, text));
            toFieldClause = new TermQuery(new Term(ngramToField, text));
        } else {
            contactFieldClause = new TermQuery(new Term(LuceneFields.L_CONTACT_DATA, withWildcard));
            String searchableToField = SolrUtils.getSearchFieldName(LuceneFields.L_H_TO);
            toFieldClause = new TermQuery(new Term(searchableToField, withWildcard));
        }
        builder.add(contactFieldClause, Occur.SHOULD);
        builder.add(toFieldClause, Occur.SHOULD);
        BooleanQuery query = builder.build();
        ZimbraLog.search.info("contact search for %s compiles to %s", text, query.toString());
        op.addClause(contactFieldSearchClause, query, evalBool(bool));
        return op;
    }

    @Override
    void dump(StringBuilder out) {
        out.append("CONTACT:").append(text);
    }

    @Override
    void sanitizedDump(StringBuilder out) {
    	int numWordsInQuery = text.split("\\s").length;
        out.append("CONTACT:").append(text);
    	out.append(":");
        out.append(Strings.repeat("$TEXT,", numWordsInQuery));
        if (out.charAt(out.length()-1) == ',') {
            out.deleteCharAt(out.length()-1);
        }
    }
}
