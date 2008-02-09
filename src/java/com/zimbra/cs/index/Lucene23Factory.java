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

import com.zimbra.common.service.ServiceException;

public class Lucene23Factory implements ILuceneFactory {
    
    public ILuceneIndex create(MailboxIndex idx, String idxParentDir, int mailboxId) throws ServiceException {
        return new Lucene23Index(idx, idxParentDir, mailboxId);
    }
 
    public void flushAllWriters() {
        Lucene23Index.flushAllWriters();
    }

    public void shutdown() {
        Lucene23Index.shutdown();
    }

    public void startup() {
        Lucene23Index.startup();
    }
}
