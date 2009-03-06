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
package com.zimbra.cs.datasource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.NamingException;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.GalContact;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.SearchGalResult;
import com.zimbra.cs.account.gal.GalOp;
import com.zimbra.cs.account.gal.GalParams;
import com.zimbra.cs.account.ldap.LdapGalMapRules;
import com.zimbra.cs.account.ldap.LdapUtil;
import com.zimbra.cs.account.ldap.ZimbraLdapContext;
import com.zimbra.cs.db.DbDataSource;
import com.zimbra.cs.db.DbDataSource.DataSourceItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mime.ParsedContact;

public class GalImport extends MailItemImport {

	public GalImport(DataSource ds) throws ServiceException {
    	super(ds);
    }
    
	public void importData(List<Integer> folderIds, boolean fullSync)
			throws ServiceException {
    	mbox.beginTrackingSync();
    	if (folderIds == null)
    		importGal(dataSource.getFolderId());
    	else
    		for (int fid : folderIds)
    			importGal(fid);
	}

	public void test() throws ServiceException {
		try {
			searchGal();
		} catch (NamingException e) {
			throw ServiceException.FAILURE("Error executing gal search", e);
		} catch (IOException e) {
			throw ServiceException.FAILURE("Error executing gal search", e);
		}
	}

	private void importGal(int fid) throws ServiceException {
    	OperationContext octxt = new Mailbox.OperationContext(mbox);
    	SearchGalResult result = null;
    	try {
    		result = searchGal();
    	} catch (Exception e) {
    		ZimbraLog.gal.error("Error executing gal search", e);
    		return;
    	}
        HashMap<String,DataSourceItem> allMappings = new HashMap<String,DataSourceItem>();
        for (DataSourceItem dsItem : DbDataSource.getAllMappings(getDataSource()))
        	allMappings.put(dsItem.remoteId, dsItem);
        for (GalContact contact : result.getMatches()) {
        	try {
                processContact(octxt, contact, fid);
                allMappings.remove(contact.getId());
        	} catch (Exception e) {
        		ZimbraLog.gal.warn("Ignoring error importing gal contact "+contact.getId(), e);
        	}
        }
        if (allMappings.size() == 0)
        	return;
        
        ArrayList<Integer> deleted = new ArrayList<Integer>();
        int[] deletedIds = new int[allMappings.size()];
        int i = 0;
        for (DataSourceItem dsItem : allMappings.values()) {
        	deleted.add(dsItem.itemId);
        	deletedIds[i++] = dsItem.itemId;
        }
    	try {
        	mbox.delete(octxt, deletedIds, MailItem.TYPE_CONTACT, null);
    	} catch (ServiceException e) {
    		ZimbraLog.gal.warn("Ignoring error deleting gal contacts", e);
    	}
        DbDataSource.deleteMappings(getDataSource(), deleted);
	}
	
	private SearchGalResult searchGal() throws ServiceException, NamingException, IOException  {
		String zimbraGalAccountId = null;
		Provisioning prov = Provisioning.getInstance();
		String[] acctIds = prov.getConfig().getGalAccountId();
		if (acctIds.length > 0)
			zimbraGalAccountId = acctIds[0];
		if (zimbraGalAccountId != null && mbox.getAccountId().equals(zimbraGalAccountId))
			return searchZimbraGal();
		else
			return searchExternalGal();
	}
	private SearchGalResult searchExternalGal() throws ServiceException, NamingException, IOException  {
		DataSource ds = getDataSource();
		GalOp galOp = GalOp.sync;
        GalParams.ExternalGalParams galParams = new GalParams.ExternalGalParams(ds, galOp);
        String[] attrs = ds.getMultiAttr(Provisioning.A_zimbraGalLdapAttrMap);
        if (attrs == null)
        	attrs = Provisioning.getInstance().getConfig().getMultiAttr(Provisioning.A_zimbraGalLdapAttrMap);
        LdapGalMapRules rules = new LdapGalMapRules(attrs);
        return LdapUtil.searchLdapGal(galParams,
                galOp,
                "", 
                0, 
                rules, 
                null,
                null);
	}
	
	private SearchGalResult searchZimbraGal() throws ServiceException {
        SearchGalResult result = SearchGalResult.newSearchGalResult(null);
		DataSource ds = getDataSource();
        String[] attrs = ds.getMultiAttr(Provisioning.A_zimbraGalLdapAttrMap);
        if (attrs.length == 0)
        	attrs = Provisioning.getInstance().getConfig().getMultiAttr(Provisioning.A_zimbraGalLdapAttrMap);
        LdapGalMapRules rules = new LdapGalMapRules(attrs);
        ZimbraLdapContext zlc = null;
        try {
            zlc = new ZimbraLdapContext(false);
            LdapUtil.searchGal(zlc,
                               0,
                               "", 
                               ds.getAttr(Provisioning.A_zimbraGalLdapFilter), 
                               0,
                               rules,
                               null,
                               result);
        } finally {
            ZimbraLdapContext.closeContext(zlc);
        }
        return result;
	}
	
	private void processContact(OperationContext octxt, GalContact contact, int fid) throws ServiceException {
		Map<String,Object> attrs = contact.getAttrs();
		HashMap<String,String> contactAttrs = new HashMap<String,String>();
		for (String key : attrs.keySet()) {
			Object val = attrs.get(key);
			if (val instanceof String)
				contactAttrs.put(key, (String)val);
			else if (val instanceof String[])
				contactAttrs.put(key, ((String[])val)[0]);
		}
		String id = contact.getId();
		ZimbraLog.gal.debug("processing gal contact "+id);
		DataSourceItem dsItem = DbDataSource.getReverseMapping(getDataSource(), id);
        String modifiedDate = (String) contact.getAttrs().get("modifyTimeStamp");
    	if (dsItem.itemId == 0) {
    		ZimbraLog.gal.debug("creating new contact "+id);
    		dsItem.remoteId = id;
            ParsedContact pc = new ParsedContact(contactAttrs);
            dsItem.itemId = mbox.createContact(octxt, pc, fid, null).getId();
    		DbDataSource.addMapping(getDataSource(), dsItem);
    	} else {
    		String syncDate = mbox.getContactById(octxt, dsItem.itemId).get("modifyTimeStamp");
            if (syncDate == null || !syncDate.equals(modifiedDate)) {
        		ZimbraLog.gal.debug("modifying contact "+id);
                ParsedContact pc = new ParsedContact(contactAttrs);
                mbox.modifyContact(octxt, dsItem.itemId, pc);
            }
    	}
	}
	
	public String getUrl() {
		return getDataSource().getAttr(Provisioning.A_zimbraGalLdapURL);
	}
	public String getFilter() {
		return getDataSource().getAttr(Provisioning.A_zimbraGalLdapFilter);
	}
	public String getSearchBase() {
		return getDataSource().getAttr(Provisioning.A_zimbraGalLdapSearchBase);
	}
	public String getAuthType() {
		return getDataSource().getAttr(Provisioning.A_zimbraGalLdapAuthMech);
	}
	public String getBindDN() {
		return getDataSource().getAttr(Provisioning.A_zimbraGalLdapBindDn);
	}
	public String getPassword() {
		return getDataSource().getAttr(Provisioning.A_zimbraGalLdapBindPassword);
	}
	public String getKerberos5Principal() {
		return getDataSource().getAttr(Provisioning.A_zimbraGalLdapKerberos5Principal);
	}
	public String getKerberos5Keytab() {
		return getDataSource().getAttr(Provisioning.A_zimbraGalLdapKerberos5Keytab);
	}
}
