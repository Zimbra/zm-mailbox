/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.index;

import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Sort;

import com.zimbra.cs.index.MailboxIndex.SortBy;

/**
 * 
 */
interface ILuceneIndex extends ITextIndex {
    /**
     * Called when the reader is closed by the ReaderCache
     * 
     * @param reader
     */
    void onReaderClose(RefCountedIndexReader reader);
    
    /**
     * @return A refcounted RefCountedIndexSearcher for this index.  Caller is responsible for 
     *            calling RefCountedIndexReader.release() on the index before allowing it to go
     *            out of scope (otherwise a RuntimeException will occur)
     * 
     * @throws IOException
     */
    RefCountedIndexSearcher getCountedIndexSearcher() throws IOException;
    
    /**
     * @param reader
     * @return
     * @throws IOException
     */
    IndexReader reopenReader(IndexReader reader) throws IOException;
    
    /**
     * @param searchOrder
     * @return
     */
    Sort getSort(SortBy searchOrder);

    
    /**
     * @return Total bytes written to the filesystem by Lucene - for stat logging
     */
    long getBytesWritten();
    
    /**
     * @return Total bytes read from the filesystem by Lucene - for stat logging
     */
    long getBytesRead();
}
