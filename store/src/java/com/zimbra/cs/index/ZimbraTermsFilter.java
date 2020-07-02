/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.index;

import java.util.Collection;
import java.util.Set;

import org.apache.lucene.index.Term;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Sets;

/**
 * Constructs a filter for docs matching any of a set of terms.
 * This can be used for filtering on multiple terms that are not necessarily in a sequence.
 * An example might be a collection of primary keys from a database query result or perhaps
 * a choice of "category" labels picked by the end user.
 */
public class ZimbraTermsFilter {

    private final Set<Term> terms=Sets.newTreeSet();

    /**
     * @param terms is the list of acceptable terms
     */
    public ZimbraTermsFilter(Collection<Term> terms) {
        this.terms.addAll(terms);
    }

    public Collection<Term> getTerms() {
        return terms;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("terms", terms).toString();
    }
}
