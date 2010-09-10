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

import java.util.Map;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;

import com.zimbra.cs.index.QueryOperation;
import com.zimbra.cs.index.TextQueryOperation;
import com.zimbra.cs.mailbox.Mailbox;

/**
 * Query by Lucene field.
 *
 * @author tim
 * @author ysasaki
 */
abstract class LuceneQuery extends Query {
    private Mailbox mMailbox;
    private String mLuceneField;
    private String mValue;

    static void addMapping(Map<String, String> map, String[] array, String value) {
        for (int i = array.length - 1; i >= 0; i--) {
            map.put(array[i], value);
        }
    }

    static String lookup(Map<String, String> map, String key) {
        String toRet = map.get(key);
        if (toRet == null) {
            return key;
        } else {
            return toRet;
        }
    }

    LuceneQuery(Mailbox mbox, int target, String luceneField, String value) {
        super(target);
        mMailbox = mbox;
        mLuceneField = luceneField;
        mValue = value;
    }

    @Override
    public QueryOperation getQueryOperation(boolean truth) {
        TextQueryOperation op = mMailbox.getMailboxIndex().createTextQueryOperation();

        op.addClause(toQueryString(mValue),
                new TermQuery(new Term(mLuceneField, mValue)), calcTruth(truth));

        return op;
    }

    @Override
    public StringBuilder dump(StringBuilder out) {
        super.dump(out);
        out.append(',');
        out.append(mLuceneField);
        out.append(':');
        out.append(mValue);
        return out.append(')');
    }

}
