/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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
import java.util.List;

import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;

/**
 * Abstraction of index write operations.
 *
 * @see IndexStore#openIndexer()
 * @author ysasaki
 * @author smukhopadhyay
 */
public interface Indexer extends Closeable {

    /**
     * Adds index documents.
     */
    void addDocument(Folder folder, MailItem item, List<IndexDocument> docs) throws IOException;

    /**
     * Deletes index documents.
     *
     * @param ids list of item IDs to delete
     */
    void deleteDocument(List<Integer> ids) throws IOException;

    /**
     * Compacts the index by expunging all the deletes.
     */
    void compact();

    /**
     * Modeled on {@link org.apache.lucene.index.IndexReader} {@code maxDoc()} whose description is: <br />
     * Returns total number of docs in this index, including docs not yet flushed (still in the RAM buffer),
     * not counting deletions.  Note that this is a cached value.
     * <p>Used from SOAP GetIndexStatsRequest</p>
     * @return total number of documents in this index excluding deletions
     */
    int maxDocs();

    /**
     * Index a stand-alone document
     * Currently used to index search history items
     */
    void addDocument(IndexDocument doc) throws IOException;

    /**
     * Deletes index documents by the specified field name
     */
    void deleteDocument(List<Integer> ids, String fieldName) throws IOException;
}
