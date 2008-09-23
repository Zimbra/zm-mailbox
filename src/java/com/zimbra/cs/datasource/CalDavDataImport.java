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
import java.io.StringReader;
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
import com.zimbra.cs.mailbox.Mailbox.SetCalendarItemData;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mailbox.calendar.ZCalendar;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Metadata;

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
        	if (folderIds == null)
        		folderIds = syncFolders();
        	Mailbox mbox = getDataSource().getMailbox();
        	mbox.beginTrackingSync();
        	OperationContext octxt = new Mailbox.OperationContext(mbox);
        	for (int fid : folderIds) {
            	Folder syncFolder = mbox.getFolderById(octxt, fid);
        		sync(mbox, octxt, syncFolder);
        	}
    	} catch (DavException e) {
    		throw ServiceException.FAILURE("error importing CalDAV data", e);
    	} catch (IOException e) {
    		throw ServiceException.FAILURE("error importing CalDAV data", e);
    	}
    }

    public String test() throws ServiceException {
    	try {
    		DataSource ds = getDataSource();
        	mClient = new CalDavClient(getTargetUrl());
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
    		if (a.startsWith("p:")) {
    			return a.substring(2).replaceAll("_USERNAME_", ds.getUsername());
    		}
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
    private List<Integer> syncFolders() throws ServiceException, IOException, DavException {
    	ArrayList<Integer> ret = new ArrayList<Integer>();
    	DataSource ds = getDataSource();
		CalDavClient client = getClient();
		Map<String,String> calendars = client.getCalendars();
		Mailbox mbox = ds.getMailbox();
    	OperationContext octxt = new Mailbox.OperationContext(mbox);
		Folder rootFolder = mbox.getFolderById(octxt, ds.getFolderId());
		for (String name : calendars.keySet()) {
			String url = calendars.get(name);
			DataSourceItem f = DbDataSource.getReverseMapping(ds.getMailbox(), ds, url);
			if (f.itemId == 0) {
				// XXX add handling for multi level subfolders
				Folder newFolder = mbox.createFolder(octxt, name, rootFolder.getId(), MailItem.TYPE_APPOINTMENT, 0, (byte)0, null);
				f.itemId = newFolder.getId();
				f.remoteId = url;
				f.md = new Metadata();
				f.md.put(METADATA_KEY_TYPE, METADATA_TYPE_FOLDER);
				DbDataSource.addMapping(mbox, ds, f);
			} else if (f.md == null) {
	    		ZimbraLog.datasource.warn("syncFolders: empty metadata for item %d", f.itemId);
	    		f.md = new Metadata();
				f.md.put(METADATA_KEY_TYPE, METADATA_TYPE_FOLDER);
				DbDataSource.addMapping(mbox, ds, f);
			}
			ret.add(f.itemId);
		}
		return ret;
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
    		throw ServiceException.FAILURE("error getting items for folder "+folder.getName(), e);
    	}
    	return ret;
    }
    private MailItem applyRemoteItem(RemoteItem remoteItem, Folder where) throws ServiceException, IOException {
    	if (!(remoteItem instanceof RemoteCalendarItem)) {
    		ZimbraLog.datasource.warn("applyRemoteItem: note a calendar item: ", remoteItem);
    		return null;
    	}
    	RemoteCalendarItem item = (RemoteCalendarItem) remoteItem;
    	ZimbraLog.datasource.debug("Check item %s", item.href);
    	DataSource ds = getDataSource();
    	Mailbox mbox = ds.getMailbox();
    	DataSourceItem dsItem = DbDataSource.getReverseMapping(mbox, ds, item.href);
    	boolean isStale = false;
    	if (dsItem.md == null) {
    		dsItem.md = new Metadata();
    		dsItem.md.put(METADATA_KEY_TYPE, METADATA_TYPE_APPOINTMENT);
    	}
    	if (dsItem.itemId == 0) {
    		isStale = true;
    	} else {
        	String etag = dsItem.md.get(METADATA_KEY_ETAG);
        	if (item.etag == null) {
        		ZimbraLog.datasource.warn("No Etag returned for item %s", item.href);
        		isStale = true;
        	} else if (etag == null) {
        		ZimbraLog.datasource.warn("Empty etag for item %d", dsItem.itemId);
        		isStale = true;
        	} else {
        		isStale = !item.etag.equals(etag);
        	}
    	}
    	OperationContext octxt = new Mailbox.OperationContext(mbox);
    	MailItem mi = null;
    	if (isStale) {
    		ZCalendar.ZVCalendar vcalendar;
    		try {
    			CalDavClient client = getClient();
    			Appointment appt = client.getCalendarData(new Appointment(item.href, item.etag));
        		dsItem.md.put(METADATA_KEY_ETAG, appt.etag);
    			vcalendar = ZCalendar.ZCalendarBuilder.build(new StringReader(appt.data));
    		} catch (DavException e) {
        		throw ServiceException.FAILURE("error getting calendar data for "+item.href, e);
    		}
    		List<Invite> invites = Invite.createFromCalendar(mbox.getAccount(), null, vcalendar, true);
    		SetCalendarItemData main = new SetCalendarItemData();
    		SetCalendarItemData exceptions[] = null;
    		if (invites.size() > 1)
    			exceptions = new SetCalendarItemData[invites.size() - 1];

    		int pos = 0;
    		boolean first = true;
    		for (Invite i : invites) {
    			if (first) {
    				main.mInv = i;
    				first = false;
    			} else {
    				SetCalendarItemData scid = new SetCalendarItemData();
    				scid.mInv = i;
    				exceptions[pos++] = scid;
    			}
    		}
    		mi = mbox.setCalendarItem(octxt, where.getId(), 0, 0,
    				main, exceptions, null, 0);
    		dsItem.itemId = mi.getId();
    		DbDataSource.addMapping(mbox, ds, dsItem);
    	} else {
    		mi = mbox.getItemById(octxt, dsItem.itemId, MailItem.TYPE_UNKNOWN);
    	}
    	return mi;
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
    			MailItem localItem = applyRemoteItem(item, syncFolder);
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
    public static void main(String[] args) throws Exception {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraDataSourceHost, "www.google.com");
        attrs.put(Provisioning.A_zimbraDataSourcePort, "443");
        attrs.put(Provisioning.A_zimbraDataSourceConnectionType, "ssl");
        attrs.put(Provisioning.A_zimbraDataSourceUsername, "ttttest123@gmail.com");
        attrs.put(Provisioning.A_zimbraDataSourcePassword, "test1234");
        attrs.put(Provisioning.A_zimbraDataSourceAttribute, "p:/calendar/dav/_USERNAME_/user");
    	attrs.put(Provisioning.A_zimbraDataSourceImportClassName, CalDavDataImport.class.getName());
        attrs.put(Provisioning.A_zimbraDataSourceFolderId, "test1234");
    }
}
