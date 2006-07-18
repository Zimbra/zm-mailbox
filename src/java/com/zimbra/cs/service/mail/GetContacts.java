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
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
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

import com.zimbra.cs.account.Account;
import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.index.MailboxIndex;
import com.zimbra.cs.mailbox.Contact;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.operation.GetContactListOperation;
import com.zimbra.cs.operation.GetContactOperation;
import com.zimbra.cs.operation.Operation.Requester;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.session.Session;
import com.zimbra.cs.session.PendingModifications.Change;
import com.zimbra.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;

public class GetContacts extends MailDocumentHandler  {

	private static final int ALL_FOLDERS = -1;

	protected static final String[] TARGET_FOLDER_PATH = new String[] { MailService.A_FOLDER };
	protected String[] getProxiedIdPath(Element request) {
		return TARGET_FOLDER_PATH;
	}

	public Element handle(Element request, Map<String, Object> context) throws ServiceException {
		ZimbraSoapContext zsc = getZimbraSoapContext(context);
		Mailbox mbox = getRequestedMailbox(zsc);
		Mailbox.OperationContext octxt = zsc.getOperationContext();
		Session session = getSession(context);

		boolean sync = request.getAttributeBool(MailService.A_SYNC, false);
		String folderIdStr  = request.getAttribute(MailService.A_FOLDER, null);
		int folderId = ALL_FOLDERS;
		if (folderIdStr != null) { 
			ItemId iidFolder = new ItemId(folderIdStr, zsc);
			if (iidFolder.belongsTo(mbox))
				folderId = iidFolder.getId();
			else 
				throw ServiceException.FAILURE("Got remote folderId: " + folderIdStr + " but did not proxy", null);
		}

		byte sort = DbMailItem.SORT_NONE;
		String sortStr = request.getAttribute(MailService.A_SORTBY, "");
		if (sortStr.equals(MailboxIndex.SortBy.NAME_ASCENDING.toString()))
			sort = DbMailItem.SORT_BY_SENDER | DbMailItem.SORT_ASCENDING;
		else if (sortStr.equals(MailboxIndex.SortBy.NAME_DESCENDING.toString()))
			sort = DbMailItem.SORT_BY_SENDER | DbMailItem.SORT_DESCENDING;

		ArrayList<String> attrs = null;
		ArrayList<ItemId> ids = null;

		for (Element e : request.listElements())
			if (e.getName().equals(MailService.E_ATTRIBUTE)) {
				String name = e.getAttribute(MailService.A_ATTRIBUTE_NAME);
				if (attrs == null)
					attrs = new ArrayList<String>();
				attrs.add(name);
			} else if (e.getName().equals(MailService.E_CONTACT)) {
				String idStr =  e.getAttribute(MailService.A_ID);
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

		Element response = zsc.createElement(MailService.GET_CONTACTS_RESPONSE);
		ContactAttrCache cacache = null;

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
				for (Element e : responses) {
					response.addElement(e);
				}
			}
			
			if (local.size() > 0) {
				GetContactOperation op = new GetContactOperation(session, octxt, mbox, Requester.SOAP, local);
				op.schedule();
				List<Contact> contacts = op.getResults();
				
				for (Contact con : contacts) {
					if (con != null && (folderId == ALL_FOLDERS || folderId == con.getFolderId()))
						ToXML.encodeContact(response, zsc, con, cacache, false, attrs, fields);
				}
			}
			
		} else {
			ItemId iidFolder = new ItemId(mbox, folderId);
			GetContactListOperation op = new GetContactListOperation(session, octxt, mbox, Requester.SOAP, iidFolder, sort);
			op.schedule();
			List<Contact> contacts = op.getResults();

			for (Contact con : contacts)
				if (con != null)
					ToXML.encodeContact(response, zsc, con, cacache, false, attrs, fields);
		}
		return response;
	}


	private void partitionItems(ZimbraSoapContext lc, ArrayList<ItemId> ids, ArrayList<Integer> local, HashMap<String, StringBuffer> remote) throws ServiceException {
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

	private List<Element> proxyRemote(Element request, Map<String, StringBuffer> remote, Map<String,Object> context)
	throws ServiceException {
		List<Element> responses = new ArrayList<Element>();

        Element cn = request.addElement(MailService.E_CONTACT);
        for (Map.Entry<String, StringBuffer> entry : remote.entrySet()) {
			cn.addAttribute(MailService.A_ID, entry.getValue().toString());

			Element response = proxyRequest(request, context, entry.getKey());
			extractResponses(response, responses);
		}

		return responses;
	}

	protected void extractResponses(Element responseElt, List<Element> responses) {
		for (Element e : responseElt.listElements(MailService.E_CONTACT)) {
			e.detach();
			responses.add(e);
		}
	}
}
