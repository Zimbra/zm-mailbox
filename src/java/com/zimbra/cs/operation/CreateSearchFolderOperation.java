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

public class CreateSearchFolderOperation extends Operation {
    
    private static int LOAD = 8;
    static {
        Operation.Config c = loadConfig(CreateSearchFolderOperation.class);
        if (c != null)
            LOAD = c.mLoad;
    }

    private ItemId mIidParent;
    private String mName;
    private String mQuery;
    private String mTypes;
    private String mSort;
    private byte mColor;

    private SearchFolder mSearch;

    public CreateSearchFolderOperation(Session session, OperationContext oc, Mailbox mbox, Requester req,
                ItemId iidParent, String name, String query, String types, String sort, byte color) {
        super(session, oc, mbox, req, LOAD);

        mIidParent = iidParent;
        mName = name;
        mQuery = query;
        mTypes = types;
        mSort = sort;
        mColor = color;
    }
    
    public String toString() {
        StringBuilder toRet = new StringBuilder("CreateSearchFolder(");

        toRet.append(" parent=").append(mIidParent).append(" color=").append(mColor);
        if (mName != null)
            toRet.append(" name=").append(mName);

        if (mQuery!= null)
            toRet.append(" query=").append(mQuery);

        if (mTypes != null)
            toRet.append(" types=").append(mTypes);

        if (mSort!= null)
            toRet.append(" sort=").append(mSort);

        toRet.append(")");

        return toRet.toString();
    }

    protected void callback() throws ServiceException {
        mSearch = getMailbox().createSearchFolder(getOpCtxt(), mIidParent.getId(), mName, mQuery, mTypes, mSort, mColor);
    }

    public SearchFolder getSearchFolder() { return mSearch; }

}
