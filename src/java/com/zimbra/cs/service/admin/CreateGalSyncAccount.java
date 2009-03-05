/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009 Zimbra, Inc.
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
package com.zimbra.cs.service.admin;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.TargetType;
import com.zimbra.cs.account.ldap.LdapUtil;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.service.account.ToXML;
import com.zimbra.soap.ZimbraSoapContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CreateGalSyncAccount extends AdminDocumentHandler {

    public boolean domainAuthSufficient(Map<String, Object> context) {
        return true;
    }

	public Element handle(Element request, Map<String, Object> context) throws ServiceException {

        ZimbraSoapContext zsc = getZimbraSoapContext(context);
	    Provisioning prov = Provisioning.getInstance();

	    String name = request.getAttribute(AdminConstants.E_NAME);
	    
	    Element acctElem = request.getElement(AdminConstants.E_ACCOUNT);
	    String acctKey = acctElem.getAttribute(AdminConstants.A_BY);
        String acctValue = acctElem.getText();

	    String password = request.getAttribute(AdminConstants.E_PASSWORD, null);
	    String folder = request.getAttribute(AdminConstants.E_FOLDER, null);

	    Account account = null;
	    try {
	    	account = prov.get(AccountBy.fromString(acctKey), acctValue, zsc.getAuthToken());
	    } catch (ServiceException se) {
	    	ZimbraLog.gal.warn("error checking GalSyncAccount", se);
	    }

	    // create the system account if not already exists.
	    if (account == null) {
	    	if (AccountBy.fromString(acctKey) != AccountBy.name)
	    		throw AccountServiceException.NO_SUCH_ACCOUNT(acctValue);
		    // XXX revisit
		    checkDomainRightByEmail(zsc, acctValue, Admin.R_createAccount);
		    Map<String,Object> accountAttrs = new HashMap<String,Object>();
		    StringUtil.addToMultiMap(accountAttrs, Provisioning.A_zimbraIsSystemResource, LdapUtil.LDAP_TRUE);
		    checkSetAttrsOnCreate(zsc, TargetType.account, acctValue, accountAttrs);
	    	account = prov.createAccount(acctValue, password, accountAttrs);
	    }

	    String acctName = account.getName();
	    
	    // create folder if not already exists.
	    if (folder == null)
	    	folder = "/Contacts";
	    else if (folder.length() > 0 && folder.charAt(0) != '/')
	    	folder = "/" + folder;
	    Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
	    Folder contactFolder = null;
	    try {
	    	contactFolder = mbox.getFolderByPath(null, folder);
	    } catch (MailServiceException.NoSuchItemException e) {
	    	contactFolder = mbox.createFolder(null, folder, (byte)0, MailItem.TYPE_CONTACT);
	    }
	    
	    int folderId = contactFolder.getId();
	    
	    // check if there is another datasource already that maps to the same contact folder.
	    for (DataSource ds : account.getAllDataSources())
	    	if (ds.getFolderId() == folderId)
	    		throw MailServiceException.ALREADY_EXISTS(ds.getName());
	    
	    // create datasource
	    Map<String,Object> attrs = AdminService.getAttrs(request, true);
	    try {
	    	attrs.put(Provisioning.A_zimbraDataSourceFolderId, "" + folderId);
	    	prov.createDataSource(account, DataSource.Type.gal, name, attrs);
	    } catch (ServiceException e) {
	    	ZimbraLog.gal.error("error creating datasource for GalSyncAccount", e);
	    	throw e;
	    }
	    
        ZimbraLog.security.info(ZimbraLog.encodeAttrs(
                new String[] {"cmd", "CreateGalSyncAccount", "name", acctName} ));         

	    Element response = zsc.createElement(AdminConstants.CREATE_GAL_SYNC_ACCOUNT_RESPONSE);
        ToXML.encodeAccountOld(response, account);
	    return response;
	}
	
    @Override
    protected void docRights(List<AdminRight> relatedRights, List<String> notes) {
    	// XXX revisit
        relatedRights.add(Admin.R_createAccount);
        notes.add(String.format(sDocRightNotesModifyEntry, Admin.R_modifyAccount.getName(), "account"));
    }
}