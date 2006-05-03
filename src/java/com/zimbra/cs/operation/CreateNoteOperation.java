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
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Sep 8, 2004
 */
package com.zimbra.cs.operation;

import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Note;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.mailbox.Note.Rectangle;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.session.Session;

public class CreateNoteOperation extends Operation {
	
	private static final int LOAD = 10;
	
	private String mContent;
	private Rectangle mBounds;
	private byte mColor;
	private ItemId mIidFolder;
	
	private Note mNote;

	public CreateNoteOperation(Session session, OperationContext oc, Mailbox mbox, Requester req,
				String content, Rectangle bounds, byte color, ItemId iidFolder) {
		super(session, oc, mbox, req, LOAD);
		
		mContent = content;
		mBounds = bounds;
		mColor = color;
		mIidFolder = iidFolder;
	}

	public String toString() {
		StringBuilder toRet = new StringBuilder("CreateNote(");
		
		toRet.append("content=").append(mContent);
		toRet.append(" bounds=").append(mBounds.toString());
		toRet.append(" color=").append(mColor);
		toRet.append(" folder=").append(mIidFolder.toString());
		
		toRet.append(")");
		
		return toRet.toString();
	}

	protected void callback() throws ServiceException {
		mNote = getMailbox().createNote(getOpCtxt(), mContent, mBounds, mColor, mIidFolder.getId());
	}
	
	public Note getNote() { return mNote; }

}
