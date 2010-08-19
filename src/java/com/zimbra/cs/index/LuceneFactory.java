/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010 Zimbra, Inc.
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

import com.zimbra.common.service.ServiceException;

/**
 *
 */
public class LuceneFactory implements IIndexFactory {

    @Override
    public ILuceneIndex create(MailboxIndex idx, String idxParentDir,
            long mailboxId) throws ServiceException {
        return new LuceneIndex(idx, idxParentDir, mailboxId);
    }

    @Override
    public void flushAllWriters() {
        LuceneIndex.flushAllWriters();
    }

    @Override
    public void shutdown() {
        LuceneIndex.shutdown();
    }

    @Override
    public void startup() {
        LuceneIndex.startup();
    }

    @Override
    public TextQueryOperation createTextQueryOperation() {
        return new LuceneQueryOperation();
    }

}
