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
package com.zimbra.cs.gal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.NamingException;

import com.zimbra.common.mailbox.ContactConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.DateUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.GalContact;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.SearchGalResult;
import com.zimbra.cs.account.ldap.LdapUtil;
import com.zimbra.cs.datasource.MailItemImport;
import com.zimbra.cs.db.DbDataSource;
import com.zimbra.cs.db.DbDataSource.DataSourceItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Contact;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mime.ParsedContact;

public class GalImport extends MailItemImport {

	public GalImport(DataSource ds) throws ServiceException {
    	super(ds);
    }
    
	public void importData(List<Integer> folderIds, boolean fullSync)
			throws ServiceException {
    	if (folderIds == null)
    		importGal(dataSource.getFolderId(), fullSync, false);
    	else
    		for (int fid : folderIds)
    			importGal(fid, fullSync, false);
	}

	public void test() throws ServiceException {
		try {
			searchGal(null, SearchGalResult.newSearchGalResult(null), false);
		} catch (NamingException e) {
			throw ServiceException.FAILURE("Error executing gal search", e);
		} catch (IOException e) {
			throw ServiceException.FAILURE("Error executing gal search", e);
		}
	}

	private static final String TYPE = "t";
	private static final String FOLDER = "f";
	public static final String SYNCTOKEN = "st";
	
	private void setStatus(boolean success) throws ServiceException {
		Date now = new Date();
		DataSource ds = getDataSource();
		Map<String,Object> attrs = new HashMap<String,Object>();
		String attr = success ? 
				Provisioning.A_zimbraGalLastSuccessfulSyncTimestamp :
				Provisioning.A_zimbraGalLastFailedSyncTimestamp;
		attrs.put(attr, DateUtil.toGeneralizedTime(now));
		Provisioning.getInstance().modifyAttrs(ds, attrs);
	}
	
	public void importGal(int fid, boolean fullSync, boolean force) throws ServiceException {
    	mbox.beginTrackingSync();
		DataSource ds = getDataSource();
		DataSourceItem folderMapping = DbDataSource.getMapping(ds, fid);
		if (folderMapping.md == null) {
			folderMapping.itemId = fid;
			folderMapping.md = new Metadata();
			folderMapping.md.put(TYPE, FOLDER);
			DbDataSource.addMapping(ds, folderMapping);
		}
		String syncToken = fullSync ? "" : folderMapping.md.get(SYNCTOKEN, "");
        HashMap<String,DataSourceItem> allMappings = new HashMap<String,DataSourceItem>();
        if (fullSync || force)
	        for (DataSourceItem dsItem : DbDataSource.getAllMappings(ds))
	        	if (dsItem.md == null || dsItem.md.get(TYPE, null) == null)  // non-folder items
	        		allMappings.put(dsItem.remoteId, dsItem);
    	OperationContext octxt = new OperationContext(mbox);
    	SearchGalResult result = SearchGalResult.newSearchGalResult(new GalSearchVisitor(mbox, allMappings, fid, force));
    	try {
    		searchGal(syncToken, result, true);
    	} catch (Exception e) {
    		setStatus(false);
    		ZimbraLog.gal.error("Error executing gal search", e);
    		return;
    	}

        folderMapping.md.put(SYNCTOKEN, result.getToken());
        DbDataSource.updateMapping(ds, folderMapping);
        if (allMappings.size() == 0 || !fullSync) {
    		setStatus(true);
        	return;
        }
        
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
		setStatus(true);
	}
	
	private void searchGal(String syncToken, SearchGalResult result, boolean fetchGroupMembers) throws ServiceException, NamingException, IOException {
		ZimbraLog.gal.debug("searchGal: "+syncToken);
		DataSource ds = getDataSource();
		GalSearchParams params = new GalSearchParams(ds);
		params.setGalResult(result);
		params.setToken(syncToken);
		params.setQuery("*");
		for (String attr : ZIMBRA_ATTRS)
        	params.getConfig().getRules().add(attr+"="+attr);
        params.getConfig().getRules().setFetchGroupMembers(fetchGroupMembers);
        LdapUtil.galSearch(params);
	}
	private static String[] ZIMBRA_ATTRS = {
		"zimbraAccountCalendarUserType",
		"zimbraCalResType",
		"zimbraCalResLocationDisplayName",
		"zimbraCalResCapacity",
		"zimbraCalResContactEmail"
	};
	
	private class GalSearchVisitor implements GalContact.Visitor {
		Mailbox mbox;
		OperationContext octxt;
		Map<String,DataSourceItem> mappings;
		int fid;
		boolean force;
		
		private GalSearchVisitor(Mailbox mbox, Map<String,DataSourceItem> mappings, int fid, boolean force) throws ServiceException {
			this.mbox = mbox;
			this.octxt = new OperationContext(mbox);
			this.mappings = mappings;
			this.fid = fid;
			this.force = force;
		}
		
		private String[] FILE_AS_STR_KEYS = {
			ContactConstants.A_fullName,
			ContactConstants.A_email,
			ContactConstants.A_email2,
			ContactConstants.A_email3
		};
		
		private void addFileAsStr(Map<String,Object> attrs) {
	        for (String key : FILE_AS_STR_KEYS) {
	        	Object fileAsStr = attrs.get(key);
	        	if (fileAsStr != null && fileAsStr instanceof String) {
	        		attrs.put(ContactConstants.A_fileAs, ContactConstants.FA_EXPLICIT+":"+(String)fileAsStr);
	        		return;
	        	}
	        }
		}
		
		public void visit(GalContact contact) throws ServiceException {
			Map<String,Object> attrs = contact.getAttrs();
			String id = contact.getId();
			attrs.put("dn", id);
			ZimbraLog.gal.debug("processing gal contact "+id);
			DataSourceItem dsItem = DbDataSource.getReverseMapping(getDataSource(), id);
	        String modifiedDate = (String) contact.getAttrs().get("modifyTimeStamp");
	        addFileAsStr(attrs);
	    	if (dsItem.itemId == 0) {
	    		ZimbraLog.gal.debug("creating new contact "+id);
	    		dsItem.remoteId = id;
	            ParsedContact pc = new ParsedContact(attrs);
	            dsItem.itemId = mbox.createContact(octxt, pc, fid, null).getId();
	    		DbDataSource.addMapping(getDataSource(), dsItem);
	    	} else {
	    		String syncDate = mbox.getContactById(octxt, dsItem.itemId).get("modifyTimeStamp");
	            if (force || syncDate == null || !syncDate.equals(modifiedDate)) {
	        		ZimbraLog.gal.debug("modifying contact "+id);
	                ParsedContact pc = new ParsedContact(attrs);
	                mbox.modifyContact(octxt, dsItem.itemId, pc);
	            }
	    	}
	    	mappings.remove(id);
		}
	}
}
