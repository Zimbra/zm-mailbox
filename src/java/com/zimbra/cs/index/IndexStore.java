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

import org.apache.lucene.search.IndexSearcher;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.index.global.GlobalIndex;
import com.zimbra.cs.mailbox.Mailbox;

/**
 * Abstraction of index store backend.
 *
 * @author ysasaki
 */
public interface IndexStore {

    /**
     * {@link Indexer#close()} must be called after use.
     */
    Indexer openIndexer() throws IOException;

    /**
     * {@link IndexSearcher#close()} must be called after use.
     */
    IndexSearcher openSearcher() throws IOException;

    /**
     * Prime the index.
     */
    void warmup();

    /**
     * Removes from cache.
     */
    void evict();

    /**
     * Deletes the whole index data for the mailbox.
     */
    void deleteIndex() throws IOException;

    /**
     * Runs a sanity check for the index data.
     */
    boolean verify(PrintStream out) throws IOException;

    interface Factory {
        IndexStore getIndexStore(Mailbox mbox) throws ServiceException;
        GlobalIndex getGlobalIndex();
        void destroy();
    }

}
