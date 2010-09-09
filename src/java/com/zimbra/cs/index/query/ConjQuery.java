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

import com.zimbra.cs.index.QueryOperation;
import com.zimbra.cs.index.query.parser.QueryParser;

/**
 * Special query that combine queries.
 *
 * @author tim
 * @author ysasaki
 */
public final class ConjQuery extends Query {
    static final int AND = QueryParser.AND;
    static final int OR = QueryParser.OR;

    public ConjQuery(int qType) {
        super(0, qType);
    }

    public boolean isOr() {
        return getQueryType() == OR;
    }

    @Override
    public StringBuilder dump(StringBuilder out) {
        switch (getQueryType()) {
            case AND:
                return out.append(" && ");
            case OR:
                return out.append(" || ");
            default:
                assert(false);
                return out;
        }
    }

    @Override
    public QueryOperation getQueryOperation(boolean truth) {
        assert(false);
        return null;
    }

}
