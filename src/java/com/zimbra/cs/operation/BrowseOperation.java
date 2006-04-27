package com.zimbra.cs.operation;

import java.io.IOException;

import com.zimbra.cs.mailbox.BrowseResult;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.session.Session;

public class BrowseOperation extends Operation {
	static final int LOAD = 10; 
	
	private String mBrowseBy;
	private BrowseResult mResult;
	
	public BrowseOperation(Session session, OperationContext oc, Mailbox mbox, 
				Requester req, String browseBy) throws ServiceException {
		super(session, oc, mbox, req, req.getPriority(), LOAD);
		mBrowseBy = browseBy;
		
		run();
	}
	
	protected void callback() throws ServiceException {
		try {
			mResult = mMailbox.browse(getOpCtxt(), mBrowseBy);
		} catch (IOException e) {
			throw ServiceException.FAILURE("IO error", e);
		}
	}
	
	public BrowseResult getResult() { return mResult; }
}
