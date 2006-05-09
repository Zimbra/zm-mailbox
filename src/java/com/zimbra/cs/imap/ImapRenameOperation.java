package com.zimbra.cs.imap;

import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.operation.Operation;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.session.Session;

public class ImapRenameOperation extends Operation {
	private static int LOAD = 10;
	static {
		Operation.Config c = loadConfig(ImapRenameOperation.class);
		if (c != null)
			LOAD = c.mLoad;
	}
	
	private String mOldName;
	private String mNewName;
	
	ImapRenameOperation(Session session, OperationContext oc, Mailbox mbox, String oldName, String newName) throws ServiceException
	{
		super(session, oc, mbox, Requester.IMAP, Requester.IMAP.getPriority(), LOAD);
		mOldName = oldName;
		mNewName = newName;
	}
	
	protected void callback() throws ServiceException {
		synchronized (mMailbox) {
			int folderId = mMailbox.getFolderByPath(this.getOpCtxt(), mOldName).getId();
			if (folderId != Mailbox.ID_FOLDER_INBOX) {
				mMailbox.renameFolder(this.getOpCtxt(), folderId, mNewName);
			} else {
				throw ImapServiceException.CANT_RENAME_INBOX();
			}
		}
	}
}
