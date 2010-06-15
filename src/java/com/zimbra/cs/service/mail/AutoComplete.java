/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.mail;

import java.util.ArrayList;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.ContactAutoComplete;
import com.zimbra.cs.mailbox.ContactAutoComplete.AutoCompleteResult;
import com.zimbra.cs.mailbox.ContactAutoComplete.ContactEntry;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.soap.ZimbraSoapContext;

public class AutoComplete extends MailDocumentHandler {

    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
    	ZimbraSoapContext zsc = getZimbraSoapContext(context);
    	Account account = getRequestedAccount(getZimbraSoapContext(context));
        
    	String n = request.getAttribute(MailConstants.A_NAME);
    	while (n.endsWith("*"))
    	    n = n.substring(0, n.length() - 1);
        
    	Provisioning.GAL_SEARCH_TYPE type = getSearchType(request.getAttribute(MailConstants.A_TYPE, "account"));
        int limit = account.getContactAutoCompleteMaxResults();
        
		AutoCompleteResult result = query(request, zsc, account, false, n, limit, type);		
		Element response = zsc.createElement(MailConstants.AUTO_COMPLETE_RESPONSE);
		toXML(response, result, zsc.getAuthtokenAccountId());
		
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
		
	protected AutoCompleteResult query(Element request, ZimbraSoapContext zsc, Account account, boolean excludeGal, String name, int limit, Provisioning.GAL_SEARCH_TYPE type) throws ServiceException {
       if (!canAccessAccount(zsc, account))
            throw ServiceException.PERM_DENIED("can not access account");
       
       ArrayList<Integer> folders = csvToArray(request.getAttribute(MailConstants.A_FOLDERS, null));
       ContactAutoComplete autoComplete = new ContactAutoComplete(account.getId());
       autoComplete.setSearchType(type);
       boolean includeGal = !excludeGal && request.getAttributeBool(MailConstants.A_INCLUDE_GAL, autoComplete.includeGal());
       autoComplete.setIncludeGal(includeGal);
       return autoComplete.query(name, folders, limit);
	}
	
	protected void toXML(Element response, AutoCompleteResult result, String authAccountId) {
		response.addAttribute(MailConstants.A_CANBECACHED, result.canBeCached);
		for (ContactEntry entry : result.entries) {
	        Element cn = response.addElement(MailConstants.E_MATCH);
	        cn.addAttribute(MailConstants.A_EMAIL, entry.getEmail());
	        cn.addAttribute(MailConstants.A_MATCH_TYPE, getType(entry));
            cn.addAttribute(MailConstants.A_RANKING, Integer.toString(entry.getRanking()));
            ItemId id = entry.getId();
            if (id != null)
            	cn.addAttribute(MailConstants.A_ID, id.toString(authAccountId));
            int folderId = entry.getFolderId();
            if (folderId > 0)
            	cn.addAttribute(MailConstants.A_FOLDER, Integer.toString(folderId));
            if (entry.isDlist())
            	cn.addAttribute(MailConstants.A_DISPLAYNAME, entry.getDisplayName());
		}
	}
	
	protected Provisioning.GAL_SEARCH_TYPE getSearchType(String typeStr) throws ServiceException {
        Provisioning.GAL_SEARCH_TYPE type;
        if (typeStr.equals("all"))
            type = Provisioning.GAL_SEARCH_TYPE.ALL;
        else if (typeStr.equals("account"))
            type = Provisioning.GAL_SEARCH_TYPE.USER_ACCOUNT;
        else if (typeStr.equals("resource"))
            type = Provisioning.GAL_SEARCH_TYPE.CALENDAR_RESOURCE;
        else
            throw ServiceException.INVALID_REQUEST("Invalid search type: " + typeStr, null);
        return type;
	}
	
	private String getType(ContactEntry entry) {
		if (entry.getFolderId() == ContactAutoComplete.FOLDER_ID_GAL)
			return "gal";
		else if (entry.getFolderId() == ContactAutoComplete.FOLDER_ID_UNKNOWN)
			return "rankingTable";
		else if (entry.isDlist())
			return "group";
		else
			return "contact";
	}
}
