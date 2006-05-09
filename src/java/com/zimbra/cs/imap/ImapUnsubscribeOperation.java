package com.zimbra.cs.imap;

import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.operation.Operation;
import com.zimbra.cs.service.ServiceException;

public class ImapUnsubscribeOperation extends Operation {
	private static int LOAD = 25;
	static {
		Operation.Config c = loadConfig(ImapUnsubscribeOperation.class);
		if (c != null)
			LOAD = c.mLoad;
	}

	private String mFolderName;
	private ImapSession mImapSession;
	
	ImapUnsubscribeOperation(ImapSession session, OperationContext oc, Mailbox mbox, String folderName) throws ServiceException		
	{
		super(session, oc, mbox, Requester.IMAP, Requester.IMAP.getPriority(), LOAD);
		mFolderName = folderName;
		mImapSession = session;
	}	
	
	protected void callback() throws ServiceException {
		synchronized (mMailbox) {
			mImapSession.unsubscribe(mMailbox.getFolderByPath(this.getOpCtxt(), mFolderName));
		}
	}
	
}
