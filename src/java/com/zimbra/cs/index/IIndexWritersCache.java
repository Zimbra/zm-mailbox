/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010 Zimbra, Inc.
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

import com.zimbra.cs.index.IndexWritersCache.IndexWriter;

interface IIndexWritersCache {

    /**
     * Called when the system needs to quiesce the index volume, EG after crash recovery
     */
    public abstract void flushAllWriters();

    
    /**
     * Called at the beginning of a write operation.  The mailbox lock is held when this call
     * begins.
     * 
     * The IWC is responsible for calling IndexWriter.doWriterOpen() if necessary to open the writer
     * 
     * doneWriting() will be called as a pair to this operation
     * 
     * @param w
     * @throws IOException
     */
    public abstract void beginWriting(IndexWriter w) throws IOException;

    /**
     * Called as a pair to beginWriting() after the write has been completed.
     * 
     * @param w
     */
    public abstract void doneWriting(IndexWriter w);

    /**
     * Called at system shutdown
     */
    public abstract void shutdown();
    
    /**
     * Called when we need to flush an index from the cache.  The IWC is responsible for 
     * making sure that IndexWriter.doWriterClose() is called (exactly once) to close
     * the index file if necessary.
     * 
     * @param target
     */
    public abstract void flush(IndexWriter target);
    
}