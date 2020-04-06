/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.index.query;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.zimbra.cs.index.LuceneQueryOperation;
import com.zimbra.cs.index.QueryOperation;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;

/**
 * Query by Lucene field.
 *
 * @author tim
 * @author ysasaki
 */
abstract class LuceneQuery extends Query {
    private final String luceneField;
    private final String queryField;
    private final Set<MailItem.Type> types;
    String term;

    static String lookup(Map<String, String> map, String key) {
        String toRet = map.get(key);
        if (toRet == null) {
            return key;
        } else {
            return toRet;
        }
    }

    public static Collection<String> lookup(Multimap<String, String> multimap, String what) {
        Collection<String> types = multimap.get(what);
        if (types.isEmpty()) {
            types = ImmutableList.of(what); // Need new collection as original types is probably immutable
        }
        return types;
    }

    LuceneQuery(String queryField, String luceneField, String term, Set<MailItem.Type> types) {
        this.queryField = queryField;
        this.luceneField = luceneField;
        this.term = term;
        this.types = types;
    }

    @Override
    public boolean hasTextOperation() {
        return true;
    }

    @Override
    public QueryOperation compile(Mailbox mbox, boolean bool) {
        LuceneQueryOperation op = new LuceneQueryOperation();
        op.addClause(queryField + term, new TermQuery(new Term(luceneField, term)), evalBool(bool), getIndexTypes(types));
        return op;
    }

    @Override
    public void dump(StringBuilder out) {
        out.append(luceneField);
        out.append(':');
        out.append(term);
    }

}
