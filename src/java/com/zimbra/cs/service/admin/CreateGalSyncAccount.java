/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010 Zimbra, Inc.
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
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.ZAttrProvisioning.GalMode;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.TargetType;
import com.zimbra.cs.account.ldap.LdapUtil;
import com.zimbra.cs.mailbox.ACL;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
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
	    String domainStr = request.getAttribute(AdminConstants.E_DOMAIN);
	    String typeStr = request.getAttribute(AdminConstants.A_TYPE);
	    
	    Element acctElem = request.getElement(AdminConstants.E_ACCOUNT);
	    String acctKey = acctElem.getAttribute(AdminConstants.A_BY);
        String acctValue = acctElem.getText();

	    String password = request.getAttribute(AdminConstants.E_PASSWORD, null);
	    String folder = request.getAttribute(AdminConstants.E_FOLDER, null);

	    GalMode type = GalMode.fromString(typeStr);
	    Domain domain = prov.getDomainByName(domainStr);
	    
	    if (domain == null)
	    	throw AccountServiceException.NO_SUCH_DOMAIN(domainStr);
	    
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
	    	// there should be one zimbra gal sync account per domain
	    	if (type == GalMode.zimbra) {
	    		for (String acctId : domain.getGalAccountId()) {
		    		Account acct = prov.getAccountById(acctId);
		    		if (acct != null) {
		    			for (DataSource ds : prov.getAllDataSources(acct)) {
		    				if (!ds.getType().equals(DataSource.Type.gal))
		    					continue;
		    				if (ds.getAttr(Provisioning.A_zimbraGalType).compareTo("zimbra") == 0)
		    					throw AccountServiceException.ACCOUNT_EXISTS(acct.getName());
		    			}
		    		}
	    		}
	    	}
		    // XXX revisit
		    checkDomainRightByEmail(zsc, acctValue, Admin.R_createAccount);
		    Map<String,Object> accountAttrs = new HashMap<String,Object>();
		    StringUtil.addToMultiMap(accountAttrs, Provisioning.A_zimbraIsSystemResource, LdapUtil.LDAP_TRUE);
		    StringUtil.addToMultiMap(accountAttrs, Provisioning.A_zimbraHideInGal, LdapUtil.LDAP_TRUE);
            StringUtil.addToMultiMap(accountAttrs, Provisioning.A_zimbraContactMaxNumEntries, "0");
		    checkSetAttrsOnCreate(zsc, TargetType.account, acctValue, accountAttrs);
	    	account = prov.createAccount(acctValue, password, accountAttrs);
	    }

	    if (!Provisioning.onLocalServer(account)) {
	        String host = account.getMailHost();
	        Server server = prov.getServerByName(host);
	        return proxyRequest(request, context, server);
	    }
	    
	    String acctName = account.getName();
	    String acctId = account.getId();
    	HashSet<String> galAcctIds = new HashSet<String>();
    	galAcctIds.addAll(Arrays.asList(domain.getGalAccountId()));
    	if (!galAcctIds.contains(acctId)) {
    		galAcctIds.add(acctId);
	    	domain.setGalAccountId(galAcctIds.toArray(new String[0]));
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
	    		throw MailServiceException.ALREADY_EXISTS("data source " + ds.getName() + " already contains folder " + folder);
	    
	    
        mbox.grantAccess(null, folderId, domain.getId(), ACL.GRANTEE_DOMAIN, ACL.stringToRights("r"), null);
	    
	    // create datasource
	    Map<String,Object> attrs = AdminService.getAttrs(request, true);
	    try {
    		attrs.put(Provisioning.A_zimbraGalType, type.name());
	    	attrs.put(Provisioning.A_zimbraDataSourceFolderId, "" + folderId);
	    	if (!attrs.containsKey(Provisioning.A_zimbraDataSourceEnabled))
	    		attrs.put(Provisioning.A_zimbraDataSourceEnabled, LdapUtil.LDAP_TRUE);
	    	if (!attrs.containsKey(Provisioning.A_zimbraGalStatus))
	    		attrs.put(Provisioning.A_zimbraGalStatus, "enabled");
	    	prov.createDataSource(account, DataSource.Type.gal, name, attrs);
	    } catch (ServiceException e) {
	    	ZimbraLog.gal.error("error creating datasource for GalSyncAccount", e);
	    	throw e;
	    }
	    
        ZimbraLog.security.info(ZimbraLog.encodeAttrs(
                new String[] {"cmd", "CreateGalSyncAccount", "name", acctName} ));         

	    Element response = zsc.createElement(AdminConstants.CREATE_GAL_SYNC_ACCOUNT_RESPONSE);
        ToXML.encodeAccount(response, account, false, emptySet, null);
	    return response;
	}
	
	private static final Set<String> emptySet = Collections.emptySet();
    
    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        // XXX revisit
        relatedRights.add(Admin.R_createAccount);
        notes.add(String.format(AdminRightCheckPoint.Notes.MODIFY_ENTRY, 
                Admin.R_modifyAccount.getName(), "account"));
    }
}