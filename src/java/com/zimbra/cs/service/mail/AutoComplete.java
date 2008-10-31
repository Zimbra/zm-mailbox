/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008 Zimbra, Inc.
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
package com.zimbra.cs.service.mail;

import java.util.ArrayList;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.mailbox.ContactAutoComplete;
import com.zimbra.cs.mailbox.ContactAutoComplete.AutoCompleteResult;
import com.zimbra.cs.mailbox.ContactAutoComplete.ContactEntry;
import com.zimbra.soap.ZimbraSoapContext;

public class AutoComplete extends MailDocumentHandler {

    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
    	ZimbraSoapContext zsc = getZimbraSoapContext(context);
		Account account = getRequestedAccount(getZimbraSoapContext(context));

		if (!canAccessAccount(zsc, account))
			throw ServiceException.PERM_DENIED("can not access account");

		String n = request.getAttribute(MailConstants.A_NAME);
		while (n.endsWith("*"))
			n = n.substring(0, n.length() - 1);
        int limit = (int) request.getAttributeLong(AccountConstants.A_LIMIT, 20);
		ArrayList<Integer> folders = csvToArray(request.getAttribute(MailConstants.A_FOLDERS, null));

		ContactAutoComplete autoComplete = new ContactAutoComplete(account.getId());
		AutoCompleteResult result = autoComplete.query(n, folders, limit);
		Element response = zsc.createElement(MailConstants.AUTO_COMPLETE_RESPONSE);
		toXML(response, result);
		
		return response;
	}

	@Override
	public boolean needsAuth(Map<String, Object> context) {
		return true;
	}
	
	private ArrayList<Integer> csvToArray(String csv) {
		if (csv == null)
			return null;
		ArrayList<Integer> array = new ArrayList<Integer>();
		for (String f : csv.split(",")) {
			array.add(Integer.parseInt(f));
		}
		return array;
	}
	
	private void toXML(Element response, AutoCompleteResult result) {
		response.addAttribute(MailConstants.A_CANBECACHED, result.canBeCached);
		for (ContactEntry entry : result.entries) {
	        Element cn = response.addElement(MailConstants.E_CONTACT);
            cn.addKeyValuePair(MailConstants.A_EMAIL, entry.getEmail(), MailConstants.E_ATTRIBUTE, MailConstants.A_ATTRIBUTE_NAME);
            cn.addKeyValuePair(MailConstants.A_RANKING, ""+entry.getRanking(), MailConstants.E_ATTRIBUTE, MailConstants.A_ATTRIBUTE_NAME);
            cn.addKeyValuePair(MailConstants.A_FOLDER, ""+entry.getFolderId(), MailConstants.E_ATTRIBUTE, MailConstants.A_ATTRIBUTE_NAME);
		}
	}
}
