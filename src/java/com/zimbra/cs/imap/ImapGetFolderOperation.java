/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.imap;

import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.operation.Operation;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;

public class ImapGetFolderOperation extends Operation {
	private static int LOAD = 25;
	static {
		Operation.Config c = loadConfig(ImapGetFolderOperation.class);
		if (c != null)
			LOAD = c.mLoad;
	}
	
	String mFolderName;
	boolean mWritable;
	ImapFolder mResult, mOldSelected;

	public ImapFolder getResult()  { return mResult; }

	public ImapGetFolderOperation(ImapSession session, OperationContext oc, Mailbox mbox, String folderName, boolean writable, ImapFolder i4old) {
		super(session, oc, mbox, Requester.IMAP, Requester.IMAP.getPriority(), LOAD);

		mFolderName = folderName;
		mWritable = writable;
        mOldSelected = i4old;
	}

	public static class MemEater {
		public char mem[];

		public MemEater() {
			mem = new char[65536];
			for (int i = 0; i < mem.length; i++)
				mem[i] = (char)i;
		}
	}

	protected void callback() throws ServiceException {
//		System.out.println("Starting GetImapFolder !\n"+ this.toString());
		synchronized(mMailbox) {
			
//			MemEater eater[] = new MemEater[100];
//
//			for (int i = 0; i < eater.length; i++) 
//				eater[i] = new MemEater();

		    ImapSession session = (ImapSession) mSession;
            if (mOldSelected != null && !mOldSelected.isVirtual() && mFolderName.toLowerCase().equals(ImapFolder.importPath(mOldSelected.getPath(), session).toLowerCase())) {
                try {
                    mOldSelected.reopen(mWritable, mMailbox, session);
                    mResult = mOldSelected;
                } catch (ServiceException e) {
                    ZimbraLog.imap.warn("error quick-reopening folder " + mFolderName + "; proceeding with manual reopen", e);
                }
            }

            if (mResult == null) {
                mResult = new ImapFolder(mFolderName, mWritable, mMailbox, session);
            }

//			int total = 0;
//			for (int i = 0; i < 10000000; i++)
//				for (int j = 0; j < 100; j++) 
//					total+=i;
//			try { Thread.sleep(10000); } catch (Exception e) {}
//			System.out.println("Total is "+total+" mem0 "+eater[0].mem[64]);
		}
//		System.out.println("COMPLETED: GetImapFolder\n" + this.toString());
	}
	
	public String toString() {
		return super.toString() + " Folder=" + mFolderName;
	}
}
