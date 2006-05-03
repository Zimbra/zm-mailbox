package com.zimbra.cs.imap;

import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.operation.Operation;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.session.Session;

public class GetImapFolderOperation extends Operation {
	String mFolderName;
	boolean mWritable;
	ImapFolder mResult;
	
	public ImapFolder getResult() { return mResult; }
	public boolean getWritable()   { return mWritable; }
	
	private final static int BASE_LOAD = 1000;
	
	public GetImapFolderOperation(Session session, OperationContext oc, Mailbox mbox, String folderName, boolean writable) throws ServiceException		
	{
		super(session, oc, mbox, Requester.IMAP, Requester.IMAP.getPriority(), BASE_LOAD);
		
		mFolderName = folderName;
		mWritable = writable;
		
		schedule();
	}
	
	protected void callback() throws ServiceException 
	{
		synchronized(mMailbox) {
			mResult = new ImapFolder(mFolderName, mWritable, mMailbox, mOpCtxt);
			mWritable = mResult.isWritable();
		}
	}
	
	public String toString() {
		return super.toString() + " Folder="+mFolderName+" Writable="+mWritable;
	}
}
