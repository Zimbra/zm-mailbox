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

import java.util.List;

import com.zimbra.cs.index.QueryOperation;

/**
 * Special query that wraps sub queries.
 *
 * @author tim
 * @author ysasaki
 */
public class SubQuery extends Query {

    private final List<Query> clauses;

    public SubQuery(List<Query> clauses) {
        this.clauses = clauses;
    }

    public List<Query> getSubClauses() {
        return clauses;
    }

    @Override
    public QueryOperation getQueryOperation(boolean truth) {
        assert(false);
        return null;
    }

    @Override
    public StringBuilder toString(StringBuilder out) {
        out.append(getModifier());
        out.append('(');
        dump(out);
        return out.append(')');
    }

    @Override
    public void dump(StringBuilder out) {
        for (Query sub : clauses) {
            sub.toString(out);
        }
    }

}
