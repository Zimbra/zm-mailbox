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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.zimbra.cs.mailbox.Contact;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.operation.Scheduler.Priority;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.session.Session;

public class CreateContactOperation extends Operation {
	
	private static int LOAD = 3;
	static {
		Operation.Config c = loadConfig(CreateContactOperation.class);
		if (c != null)
			LOAD = c.mLoad;
	}
	
	private ItemId mIidFolder;
	private String mTagsStr;
	private List<Map<String, String>> mList;
	
	private List<Contact> mContacts;
    
    private final static int CHUNK_SIZE = 100;
    
    public static List<ItemId> ImportCsvContacts(Session session, OperationContext oc, Mailbox mbox, Requester req,
                ItemId iidFolder, List<Map<String, String>> csvContacts, String tagsStr) throws ServiceException {
        
        List<ItemId> toRet = new LinkedList<ItemId>();
        
        Iterator<Map<String, String>> iter = csvContacts.iterator();
        
        List<Map<String, String>> curChunk = new LinkedList<Map<String,String>>();
        
        while(iter.hasNext()) {
            curChunk.clear();
            for (int i = 0; i < CHUNK_SIZE && iter.hasNext(); i++) {
                curChunk.add(iter.next());
            }
            CreateContactOperation op = new CreateContactOperation(session, oc, mbox, req, iidFolder, curChunk, tagsStr);
            op.schedule();
            for (Contact c : op.getContacts())
                toRet.add(new ItemId(c));
            
            op = null;
        }
        
        return toRet;
    }

	public CreateContactOperation(Session session, OperationContext oc, Mailbox mbox, Requester req,
				ItemId iidFolder, Map<String,String> attrs, String tagsStr)
	{
		super(session, oc, mbox, req, LOAD);
		mIidFolder = iidFolder;
		mList = new ArrayList<Map<String, String>>(1);
		mList.add(attrs);
		mTagsStr = tagsStr;
	}
	
	public CreateContactOperation(Session session, OperationContext oc, Mailbox mbox, Requester req,
				ItemId iidFolder, List<Map<String,String>> list, String tagsStr)
	{
		super(session, oc, mbox, req, Priority.BATCH, Math.max(LOAD * list.size(), 30));
		mIidFolder = iidFolder;
		mList = list;
		mTagsStr = tagsStr;
	}
	
	protected void callback() throws ServiceException {
		mContacts = new ArrayList<Contact>();
		synchronized(getMailbox()) {
			for (Map<String, String> attrs : mList) 
				mContacts.add(getMailbox().createContact(getOpCtxt(), attrs, mIidFolder.getId(), mTagsStr));
		}
	}
	
	public List<Contact> getContacts() { 
		return mContacts;
	}
	
	public Contact getContact() {
		return mContacts.get(0);
	}
	
	public String toString() {
		StringBuilder toRet = new StringBuilder();
		toRet.append("CreateContact(folder=").append(mIidFolder.toString());
		toRet.append(" ").append(mList.size()).append(" entries)");
		return toRet.toString();
	}

}
