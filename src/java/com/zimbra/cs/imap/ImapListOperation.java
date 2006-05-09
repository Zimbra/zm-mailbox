package com.zimbra.cs.imap;

import java.util.ArrayList;
import java.util.List;

import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.operation.Operation;
import com.zimbra.cs.service.ServiceException;

public class ImapListOperation extends Operation {
	
	private static int LOAD = 25;
	static {
		Operation.Config c = loadConfig(ImapListOperation.class);
		if (c != null)
			LOAD = c.mLoad;
	}
	
	private IImapGetFolderAttributes mIGetFolderAttributes;
	private String mPattern;
	
	private List<String> mMatches;

	ImapListOperation(ImapSession session, OperationContext oc, Mailbox mbox, 
				IImapGetFolderAttributes getFolderAttributes, String pattern) throws ServiceException		
	{
		super(session, oc, mbox, Requester.IMAP, Requester.IMAP.getPriority(), LOAD);
		
		mIGetFolderAttributes = getFolderAttributes;
		mPattern = pattern;
	}

	protected void callback() throws ServiceException {
		synchronized (mMailbox) {
			mMatches = new ArrayList<String>();
			
			Folder root = mMailbox.getFolderById(this.getOpCtxt(), Mailbox.ID_FOLDER_USER_ROOT);
			for (Folder folder : root.getSubfolderHierarchy()) {
				if (!ImapFolder.isFolderVisible(folder))
					continue;
				String path = folder.getPath().substring(1);
				// FIXME: need to determine "name attributes" for mailbox (\Marked, \Unmarked, \Noinferiors, \Noselect)
				if (path.toUpperCase().matches(mPattern))
					mMatches.add("LIST (" + mIGetFolderAttributes.doGetFolderAttributes(folder) + ") \"/\" " + ImapFolder.encodeFolder(path));
			}
		}
	}
	
	public List<String> getMatches() { return mMatches; }

}
