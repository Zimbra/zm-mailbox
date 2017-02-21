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

import com.zimbra.cs.index.QueryOperation;
import com.zimbra.cs.mailbox.Mailbox;

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
    public QueryOperation compile(Mailbox mbox, boolean bool) {
        assert false;
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasTextOperation() {
        return false;
    }

}
