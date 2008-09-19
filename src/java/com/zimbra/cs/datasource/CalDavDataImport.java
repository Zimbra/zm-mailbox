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
package com.zimbra.cs.datasource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.dav.client.CalDavClient;
import com.zimbra.cs.dav.client.CalDavClient.Appointment;
import com.zimbra.cs.db.DbDataSource;
import com.zimbra.cs.db.DbDataSource.DataSourceItem;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.mailbox.MailItem;

public class CalDavDataImport extends MailItemImport {

	private static final String METADATA_KEY_TYPE = "t";
	private static final String METADATA_TYPE_FOLDER = "f";
	private static final String METADATA_TYPE_APPOINTMENT = "a";
	private static final String METADATA_KEY_ETAG = "e";
	
	private CalDavClient mClient;
	
    public CalDavDataImport(DataSource ds) {
    	super(ds);
    }
	
    public void importData(List<Integer> folderIds, boolean fullSync)
    throws ServiceException {
    	try {
        	if (folderIds == null) {
        		syncFolders();
            	folderIds = new ArrayList<Integer>();
            	folderIds.add(getDataSource().getFolderId());
        	}
        	Mailbox mbox = getDataSource().getMailbox();
        	OperationContext octxt = new Mailbox.OperationContext(mbox);
        	for (int fid : folderIds) {
            	Folder syncFolder = mbox.getFolderById(octxt, fid);
        		sync(mbox, octxt, syncFolder);
        	}
    	} catch (DavException e) {
    	} catch (IOException e) {
    	}
    }

    public String test() throws ServiceException {
    	try {
    		DataSource ds = getDataSource();
    		mClient.setCredential(ds.getUsername(), ds.getDecryptedPassword());
    		mClient.login(getPrincipalUrl());
    	} catch (Exception e) {
    		return e.getMessage();
    	}
    	return null;
    }

    private String getPrincipalUrl() {
    	DataSource ds = getDataSource();
    	String attrs[] = ds.getMultiAttr(Provisioning.A_zimbraDataSourceAttribute);
    	for (String a : attrs) {
    		if (a.startsWith("p:"))
    			return a.substring(2);
    	}
    	return null;
    }

    private String getTargetUrl() {
    	DataSource ds = getDataSource();
    	DataSource.ConnectionType ctype = ds.getConnectionType();
    	StringBuilder url = new StringBuilder();

    	switch (ctype) {
    	case ssl:
    		url.append("https://");
    		break;
    	case cleartext:
    	default:
    		url.append("http://");
    	break;
    	}
    	url.append(ds.getHost()).append(":").append(ds.getPort());
    	return url.toString();
    }

    private CalDavClient getClient() throws ServiceException, IOException, DavException {
    	if (mClient == null) {
    		DataSource ds = getDataSource();
        	mClient = new CalDavClient(getTargetUrl());
    		mClient.setCredential(ds.getUsername(), ds.getDecryptedPassword());
    		mClient.login(getPrincipalUrl());
    	}
    	return mClient;
    }
    
