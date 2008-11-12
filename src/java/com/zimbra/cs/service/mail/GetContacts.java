/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on May 26, 2004
 */
package com.zimbra.cs.service.mail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.db.DbSearch;
import com.zimbra.cs.index.MailboxIndex;
import com.zimbra.cs.mailbox.Contact;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.service.util.ItemIdFormatter;
import com.zimbra.cs.session.PendingModifications.Change;
import com.zimbra.soap.ZimbraSoapContext;

public class GetContacts extends MailDocumentHandler  {

	private static final int ALL_FOLDERS = -1;

	protected static final String[] TARGET_FOLDER_PATH = new String[] { MailConstants.A_FOLDER };
	protected String[] getProxiedIdPath(Element request) {
		return TARGET_FOLDER_PATH;
	}

	public Element handle(Element request, Map<String, Object> context) throws ServiceException {
		ZimbraSoapContext zsc = getZimbraSoapContext(context);
		Mailbox mbox = getRequestedMailbox(zsc);
		Mailbox.OperationContext octxt = getOperationContext(zsc, context);
        ItemIdFormatter ifmt = new ItemIdFormatter(zsc);

		boolean sync = request.getAttributeBool(MailConstants.A_SYNC, false);
		String folderIdStr  = request.getAttribute(MailConstants.A_FOLDER, null);
		int folderId = ALL_FOLDERS;
		if (folderIdStr != null) { 
			ItemId iidFolder = new ItemId(folderIdStr, zsc);
			if (iidFolder.belongsTo(mbox))
				folderId = iidFolder.getId();
			else 
				throw ServiceException.FAILURE("Got remote folderId: " + folderIdStr + " but did not proxy", null);
		}

		byte sort = DbSearch.SORT_NONE;
		String sortStr = request.getAttribute(MailConstants.A_SORTBY, "");
		if (sortStr.equals(MailboxIndex.SortBy.NAME_ASCENDING.toString()))
			sort = DbSearch.SORT_BY_SENDER | DbSearch.SORT_ASCENDING;
		else if (sortStr.equals(MailboxIndex.SortBy.NAME_DESCENDING.toString()))
			sort = DbSearch.SORT_BY_SENDER | DbSearch.SORT_DESCENDING;

		ArrayList<String> attrs = null;
		ArrayList<ItemId> ids = null;

		for (Element e : request.listElements()) {
			if (e.getName().equals(MailConstants.E_ATTRIBUTE)) {
				String name = e.getAttribute(MailConstants.A_ATTRIBUTE_NAME);
				if (attrs == null)
					attrs = new ArrayList<String>();
				attrs.add(name);
			} else if (e.getName().equals(MailConstants.E_CONTACT)) {
				String idStr = e.getAttribute(MailConstants.A_ID);
				String targets[] = idStr.split(",");
				for (String target : targets) { 
					ItemId iid = new ItemId(target, zsc);
					if (ids == null)
						ids = new ArrayList<ItemId>();
					ids.add(iid);
				}
				
				// remove it from the request, so we can re-use the request for proxying below
				e.detach();
			}
        }

		Element response = zsc.createElement(MailConstants.GET_CONTACTS_RESPONSE);

		// want to return modified date only on sync-related requests
		int fields = ToXML.NOTIFY_FIELDS;
		if (sync)
			fields |= Change.MODIFIED_CONFLICT;


		if (ids != null) {
			ArrayList<Integer> local = new ArrayList<Integer>();
			HashMap<String, StringBuffer> remote = new HashMap<String, StringBuffer>();
			partitionItems(zsc, ids, local, remote);
			
			if (remote.size() > 0) {
				if (folderId > 0)
					throw ServiceException.INVALID_REQUEST("Cannot specify a folder with mixed-mailbox items", null);

				List<Element> responses = proxyRemote(request, remote, context);
				for (Element e : responses)
					response.addElement(e);
			}

			if (local.size() > 0) {
			    synchronized(mbox) {
                    for (int id : local) {
                        Contact con = mbox.getContactById(octxt, id);
                        if (con != null && (folderId == ALL_FOLDERS || folderId == con.getFolderId()))
                            ToXML.encodeContact(response, ifmt, con, false, attrs, fields);
                    }
			    }
			}
			
		} else {
			ItemId iidFolder = new ItemId(mbox, folderId);
            List<Contact> contacts;
            contacts = mbox.getContactList(octxt, iidFolder != null ? iidFolder.getId() : -1, sort);

			for (Contact con : contacts) {
				if (con != null)
					ToXML.encodeContact(response, ifmt, con, false, attrs, fields);
            }
		}
		return response;
	}


	static void partitionItems(ZimbraSoapContext lc, ArrayList<ItemId> ids, ArrayList<Integer> local, HashMap<String, StringBuffer> remote)
    throws ServiceException {
		Account acct = getRequestedAccount(lc);
		for (ItemId iid : ids) {
			if (iid.belongsTo(acct))
				local.add(iid.getId());
			else {
				StringBuffer sb = remote.get(iid.getAccountId());
				if (sb == null)
					remote.put(iid.getAccountId(), new StringBuffer(iid.toString()));
				else
					sb.append(',').append(iid.toString());
			}
		}
	}

	List<Element> proxyRemote(Element request, Map<String, StringBuffer> remote, Map<String,Object> context)
	throws ServiceException {
		List<Element> responses = new ArrayList<Element>();

        // note that we removed all <cn> elements from the request during handle(), above
        Element cn = request.addElement(MailConstants.E_CONTACT);
        for (Map.Entry<String, StringBuffer> entry : remote.entrySet()) {
			cn.addAttribute(MailConstants.A_ID, entry.getValue().toString());

			Element response = proxyRequest(request, context, entry.getKey());
            for (Element e : response.listElements())
                responses.add(e.detach());
		}

		return responses;
	}
}
