package com.zimbra.cs.imap;

import java.util.Map;

import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.operation.Operation;
import com.zimbra.cs.service.ServiceException;

public class ImapLSubOperation extends Operation {
	private static int LOAD = 25;
	static {
		Operation.Config c = loadConfig(ImapLSubOperation.class);
		if (c != null)
			LOAD = c.mLoad;
	}
	
	private ImapSession mImapSession;
	private IImapGetFolderAttributes mIGetFolderAttributes; 	
	private String mPattern;
	
	private Map<String, String> mSubs;
	
	ImapLSubOperation(ImapSession session, OperationContext oc, Mailbox mbox, 
				IImapGetFolderAttributes getFolderAttributes, String pattern) throws ServiceException		
	{
		super(session, oc, mbox, Requester.IMAP, Requester.IMAP.getPriority(), LOAD);

		mImapSession = session;
		mIGetFolderAttributes = getFolderAttributes;
		mPattern = pattern;
	}
	
	protected void callback() throws ServiceException {
		synchronized (mMailbox) {
			mSubs = mImapSession.getMatchingSubscriptions(mMailbox, mPattern);
			for (Map.Entry<String, String> hit : mSubs.entrySet()) {
				Folder folder = null;
				try {
					folder = mMailbox.getFolderByPath(this.getOpCtxt(), hit.getKey());
				} catch (MailServiceException.NoSuchItemException nsie) { }
				// FIXME: need to determine "name attributes" for mailbox (\Marked, \Unmarked, \Noinferiors, \Noselect)
				boolean visible = hit.getValue() != null && ImapFolder.isFolderVisible(folder);
				String attributes = visible ? mIGetFolderAttributes.doGetFolderAttributes(folder) : "\\Noselect";
				hit.setValue("LSUB (" + attributes + ") \"/\" " + ImapFolder.encodeFolder(hit.getKey()));
			}
		}
	}
	
	Map<String, String> getSubs() { return mSubs; }
}
