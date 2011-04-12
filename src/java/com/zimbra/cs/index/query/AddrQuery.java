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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;

import com.zimbra.cs.index.LuceneFields;

/**
 * A simpler way of expressing (to:FOO or from:FOO or cc:FOO).
 *
 * @author tim
 * @author ysasaki
 */
public final class AddrQuery extends SubQuery {

    public enum Address {
        FROM, TO, CC
    }

    private AddrQuery(List<Query> clauses) {
        super(clauses);
    }

    @Override
    public boolean hasTextOperation() {
        return true;
    }

    public static AddrQuery create(Analyzer analyzer, Set<Address> addrs, String text) {
        List<Query> clauses = new ArrayList<Query>();

        if (addrs.contains(Address.FROM)) {
            clauses.add(new TextQuery(analyzer, LuceneFields.L_H_FROM, text));
        }

        if (addrs.contains(Address.TO)) {
            if (!clauses.isEmpty()) {
                clauses.add(new ConjQuery(ConjQuery.Conjunction.OR));
            }
            clauses.add(new TextQuery(analyzer, LuceneFields.L_H_TO, text));
        }

        if (addrs.contains(Address.CC)) {
            if (!clauses.isEmpty()) {
                clauses.add(new ConjQuery(ConjQuery.Conjunction.OR));
            }
            clauses.add(new TextQuery(analyzer, LuceneFields.L_H_CC, text));
        }

        return new AddrQuery(clauses);
    }
}
