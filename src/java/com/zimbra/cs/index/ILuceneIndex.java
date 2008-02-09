/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.index;

import java.io.IOException;

import org.apache.lucene.search.Sort;

import com.zimbra.cs.index.MailboxIndex.SortBy;

/**
 * 
 */
interface ILuceneIndex extends ITextIndex {
    Object getCurrentCommitPoint();
    void onClose(RefCountedIndexReader reader);
    RefCountedIndexSearcher getCountedIndexSearcher() throws IOException;
    Sort getSort(SortBy searchOrder);
    void checkBlobIds() throws IOException;
}
