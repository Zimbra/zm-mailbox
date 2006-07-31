/* ***** BEGIN LICENSE BLOCK *****
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

import java.util.List;

import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailItem.TargetConstraint;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.session.Session;
import com.zimbra.cs.util.ArrayUtil;


public class DeleteOperation extends Operation {
    
    private static int LOAD = 15;
        static {
            Operation.Config c = loadConfig(DeleteOperation.class);
            if (c != null)
                LOAD = c.mLoad;
        }

    public static final int SUGGESTED_BATCH_SIZE = 30;

    private int[] mItemIds;
    private byte mType;
    private TargetConstraint mTcon;

    public DeleteOperation(Session session, OperationContext oc, Mailbox mbox, Requester req,
                int itemId, byte type) {
        this(session, oc, mbox, req, itemId, type, null);
    }

    public DeleteOperation(Session session, OperationContext oc, Mailbox mbox, Requester req,
                int itemId, byte type, TargetConstraint tcon) {
        super(session, oc, mbox, req, LOAD);

        mType = type;
        mTcon = tcon;
        mItemIds = new int[] { itemId };
    }

    public DeleteOperation(Session session, OperationContext oc, Mailbox mbox, Requester req,
                List<Integer> itemIds, byte type, TargetConstraint tcon) {
        super(session, oc, mbox, req, LOAD * itemIds.size());

        mType = type;
        mTcon = tcon;
        mItemIds = new int[itemIds.size()];
        int i = 0;
        for (int id : itemIds)
            mItemIds[i++] = id;
    }

    protected void callback() throws ServiceException {
        getMailbox().delete(getOpCtxt(), mItemIds, mType, mTcon);
    }

    public String toString() {
        StringBuilder toRet = new StringBuilder(super.toString());
        toRet.append(" id=").append(ArrayUtil.toString(mItemIds)).append(" type=").append(mType);
        return toRet.toString();
    }
}
