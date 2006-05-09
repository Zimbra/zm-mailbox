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
