/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014, 2016 Synacor, Inc.
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

import java.io.Closeable;
import java.io.IOException;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;

import com.zimbra.common.service.ServiceException;

/**
 * Modeled on a subset of {@link org.apache.lucene.search.IndexSearcher}
 */
public interface ZimbraIndexSearcher extends Closeable {

    /**
     * Returns the stored fields of a document {@code docID} (an index store specific ID for the document)
     * @throws ServiceException
     */
    Document doc(ZimbraIndexDocumentID docID) throws IOException, ServiceException;

    /**
     * Sometimes used to decide whether we think a query is best evaluated DB-FIRST or INDEX-FIRST.
     * @return the number of documents containing the term {@code term}.
     * @throws ServiceException
     */
    int docFreq(Term term) throws IOException, ServiceException;

    /**
     * Return the IndexReader this searches.
     */
    ZimbraIndexReader getIndexReader();

    /**
     * Finds the top n hits for query.
     * @throws ServiceException
     */
    public ZimbraTopDocs search(Query query, int n) throws IOException, ServiceException;

    /**
     * Finds the top n hits for query, applying filter if non-null.
     * @throws ServiceException
     */
    public ZimbraTopDocs search(Query query, ZimbraTermsFilter filter, int n) throws IOException, ServiceException;

    /**
     * Search implementation with arbitrary sorting. Finds the top n hits for query, applying filter if non-null,
     * and sorting the hits by the criteria in sort. NOTE: this does not compute scores by default; use
     * setDefaultFieldSortScoring(boolean, boolean) to enable scoring.
     * @throws ServiceException
     */
    public ZimbraTopDocs search(Query query, ZimbraTermsFilter filter, int n, Sort sort) throws IOException, ServiceException;

    /**
     * Search implementation that also accepts a field name to be used as the ZimbraIndexDocumentID field,
     * as well as an array of additional fields to fetch from the index.
     */
    public ZimbraTopDocs search(Query query, ZimbraTermsFilter filter, int n, Sort sort, String idField, String[] fetchFields) throws IOException, ServiceException;
}
