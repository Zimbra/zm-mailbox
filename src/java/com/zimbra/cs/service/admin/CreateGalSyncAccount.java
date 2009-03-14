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
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.TargetType;
import com.zimbra.cs.account.gal.GalNamedFilter;
import com.zimbra.cs.account.gal.GalOp;
import com.zimbra.cs.account.ldap.LdapProvisioning;
import com.zimbra.cs.account.ldap.LdapUtil;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.service.account.ToXML;
import com.zimbra.soap.ZimbraSoapContext;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CreateGalSyncAccount extends AdminDocumentHandler {

    public boolean domainAuthSufficient(Map<String, Object> context) {
        return true;
    }

	public Element handle(Element request, Map<String, Object> context) throws ServiceException {

        ZimbraSoapContext zsc = getZimbraSoapContext(context);
	    Provisioning prov = Provisioning.getInstance();

	    String name = request.getAttribute(AdminConstants.E_NAME);
	    String domainStr = request.getAttribute(AdminConstants.E_DOMAIN, null);
	    
	    Element acctElem = request.getElement(AdminConstants.E_ACCOUNT);
	    String acctKey = acctElem.getAttribute(AdminConstants.A_BY);
        String acctValue = acctElem.getText();

	    String password = request.getAttribute(AdminConstants.E_PASSWORD, null);
	    String folder = request.getAttribute(AdminConstants.E_FOLDER, null);

	    Domain domain = null;
	    if (domain != null)
	    	prov.getDomainByName(domainStr);
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
	    	// there should be one zimbra gal sync account
	    	if (domain == null) {
	    		String[] galAcctIds = prov.getConfig().getGalAccountId();
	    		if (galAcctIds.length == 1 && prov.getAccountById(galAcctIds[0]) != null)
		    		throw AccountServiceException.ACCOUNT_EXISTS(acctValue);
	    	}
		    // XXX revisit
		    checkDomainRightByEmail(zsc, acctValue, Admin.R_createAccount);
		    Map<String,Object> accountAttrs = new HashMap<String,Object>();
		    StringUtil.addToMultiMap(accountAttrs, Provisioning.A_zimbraIsSystemResource, LdapUtil.LDAP_TRUE);
		    StringUtil.addToMultiMap(accountAttrs, Provisioning.A_zimbraHideInGal, LdapUtil.LDAP_TRUE);
		    checkSetAttrsOnCreate(zsc, TargetType.account, acctValue, accountAttrs);
	    	account = prov.createAccount(acctValue, password, accountAttrs);
	    }

	    String acctName = account.getName();
	    String acctId = account.getId();
	    if (domain == null)
	    	prov.getConfig().setGalAccountId(new String[] { acctId });
	    else {
	    	HashSet<String> galAcctIds = new HashSet<String>();
	    	galAcctIds.addAll(Arrays.asList(domain.getGalAccountId()));
	    	if (!galAcctIds.contains(acctId)) {
	    		galAcctIds.add(acctId);
		    	domain.setGalAccountId(galAcctIds.toArray(new String[0]));
	    	}
	    }
	    
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
	    	if (domain == null)
	    		getDefaultZimbraGalParams(attrs);
	    	attrs.put(Provisioning.A_zimbraDataSourceFolderId, "" + folderId);
	    	if (!attrs.containsKey(Provisioning.A_zimbraDataSourceEnabled))
	    		attrs.put(Provisioning.A_zimbraDataSourceEnabled, LdapUtil.LDAP_TRUE);
	    	if (!attrs.containsKey(Provisioning.A_zimbraGalStatus))
	    		attrs.put(Provisioning.A_zimbraGalStatus, "active");
	    	prov.createDataSource(account, DataSource.Type.gal, name, attrs);
	    } catch (ServiceException e) {
	    	ZimbraLog.gal.error("error creating datasource for GalSyncAccount", e);
	    	throw e;
	    }
	    
        ZimbraLog.security.info(ZimbraLog.encodeAttrs(
                new String[] {"cmd", "CreateGalSyncAccount", "name", acctName} ));         

	    Element response = zsc.createElement(AdminConstants.CREATE_GAL_SYNC_ACCOUNT_RESPONSE);
        ToXML.encodeAccountOld(response, account, false, emptySet);
	    return response;
	}
	
	private static final Set<String> emptySet = Collections.emptySet();
	
    @Override
    protected void docRights(List<AdminRight> relatedRights, List<String> notes) {
    	// XXX revisit
        relatedRights.add(Admin.R_createAccount);
        notes.add(String.format(sDocRightNotesModifyEntry, Admin.R_modifyAccount.getName(), "account"));
    }
    
    private void getDefaultZimbraGalParams(Map<String,Object> attrs) throws ServiceException {
    	if (attrs.get(Provisioning.A_zimbraGalLdapFilter) == null) {
    		String filter = LdapProvisioning.getFilterDef(GalNamedFilter.getZimbraAcountFilter(GalOp.sync));
    		filter = "(&(!(zimbraHideInGal=TRUE))" + filter + ")";
    		attrs.put(Provisioning.A_zimbraGalLdapFilter, filter.replaceAll("%s\\*", ""));
    	}
    }
}