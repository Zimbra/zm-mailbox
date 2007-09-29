/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.operation;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.SearchFolder;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.session.Session;

public class ModifySearchFolderOperation extends Operation {

    private static int LOAD = 2;
    static {
        Operation.Config c = loadConfig(ModifySearchFolderOperation.class);
        if (c != null)
            LOAD = c.mLoad;
    }

    private ItemId mIid;
    private String mQuery;
    private String mTypes;
    private String mSort;

    private SearchFolder mSf;

    public ModifySearchFolderOperation(Session session, OperationContext oc, Mailbox mbox, Requester req,
                ItemId iid, String query, String types, String sort) {
        super(session, oc, mbox, req, LOAD);
        mIid = iid;
        mQuery = query;
        mTypes = types;
        mSort = sort;
    }	

    protected void callback() throws ServiceException {
        getMailbox().modifySearchFolder(getOpCtxt(), mIid.getId(), mQuery, mTypes, mSort);
        mSf = getMailbox().getSearchFolderById(getOpCtxt(), mIid.getId());
    }

    public SearchFolder getSf() { return mSf; }

}
