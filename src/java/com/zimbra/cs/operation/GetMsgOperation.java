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

import java.util.ArrayList;
import java.util.List;

import com.zimbra.cs.mailbox.Appointment;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.redolog.RedoLogProvider;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.session.Session;

public class GetMsgOperation extends Operation {
    private int[] mMsgIds;
    private ItemId mIid;
    private boolean mRead;
    private List<Message> mMsgs;
    private Appointment mAppt;

	private static int LOAD = 5;
    	static {
    		Operation.Config c = loadConfig(GetMsgOperation.class);
    		if (c != null)
    			LOAD = c.mLoad;
    	}

    public GetMsgOperation(Session session, OperationContext octxt, Mailbox mbox, Requester req, ItemId iid, boolean read) {
        super(session, octxt, mbox, req, LOAD);

        mIid = iid;
        mRead = read;
    }

    public GetMsgOperation(Session session, OperationContext octxt, Mailbox mbox, Requester req, List<Integer> ids, boolean read) {
        super(session, octxt, mbox, req, LOAD);

        int i = 0;
        mMsgIds = new int[ids.size()];
        for (int id : ids)
            mMsgIds[i++] = id;
        mRead = read;
    }
	
	protected void callback() throws ServiceException {
        if (mMsgIds != null) {
            mMsgs = new ArrayList<Message>();
            for (MailItem item : mMailbox.getItemById(mOpCtxt, mMsgIds, MailItem.TYPE_MESSAGE))
                mMsgs.add((Message) item);
        } else if (!mIid.hasSubpart()) {
			Message msg = mMailbox.getMessageById(mOpCtxt, mIid.getId());
			(mMsgs = new ArrayList<Message>()).add(msg);

			if (mRead && msg.isUnread() && !RedoLogProvider.getInstance().isSlave())
				mMailbox.alterTag(mOpCtxt, mIid.getId(), MailItem.TYPE_MESSAGE, Flag.ID_FLAG_UNREAD, false);
		} else {
			mAppt = mMailbox.getAppointmentById(mOpCtxt, mIid.getId());
        }
	}

	public String toString() {
		String toRet = super.toString() + " IID=" + mIid + " mRead=" + mRead;
		return toRet;
	}

    public Message getMsg() { return (mMsgs != null && !mMsgs.isEmpty() ? mMsgs.get(0) : null); }
    public List<Message> getMsgList() { return mMsgs; }
	public Appointment getAppt() { return mAppt; }
}
