/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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


/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.Filter;
import org.apache.lucene.util.OpenBitSet;

import com.google.common.collect.Sets;

/**
 * Constructs a filter for docs matching any of the terms added to this class.
 * Unlike a RangeFilter this can be used for filtering on multiple terms that are not necessarily in
 * a sequence. An example might be a collection of primary keys from a database query result or perhaps
 * a choice of "category" labels picked by the end user. As a filter, this is much faster than the
 * equivalent query (a BooleanQuery with many "should" TermQueries)
 *
 */
public class TermsFilter extends Filter
{
    private static final long serialVersionUID = 3705108370140268162L;
    private final Set<Term> terms=Sets.newTreeSet();

    /**
     * @param terms is the list of acceptable terms
     */
    public TermsFilter (Collection<Term> terms) {
        this.terms.addAll(terms);
    }

    /** (non-Javadoc)
     * @see org.apache.lucene.search.Filter#getDocIdSet(org.apache.lucene.index.IndexReader)
     */
    @Override
    public DocIdSet getDocIdSet(IndexReader reader) throws IOException {
    OpenBitSet result=new OpenBitSet(reader.maxDoc());
        TermDocs td = reader.termDocs();
        try {
            for (Iterator<Term> iter = terms.iterator(); iter.hasNext();) {
                Term term = iter.next();
                td.seek(term);
                while (td.next()) {
                    result.set(td.doc());
                }
            }
        } finally {
            try {
                td.close();
            } catch (Exception e) {
            }
        }
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj) {
            return true;
        }
        if((obj == null) || (obj.getClass() != this.getClass())) {
            return false;
        }
        TermsFilter test = (TermsFilter)obj;
        return (terms == test.terms || (terms != null && terms.equals(test.terms)));
    }

    @Override
    public int hashCode() {
        int hash=9;
        for (Iterator<Term> iter = terms.iterator(); iter.hasNext();) {
            Term term = iter.next();
            hash = 31 * hash + term.hashCode();
        }
        return hash;
    }
}
