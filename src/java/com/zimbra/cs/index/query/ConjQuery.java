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

/**
 * Special query that combine queries.
 *
 * @author tim
 * @author ysasaki
 */
public final class ConjQuery extends Query {

    public enum Conjunction {
        AND("&&"), OR("||");

        private final String symbol;

        private Conjunction(String symbol) {
            this.symbol = symbol;
        }

        @Override
        public String toString() {
            return symbol;
        }
    }

    private final Conjunction conjunction;

    public ConjQuery(Conjunction conj) {
        conjunction = conj;
    }

    public Conjunction getConjunction() {
        return conjunction;
    }

    @Override
    public StringBuilder toString(StringBuilder out) {
        out.append(' ');
        dump(out);
        return out.append(' ');
    }

    @Override
    public void dump(StringBuilder out) {
        out.append(conjunction);
    }

    @Override
    public QueryOperation getQueryOperation(boolean truth) {
        assert(false);
        return null;
    }

}
