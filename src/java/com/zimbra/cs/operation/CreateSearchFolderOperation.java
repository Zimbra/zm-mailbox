/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s):
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.operation;

import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.SearchFolder;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.service.ServiceException;
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