    private enum Status { created, deleted, modified };
    private static class RemoteItem {
    	Status status;
    }
    private static class RemoteCalendarItem extends RemoteItem {
    	public RemoteCalendarItem(String h, String e) { href = h; etag = e; }
    	String href;
    	String etag;
    }
    private void syncFolders() throws ServiceException, IOException, DavException {
    	DataSource ds = getDataSource();
		CalDavClient client = getClient();
		Map<String,String> calendars = client.getCalendars();
		for (String name : calendars.keySet()) {
			String url = calendars.get(name);
			DataSourceItem f = DbDataSource.getReverseMapping(ds.getMailbox(), ds, url);
			if (f.itemId == 0) {
				// add new mapping
			} else if (f.md == null) {
	    		ZimbraLog.datasource.warn("syncFolders: empty metadata for item %d", f.itemId);
			}
		}
    }
    private void pushDelete(int itemId) throws ServiceException {
    	DataSource ds = getDataSource();
    	DataSourceItem item = DbDataSource.getMapping(ds.getMailbox(), ds, itemId);
    	if (item.md == null) {
    		ZimbraLog.datasource.warn("pushDelete: empty metadata for item %d", itemId);
    		return;
    	}
    	String type = item.md.get(METADATA_KEY_TYPE);
    	if (METADATA_TYPE_FOLDER.equals(type)) {
    		// delete remote folder
    	} else if (METADATA_TYPE_APPOINTMENT.equals(type)) {
    		// delete remote appointment
    	} else {
    		ZimbraLog.datasource.warn("pushDelete: unrecognized item type for %d: %s", itemId, type);
    		return;
    	}
    }
    private void pushModify(MailItem mitem) throws ServiceException {
    	int itemId = mitem.getId();
    	DataSource ds = getDataSource();
    	DataSourceItem item = DbDataSource.getMapping(ds.getMailbox(), ds, itemId);
    	if (item.md == null) {
    		ZimbraLog.datasource.warn("pushModify: empty metadata for item %d", itemId);
    		return;
    	}
    	String type = item.md.get(METADATA_KEY_TYPE);
    	if (METADATA_TYPE_FOLDER.equals(type)) {
    		if (mitem.getType() != MailItem.TYPE_FOLDER) {
        		ZimbraLog.datasource.warn("pushModify: item type doesn't match in metadata for item %d", itemId);
        		return;
    		}
    		// detect and push rename
    	} else if (METADATA_TYPE_APPOINTMENT.equals(type)) {
    		if (mitem.getType() != MailItem.TYPE_APPOINTMENT) {
        		ZimbraLog.datasource.warn("pushModify: item type doesn't match in metadata for item %d", itemId);
        		return;
    		}
    		// push modified appt
    	} else {
    		ZimbraLog.datasource.warn("pushModify: unrecognized item type for %d: %s", itemId, type);
    		return;
    	}
    }
    private List<RemoteItem> getRemoteItems(Folder folder) throws ServiceException, IOException {
		ZimbraLog.datasource.debug("Refresh folder %s", folder.getPath());
    	DataSource ds = getDataSource();
    	Mailbox mbox = ds.getMailbox();
    	DataSourceItem item = DbDataSource.getMapping(mbox, ds, folder.getId());
    	if (item.md == null)
            throw ServiceException.FAILURE("Mapping for folder "+folder.getPath()+" not found", null);
    	ArrayList<RemoteItem> ret = new ArrayList<RemoteItem>();
    	try {
        	CalDavClient client = getClient();
        	Collection<Appointment> appts = client.getEtags(item.remoteId);
        	for (Appointment a : appts) {
        		ret.add(new RemoteCalendarItem(a.href, a.etag));
        	}
    	} catch (DavException e) {
    		
    	}
    	return ret;
    }
    private MailItem applyRemoteItem(RemoteItem remoteItem) throws ServiceException, IOException {
    	if (!(remoteItem instanceof RemoteCalendarItem)) {
    		ZimbraLog.datasource.warn("applyRemoteItem: note a calendar item: ", remoteItem);
    		return null;
    	}
    	RemoteCalendarItem item = (RemoteCalendarItem) remoteItem;
    	ZimbraLog.datasource.debug("Check item %s", item.href);
    	DataSource ds = getDataSource();
    	Mailbox mbox = ds.getMailbox();
    	DataSourceItem dsItem = DbDataSource.getReverseMapping(mbox, ds, item.href);
    	if (dsItem.itemId == 0) {
    		// new item
    	} else if (dsItem.md == null) {
    		throw ServiceException.FAILURE("Mapping for item "+dsItem.itemId+" not found", null);
    	} else {
    		String etag = dsItem.md.get(METADATA_KEY_ETAG);
    		boolean isStale = false;
    		if (item.etag == null) {
    			ZimbraLog.datasource.warn("No Etag returned for item %s", item.href);
    			isStale = true;
    		} else if (etag == null) {
    			ZimbraLog.datasource.warn("Empty etag for item %d", dsItem.itemId);
    			isStale = true;
    		} else {
    			isStale = !item.etag.equals(etag);
    		}
    		if (isStale) {
    	    	try {
    	    		CalDavClient client = getClient();
    	    		Appointment appt = client.getCalendarData(new Appointment(item.href, item.etag));
    	    		// update the appt in the mailbox
    	    		dsItem.md.put(METADATA_KEY_ETAG, appt.etag);
    	    		DbDataSource.addMapping(mbox, ds, dsItem);
    	    	} catch (DavException e) {
    	    		
    	    	}
    		}
    	}
    	return null;
    }
    private void sync(Mailbox mbox, OperationContext octxt, Folder syncFolder) throws ServiceException, IOException {
    	HashSet<Integer> fid = new HashSet<Integer>();
    	int lastSync = (int)syncFolder.getLastSyncDate();
    	int currentSync = 0;
    	boolean allDone = false;
    	HashMap<Integer,Integer> modifiedFromRemote = new HashMap<Integer,Integer>();
    	ArrayList<Integer> deletedFromRemote = new ArrayList<Integer>();

    	// loop through as long as there are un'synced local changes
    	while (!allDone) {
    		allDone = true;
    		currentSync = mbox.getLastChangeID();
    		
    		// push local deletion
    		List<Integer> deleted = mbox.getTombstones(lastSync).getAll();
    		for (int itemId : deleted) {
    			if (deletedFromRemote.contains(itemId))
    				continue;  // was just deleted from sync
    			pushDelete(itemId);
    			allDone = false;
    		}

    		// push local modification
    		List<Integer> modified = mbox.getModifiedItems(octxt, lastSync, MailItem.TYPE_UNKNOWN, fid).getFirst();
    		for (int itemId : modified) {
    			MailItem item = mbox.getItemById(octxt, itemId, MailItem.TYPE_UNKNOWN);
    			if (modifiedFromRemote.containsKey(itemId) &&
    					modifiedFromRemote.get(itemId).equals(item.getModifiedSequence()))
    				continue;  // was just downloaded from remote
    			pushModify(item);
    			allDone = false;
    		}

    		
    		// pull in the changes from the remote server
    		List<RemoteItem> remoteItems = getRemoteItems(syncFolder);
    		for (RemoteItem item : remoteItems) {
    			MailItem localItem = applyRemoteItem(item);
    			if (localItem != null) {
    				if (item.status == Status.deleted)
    					deletedFromRemote.add(localItem.getId());
    				else
    					modifiedFromRemote.put(localItem.getId(), localItem.getModifiedSequence());
    			}
    		}

    		lastSync = currentSync;
    	}
    	mbox.setSyncDate(octxt, syncFolder.getId(), currentSync);
    }
}
