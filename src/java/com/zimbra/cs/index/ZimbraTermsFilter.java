/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2013 Zimbra Software, LLC.
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
package com.zimbra.cs.index;

import java.util.Collection;
import java.util.Set;

import org.apache.lucene.index.Term;
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
}
