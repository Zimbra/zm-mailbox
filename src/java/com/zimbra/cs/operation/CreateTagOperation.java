package com.zimbra.cs.operation;

import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Tag;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.session.Session;

public class CreateTagOperation extends Operation {
	
	private static final int LOAD = 8;
	
	private String mName;
	private byte mColor;
	
	private Tag mTag;

	public CreateTagOperation(Session session, OperationContext oc, Mailbox mbox, Requester req,
				String name, byte color) {
		super(session, oc, mbox, req, LOAD);
		
		mName = name;
		mColor = color;
	}
	
	public String toString() {
		StringBuilder toRet = new StringBuilder("CreateTag(");
		
		toRet.append("name=").append(mName);
		toRet.append(" color=").append(mColor);
		
		toRet.append(")");
		return toRet.toString();
	}

	protected void callback() throws ServiceException {
		mTag = getMailbox().createTag(getOpCtxt(), mName, mColor);
	}
	
	public Tag getTag() { return mTag; }

}
