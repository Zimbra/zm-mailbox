/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012 Zimbra, Inc.
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

import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopFieldDocs;

/**
 * Modeled on a subset of {@link org.apache.lucene.search.IndexSearcher}
 */
public interface ZimbraIndexSearcher extends Closeable {

    /**
     * Returns the stored fields of document docID.
     */
    Document doc(int docID) throws IOException;

    /**
     * Returns total docFreq for this term.
     */
    int docFreq(Term term) throws IOException;

    /**
     * Return the IndexReader this searches.
     */
    ZimbraIndexReader getIndexReader();
    
    /**
     * Finds the top n hits for query.
     */
    public TopDocs search(Query query, int n) throws IOException;
    
    /**
     * Finds the top n hits for query, applying filter if non-null.
     */
    public TopDocs search(Query query, Filter filter, int n) throws IOException;

    /**
     * Search implementation with arbitrary sorting. Finds the top n hits for query, applying filter if non-null,
     * and sorting the hits by the criteria in sort. NOTE: this does not compute scores by default; use
     * setDefaultFieldSortScoring(boolean, boolean) to enable scoring.
     */
    public TopFieldDocs search(Query query, Filter filter, int n, Sort sort) throws IOException;

}
