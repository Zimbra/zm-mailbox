package com.zimbra.cs.operation;

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

	protected void callback() throws ServiceException {
		if (!mIId.hasSubpart()) {
			mMsg = mMailbox.getMessageById(mOpCtxt, mIId.getId());
            
			if (mRead && mMsg.isUnread() && !RedoLogProvider.getInstance().isSlave())
				mMailbox.alterTag(mOpCtxt, mIId.getId(), MailItem.TYPE_MESSAGE, Flag.ID_FLAG_UNREAD, false);
		} else {
			mAppt = mMailbox.getAppointmentById(mOpCtxt, mIId.getId());
        }
	}
	
	public String toString() {
		String toRet = super.toString() + " IID="+mIId.toString()+" mRaw="+mRaw+" mRead="+mRead;
		if (mPart != null) {
			toRet = toRet + " Part="+mPart;
		}
		return toRet;
	}

	public GetMsgOperation(Session session, OperationContext oc, Mailbox mbox, Requester req,
				ItemId iid, boolean raw, boolean read, String part) throws ServiceException {
		super(session, oc, mbox, req, 5);
		
		mIId = iid;
		mRaw = raw;
		mRead = read;
		mPart = part;
		
		run();
	}
	
	public Message getMsg() { return mMsg; }
	public Appointment getAppt() { return mAppt; }

	ItemId mIId;
	boolean mRaw;
	boolean mRead;
	String mPart;
	Message mMsg;
	Appointment mAppt;
	

}
