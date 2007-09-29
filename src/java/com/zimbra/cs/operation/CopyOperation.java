/*
 * ***** BEGIN LICENSE BLOCK *****
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
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.operation;

import java.io.IOException;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.session.Session;

public class CopyOperation extends Operation {

    private static int LOAD = 15;
    static {
        Operation.Config c = loadConfig(CopyOperation.class);
        if (c != null)
            LOAD = c.mLoad;
    }
    
    int mItemId;
    byte mType;
    int mTargetId;
    
    MailItem mResult;
    
    public CopyOperation(Session session, OperationContext oc, Mailbox mbox, Requester req,
                int itemId, byte type, int targetId)
    {
        super(session, oc, mbox, req, LOAD);
        
        mItemId = itemId;
        mType = type;
        mTargetId = targetId;
    }
    
    protected void callback() throws ServiceException {
        try {
            mResult = getMailbox().copy(this.getOpCtxt(), mItemId, mType, mTargetId);
        } catch (IOException e) {
            throw ServiceException.FAILURE("Caught IOException execiting " + this.toString(), e);
        }
    }
    
    public MailItem getResult() { return mResult; }
    
    public String toString() {
        StringBuilder toRet = new StringBuilder(super.toString());

        toRet.append(" id=").append(mItemId).append(" type=").append(mType).append(" target=").append(mTargetId);
        
        return toRet.toString();
    }
}
