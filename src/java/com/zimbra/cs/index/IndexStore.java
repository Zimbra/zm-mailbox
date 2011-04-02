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

import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;
import java.util.List;

import com.zimbra.cs.mailbox.MailItem;

/**
 * Abstraction of index store backend.
 *
 * @author ysasaki
 */
public interface IndexStore {

    /**
     * Starts bulk writes.
     * <ul>
     *  <li>Caller MUST hold call {@link #endWrite()} at the end.
     *  <li>Caller MUST hold the mailbox lock for the duration of the begin/end pair.
     * </ul>
     */
    void beginWrite() throws IOException;

    /**
     * Finishes the bulk writes started by {@link #beginWrite()}.
     */
    void endWrite() throws IOException;

    /**
     * Removes from cache.
     */
    void evict();

    /**
     * Adds index documents.
     */
    void addDocument(MailItem item, List<IndexDocument> docs) throws IOException;

    /**
     * Deletes index documents.
     *
     * @param ids list of item IDs to delete
     */
    void deleteDocument(List<Integer> ids) throws IOException;

    /**
     * Deletes the whole index data for the mailbox.
     */
    void deleteIndex() throws IOException;

    //TODO Hide Lucene Searcher
    IndexSearcherRef getIndexSearcherRef(SortBy sortBy) throws IOException;

    boolean expandWildcard(Collection<String> result, String field, String token, int max) throws IOException;
    List<BrowseTerm> getDomains(String field, String regex) throws IOException;
    List<BrowseTerm> getAttachmentTypes(String regex) throws IOException;
    List<BrowseTerm> getObjects(String regex) throws IOException;

    /**
     * Runs a sanity check for the index data.
     */
    boolean verify(PrintStream out) throws IOException;

}
