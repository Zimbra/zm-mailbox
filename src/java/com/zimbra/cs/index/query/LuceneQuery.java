/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011, 2013 Zimbra Software, LLC.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.4 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.index.query;

import java.util.Collection;
import java.util.Map;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.zimbra.cs.index.LuceneQueryOperation;
import com.zimbra.cs.index.QueryOperation;
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

    LuceneQuery(String queryField, String luceneField, String term) {
        this.queryField = queryField;
        this.luceneField = luceneField;
        this.term = term;
    }

    @Override
    public boolean hasTextOperation() {
        return true;
    }

    @Override
    public QueryOperation compile(Mailbox mbox, boolean bool) {
        LuceneQueryOperation op = new LuceneQueryOperation();
        op.addClause(queryField + term, new TermQuery(new Term(luceneField, term)), evalBool(bool));
        return op;
    }

    @Override
    public void dump(StringBuilder out) {
        out.append(luceneField);
        out.append(':');
        out.append(term);
    }

}
