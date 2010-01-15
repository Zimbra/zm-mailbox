/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009 Zimbra, Inc.
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

import com.zimbra.cs.index.IndexWritersCache.IndexWriter;

/**
 * 
 */
public class DummyIndexWritersCache implements IIndexWritersCache {

    /**
     * 
     */
    public DummyIndexWritersCache() {
    // TODO Auto-generated constructor stub
    }

    public void beginWriting(IndexWriter w) throws IOException {
        w.doWriterOpen();
    }

    public void doneWriting(IndexWriter w) {
        w.doWriterClose();
    }

    public void flush(IndexWriter target) {
        target.doWriterClose();
    }

    public void flushAllWriters() {
        // nop
    }

    public void shutdown() {
        // nop
    }

}
