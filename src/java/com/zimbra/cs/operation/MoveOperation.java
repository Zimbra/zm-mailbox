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

import java.util.Arrays;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailItem.TargetConstraint;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.session.Session;

public class MoveOperation extends Operation {

    private static int LOAD = 15;
    static {
        Operation.Config c = loadConfig(MoveOperation.class);
        if (c != null)
            LOAD = c.mLoad;
    }
    
    int[] mItemIds;
    byte mType;
    int mTargetId;
    TargetConstraint mTcon;
    
    public MoveOperation(Session session, OperationContext oc, Mailbox mbox, Requester req,
                int itemId, byte type, int targetId)
    {
        this(session, oc, mbox, req, new int[] { itemId }, type, targetId, null);
    }
    
    public MoveOperation(Session session, OperationContext oc, Mailbox mbox, Requester req,
                int[] itemIds, byte type, int targetId, TargetConstraint tcon)
    {
        super(session, oc, mbox, req, LOAD);
        
        mItemIds = itemIds;
        mType = type;
        mTargetId = targetId;
        mTcon = tcon;
    }
    
    protected void callback() throws ServiceException {
        getMailbox().move(this.getOpCtxt(), mItemIds, mType, mTargetId, mTcon);
    }

    public String toString() {
        StringBuilder toRet = new StringBuilder(super.toString());
        toRet.append(" id=").append(Arrays.toString(mItemIds)).append(" type=").append(mType).append(" target=").append(mTargetId);
        return toRet.toString();
    }
}
