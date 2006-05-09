package com.zimbra.cs.imap;

import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.operation.Operation;
import com.zimbra.cs.service.ServiceException;

public class ImapSubscribeOperation extends Operation {
	private static int LOAD = 25;
	static {
		Operation.Config c = loadConfig(ImapSubscribeOperation.class);
		if (c != null)
			LOAD = c.mLoad;
	}

	private String mFolderName;
	private ImapSession mImapSession;
	
	ImapSubscribeOperation(ImapSession session, OperationContext oc, Mailbox mbox, String folderName) throws ServiceException		
	{
		super(session, oc, mbox, Requester.IMAP, Requester.IMAP.getPriority(), LOAD);
		mFolderName = folderName;
		mImapSession = session;
	}	
	
	protected void callback() throws ServiceException {
        synchronized (mMailbox) {
            Folder folder = mMailbox.getFolderByPath(this.getOpCtxt(), mFolderName);
            if (!ImapFolder.isFolderVisible(folder)) {
            	throw ImapServiceException.FOLDER_NOT_VISIBLE(mFolderName);
            }
            mImapSession.subscribe(folder);
        }
	}

}
