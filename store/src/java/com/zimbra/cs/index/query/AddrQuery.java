/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011, 2013, 2014, 2016, 2017 Synacor, Inc.
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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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

    public static AddrQuery create(Set<Address> addrs, String text) {
        return create(addrs, text, false);
    }

    public static AddrQuery create(Set<Address> addrs, String text, boolean isPhraseQuery) {
        List<Query> clauses = new ArrayList<Query>();

        if (addrs.contains(Address.FROM)) {
            clauses.add(new TextQuery(LuceneFields.L_H_FROM, text, isPhraseQuery));
        }

        if (addrs.contains(Address.TO)) {
            if (!clauses.isEmpty()) {
                clauses.add(new ConjQuery(ConjQuery.Conjunction.OR));
            }
            clauses.add(new TextQuery(LuceneFields.L_H_TO, text, isPhraseQuery));
        }

        if (addrs.contains(Address.CC)) {
            if (!clauses.isEmpty()) {
                clauses.add(new ConjQuery(ConjQuery.Conjunction.OR));
            }
            clauses.add(new TextQuery(LuceneFields.L_H_CC, text, isPhraseQuery));
        }

        return new AddrQuery(clauses);
    }
}
