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
package com.zimbra.cs.imap;

import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.operation.Operation;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.session.Session;

public class GetImapFolderOperation extends Operation {
	private static int LOAD = 25;
	static {
		Operation.Config c = loadConfig(GetImapFolderOperation.class);
		if (c != null)
			LOAD = c.mLoad;
	}
	
	String mFolderName;
	boolean mWritable;
	ImapFolder mResult;
	
	public ImapFolder getResult() { return mResult; }
	public boolean getWritable()   { return mWritable; }
	
	public GetImapFolderOperation(Session session, OperationContext oc, Mailbox mbox, String folderName, boolean writable) throws ServiceException		
	{
		super(session, oc, mbox, Requester.IMAP, Requester.IMAP.getPriority(), LOAD);
		
		mFolderName = folderName;
		mWritable = writable;
	}
	
	public static class MemEater {
		public char mem[];
		
		public MemEater() {
			mem = new char[65536];
			for (int i = 0; i < mem.length; i++)
				mem[i] = (char)i;
		}
	}
	
	protected void callback() throws ServiceException 
	{
		System.out.println("Starting GetImapFolder !\n"+ this.toString());
		synchronized(mMailbox) {
			
//			MemEater eater[] = new MemEater[100];
//
//			for (int i = 0; i < eater.length; i++) 
//				eater[i] = new MemEater();
			
			mResult = new ImapFolder(mFolderName, mWritable, mMailbox, mOpCtxt);
			
//			int total = 0;
//			for (int i = 0; i < 10000000; i++)
//				for (int j = 0; j < 100; j++) 
//					total+=i;
//			try { Thread.sleep(10000); } catch (Exception e) {}
//			System.out.println("Total is "+total+" mem0 "+eater[0].mem[64]);

			mWritable = mResult.isWritable();
		}
		System.out.println("COMPLETED: GetImapFolder\n" + this.toString());
	}
	
	public String toString() {
		return super.toString() + " Folder="+mFolderName+" Writable="+mWritable;
	}
}
