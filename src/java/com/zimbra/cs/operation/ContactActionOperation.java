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

import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.service.util.ItemIdFormatter;
import com.zimbra.cs.session.Session;
import com.zimbra.soap.ZimbraSoapContext;

public class ContactActionOperation extends ItemActionOperation {

    public static ContactActionOperation UPDATE(ZimbraSoapContext zc, Session session, OperationContext oc,
                                                Mailbox mbox, Requester req, List<Integer> ids, ItemId iidFolder,
                                                String flags, String tags, byte color, Map<String, String> fields)
    throws ServiceException {
        ContactActionOperation ca = new ContactActionOperation(zc, session, oc, mbox, req, LOAD, ids, Op.UPDATE);
        ca.setIidFolder(iidFolder);
        ca.setFlags(flags);
        ca.setTags(tags);
        ca.setColor(color);
        ca.setFields(fields);
        ca.schedule();
        return ca;
    }

    // only when OP=UPDATE
    private Map<String, String> mFields;


    public void setFields(Map<String, String> fields) {                        
        assert(mOperation == Op.UPDATE);
        mFields = (fields == null || fields.isEmpty() ? null : fields); 
    }

    ContactActionOperation(ZimbraSoapContext zc, Session session, OperationContext octxt, Mailbox mbox,
            Requester req, int baseLoad, List<Integer> ids, Op op)
            throws ServiceException {
        super(zc, session, octxt, mbox, req, baseLoad, ids, op, MailItem.TYPE_CONTACT, true, null);
    }

    protected void callback() throws ServiceException {
        // iterate over the local items and perform the requested operation
        switch (mOperation) {
            case UPDATE:
                if (!mIidFolder.belongsTo(getMailbox()))
                    throw ServiceException.INVALID_REQUEST("cannot move item between mailboxes", null);
                
                if (mIidFolder.getId() > 0)
                    getMailbox().move(getOpCtxt(), mIds, mItemType, mIidFolder.getId(), mTargetConstraint);
                if (mTags != null || mFlags != null)
                    getMailbox().setTags(getOpCtxt(), mIds, mItemType, mFlags, mTags, mTargetConstraint);
                if (mColor >= 0)
                    getMailbox().setColor(getOpCtxt(), mIds, mItemType, mColor);
                if (mFields != null)
                    for (int id : mIds)
                        getMailbox().modifyContact(getOpCtxt(), id, mFields, true);
                break;
            default:
                throw ServiceException.INVALID_REQUEST("unknown operation: " + mOperation, null);
        }

        ItemIdFormatter ifmt = new ItemIdFormatter(mSoapContext);
        StringBuilder successes = new StringBuilder();
        for (int id : mIds)
            successes.append(successes.length() > 0 ? "," : "").append(ifmt.formatItemId(id));
        mResult = successes.toString();
    }

    public String toString() {
        StringBuffer toRet = new StringBuffer(super.toString());
        if (mOperation == Op.UPDATE) {
            if (mFields != null)
                toRet.append(" Fields=").append(mFields);
        }
        return toRet.toString();
    }
}
