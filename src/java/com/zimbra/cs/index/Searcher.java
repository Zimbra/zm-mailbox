/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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
package com.zimbra.cs.index;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;

/**
 * Abstraction of index read operations.
 *
 * @see IndexStore#openSearcher()
 * @author ysasaki
 */
public interface Searcher extends Closeable {

    /**
     * Finds the hits up to the max for query, applying filter if non-null.
     *
     * @param query the query to match documents
     * @param filter if non-null, used to permit documents to be collected
     * @param sort sort order
     * @param max max hits
     * @return documents
     */
    List<Document> search(Query query, Filter filter, Sort sort, int max) throws IOException;

    /**
     * Returns an enumeration of all terms starting at a given term. If the given term does not exist, the enumeration
     * is positioned at the first term greater than the supplied term. The enumeration is ordered by
     * {@link Term#compareTo(Term)}. Each term is greater than all that precede it in the enumeration.
     */
    TermEnum getTerms(Term term) throws IOException;

    /**
     * Returns the number of documents containing the term.
     */
    int getCount(Term term) throws IOException;

    /**
     * Returns the total number of documents.
     */
    int getTotal() throws IOException;

}
