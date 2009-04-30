/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.datasource;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.dav.DavElements;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.dav.client.CalDavClient;
import com.zimbra.cs.dav.client.DavObject;
import com.zimbra.cs.dav.client.CalDavClient.Appointment;
import com.zimbra.cs.dav.client.DavRequest;
import com.zimbra.cs.db.DbDataSource;
import com.zimbra.cs.db.DbDataSource.DataSourceItem;
import com.zimbra.cs.mailbox.CalendarItem;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.Mailbox.SetCalendarItemData;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.calendar.ICalTimeZone;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mailbox.calendar.ZCalendar;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Metadata;

public class CalDavDataImport extends MailItemImport {

	private static final String METADATA_KEY_TYPE = "t";
	private static final String METADATA_TYPE_FOLDER = "f";
	private static final String METADATA_TYPE_APPOINTMENT = "a";
	private static final String METADATA_KEY_ETAG = "e";
	private static final String METADATA_KEY_CTAG = "c";
	private static final int DEFAULT_FOLDER_FLAGS = Flag.flagsToBitmask("#");
	
	private CalDavClient mClient;
	
    public CalDavDataImport(DataSource ds) throws ServiceException {
    	super(ds);
    }
	
    public void importData(List<Integer> folderIds, boolean fullSync)
    throws ServiceException {
    	try {
        	mbox.beginTrackingSync();
        	if (folderIds == null)
        		folderIds = syncFolders();
        	OperationContext octxt = new OperationContext(mbox);
        	for (int fid : folderIds) {
            	Folder syncFolder = mbox.getFolderById(octxt, fid);
            	if (syncFolder.getDefaultView() == MailItem.TYPE_APPOINTMENT)
            	    sync(octxt, syncFolder);
        	}
    	} catch (DavException e) {
    		throw ServiceException.FAILURE("error importing CalDAV data", e);
    	} catch (IOException e) {
    		throw ServiceException.FAILURE("error importing CalDAV data", e);
    	}
    }

    public void test() throws ServiceException {
    	mClient = new CalDavClient(getTargetUrl());
    	mClient.setAppName(getAppName());
		mClient.setCredential(getUsername(), getDecryptedPassword());
		mClient.setDebugEnabled(dataSource.isDebugTraceEnabled());
		try {
			mClient.login(getPrincipalUrl());
		} catch (Exception x) {
			throw ServiceException.FAILURE(x.getMessage(), x);
		}
    }

    protected String getUsername() {
        return getDataSource().getUsername();
    }
    
    protected String getDecryptedPassword() throws ServiceException {
        return getDataSource().getDecryptedPassword();
    }
    
    protected String getPrincipalUrl() {
    	DataSource ds = getDataSource();
    	String attrs[] = ds.getMultiAttr(Provisioning.A_zimbraDataSourceAttribute);
    	for (String a : attrs) {
    		if (a.startsWith("p:")) {
    			return a.substring(2).replaceAll("_USERNAME_", getUsername());
    		}
    	}
    	return null;
    }

    protected String getTargetUrl() {
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

    protected String getAppName() {
        return "ZCS";
    }
    
    private CalDavClient getClient() throws ServiceException, IOException, DavException {
    	if (mClient == null) {
        	mClient = new CalDavClient(getTargetUrl());
        	mClient.setAppName(getAppName());
    		mClient.setCredential(getUsername(), getDecryptedPassword());
    		mClient.setDebugEnabled(dataSource.isDebugTraceEnabled());
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
    
    protected int getRootFolderId(DataSource ds) throws ServiceException {
    	return ds.getFolderId();
    }
    
    protected HashMap<String,DataSourceItem> getAllFolderMappings(DataSource ds) throws ServiceException {
		Collection<DataSourceItem> allFolders = DbDataSource.getAllMappingsInFolder(ds, getRootFolderId(ds));
		HashMap<String,DataSourceItem> folders = new HashMap<String,DataSourceItem>();
		for (DataSourceItem f : allFolders)
			if (f.remoteId != null)
				folders.put(f.remoteId, f);
		return folders;
    }
    
    private List<Integer> syncFolders() throws ServiceException, IOException, DavException {
    	ArrayList<Integer> ret = new ArrayList<Integer>();
    	DataSource ds = getDataSource();
		CalDavClient client = getClient();
		Map<String,DavObject> calendars = client.getCalendars();
		OperationContext octxt = new OperationContext(mbox);
		Folder rootFolder = mbox.getFolderById(octxt, getRootFolderId(ds));
		HashMap<String,DataSourceItem> allFolders = getAllFolderMappings(ds);
		List<Integer> deleted = new ArrayList<Integer>();
    	int lastSync = (int)rootFolder.getLastSyncDate();
		for (int itemId : mbox.getTombstones(lastSync).getAll())
			deleted.add(itemId);
		for (String name : calendars.keySet()) {
			DavObject obj = calendars.get(name);
			String ctag = obj.getPropertyText(DavElements.E_GETCTAG);
			String url = obj.getHref();
			DataSourceItem f = allFolders.get(url);
			if (f == null)
				f = new DataSourceItem(0, 0, url, null);
			Folder folder = null;
			if (f.itemId != 0) {
				// check if the folder was deleted
				if (deleted.contains(f.itemId)) {
					allFolders.remove(url);
					DbDataSource.deleteMapping(ds, f.itemId);
					DbDataSource.deleteAllMappingsInFolder(ds, f.itemId);
					deleteRemoteFolder(url);
					continue;
				}
				// check if the folder is valid
				try {
					folder = mbox.getFolderById(octxt, f.itemId);
				} catch (ServiceException se) {
					if (se.getCode() != MailServiceException.NO_SUCH_FOLDER)
						throw se;
					f.itemId = 0;
				}
			}
			if (f.itemId == 0) {
				try {
					// check if we can use the folder
					folder = mbox.getFolderByName(octxt, rootFolder.getId(), name);
					if (folder.getDefaultView() != MailItem.TYPE_APPOINTMENT) {
						name = name + " (" + getDataSource().getName() + ")";
						folder = null;
					}
				} catch (MailServiceException.NoSuchItemException e) {
				}
				
				if (folder == null)
					folder = mbox.createFolder(octxt, name, rootFolder.getId(), MailItem.TYPE_APPOINTMENT, DEFAULT_FOLDER_FLAGS, (byte)0, null);
				
				f.itemId = folder.getId();
				f.folderId = folder.getFolderId();
				f.md = new Metadata();
				f.md.put(METADATA_KEY_TYPE, METADATA_TYPE_FOLDER);
				if (ctag != null)
					f.md.put(METADATA_KEY_CTAG, ctag);
				f.remoteId = url;
		    	mbox.setSyncDate(octxt, folder.getId(), mbox.getLastChangeID());
				DbDataSource.addMapping(ds, f);
			} else if (f.md == null) {
	    		ZimbraLog.datasource.warn("syncFolders: empty metadata for item %d", f.itemId);
				f.folderId = folder.getFolderId();
				f.remoteId = url;
	    		f.md = new Metadata();
				f.md.put(METADATA_KEY_TYPE, METADATA_TYPE_FOLDER);
				if (ctag != null)
					f.md.put(METADATA_KEY_CTAG, ctag);
				DbDataSource.addMapping(ds, f);
			}
			String fname = folder.getName();
			if (!fname.equals(name)) {
        		ZimbraLog.datasource.warn("renaming folder %s to %s", fname, name);
        		try {
    				mbox.rename(octxt, f.itemId, MailItem.TYPE_FOLDER, name, folder.getFolderId());
        		} catch (ServiceException e) {
        			ZimbraLog.datasource.warn("folder rename failed", e);
        		}
			}
			if (ctag != null) {
				String oldctag = f.md.get(METADATA_KEY_CTAG, null);
				if (!ctag.equals(oldctag)) {
					ret.add(f.itemId);
					f.md.put(METADATA_KEY_CTAG, ctag);
					DbDataSource.updateMapping(ds, f);
				}
			}
			allFolders.remove(url);
		}
		if (!allFolders.isEmpty()) {
			// handle deleted folders
			ArrayList<Integer> fids = new ArrayList<Integer>();
			int[] fidArray = new int[allFolders.size()];
			int i = 0;
			for (DataSourceItem f : allFolders.values()) {
				fids.add(f.itemId);
				fidArray[i++] = f.itemId;
				DbDataSource.deleteAllMappingsInFolder(ds, f.itemId);
			}
			DbDataSource.deleteMappings(ds, fids);
			try {
				mbox.delete(octxt, fidArray, MailItem.TYPE_FOLDER, null);
			} catch (ServiceException e) {
    			ZimbraLog.datasource.warn("folder delete failed", e);
			}
		}
		return ret;
    }
    private void deleteRemoteFolder(String url) throws ServiceException, IOException, DavException {
		ZimbraLog.datasource.debug("deleteRemoteFolder: deleting remote folder %s", url);
		getClient().sendRequest(DavRequest.DELETE(url));
    }
    private boolean pushDelete(Collection<Integer> itemIds) throws ServiceException, IOException, DavException {
    	DataSource ds = getDataSource();
    	boolean deleted = false;
    	ArrayList<Integer> toDelete = new ArrayList<Integer>();
    	for (int itemId : itemIds) {
    		try {
            	deleteRemoteItem(DbDataSource.getMapping(ds, itemId));
            	toDelete.add(itemId);
    		} catch (Exception e) {
        		ZimbraLog.datasource.warn("pushDelete: can't delete remote item for item "+itemId, e);
    		}
    	}
    	if (toDelete.size() > 0) {
    		DbDataSource.deleteMappings(ds, toDelete);
    		deleted = true;
    	}
    	return deleted;
    }
    private void deleteRemoteItem(DataSourceItem item) throws ServiceException, IOException, DavException {
    	if (item.itemId <= 0 || item.md == null) {
    		ZimbraLog.datasource.warn("pushDelete: empty item %d", item.itemId);
    		return;
    	}
    	String type = item.md.get(METADATA_KEY_TYPE);
    	String uri = item.remoteId;
    	if (uri == null) {
    		ZimbraLog.datasource.warn("pushDelete: empty uri for item %d", item.itemId);
    		return;
    	}
    	if (METADATA_TYPE_FOLDER.equals(type)) {
    		ZimbraLog.datasource.debug("pushDelete: deleting remote folder %s", uri);
    		getClient().sendRequest(DavRequest.DELETE(uri));
    	} else if (METADATA_TYPE_APPOINTMENT.equals(type)) {
    		ZimbraLog.datasource.debug("pushDelete: deleting remote appointment %s", uri);
    		getClient().sendRequest(DavRequest.DELETE(uri));
    	} else {
    		ZimbraLog.datasource.warn("pushDelete: unrecognized item type for %d: %s", item.itemId, type);
    	}
    }
    private String createTargetUrl(MailItem mitem) throws ServiceException {
		DataSourceItem folder = DbDataSource.getMapping(getDataSource(), mitem.getFolderId());
		String url = folder.remoteId;
		switch (mitem.getType()) {
		case MailItem.TYPE_APPOINTMENT:
			url += ((CalendarItem)mitem).getUid() + ".ics";
			break;
		default:
			String name = mitem.getName();
			if (name != null)
				url += name;
			else
				url += mitem.getSubject();
			break;
		}
		return url;
    }
    private void pushModify(MailItem mitem) throws ServiceException, IOException, DavException {
    	int itemId = mitem.getId();
    	DataSource ds = getDataSource();
    	DataSourceItem item = DbDataSource.getMapping(ds, itemId);
    	if (item.remoteId == null) {
    		// new item
    		item.md = new Metadata();
			item.md.put(METADATA_KEY_TYPE, METADATA_TYPE_APPOINTMENT);
			item.remoteId = createTargetUrl(mitem);
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
    		ZimbraLog.datasource.debug("pushModify: sending appointment %s", item.remoteId);
    		String etag = putAppointment((CalendarItem)mitem, item);
    		item.md.put(METADATA_KEY_ETAG, etag);
    		DbDataSource.addMapping(ds, item);
    	} else {
    		ZimbraLog.datasource.warn("pushModify: unrecognized item type for %d: %s", itemId, type);
    		return;
    	}
    }
    private String putAppointment(CalendarItem calItem, DataSourceItem dsItem) throws ServiceException, IOException, DavException {
        StringBuilder buf = new StringBuilder();
        ArrayList<String> recipients = new ArrayList<String>();

        buf.append("BEGIN:VCALENDAR\r\n");
        buf.append("VERSION:").append(ZCalendar.sIcalVersion).append("\r\n");
        buf.append("PRODID:").append(ZCalendar.sZimbraProdID).append("\r\n");
        Iterator<ICalTimeZone> iter = calItem.getTimeZoneMap().tzIterator();
        while (iter.hasNext()) {
            ICalTimeZone tz = iter.next();
            CharArrayWriter wr = new CharArrayWriter();
            tz.newToVTimeZone().toICalendar(wr, true);
            wr.flush();
            buf.append(wr.toCharArray());
            wr.close();
        }
        for (Invite inv : calItem.getInvites()) {
            CharArrayWriter wr = new CharArrayWriter();
            ZCalendar.ZComponent vcomp = inv.newToVComponent(false, true);
            ZCalendar.ZProperty organizer = vcomp.getProperty(ZCalendar.ICalTok.ORGANIZER);
            if (organizer != null)
            	organizer.setValue(getUsername());
                //organizer.setValue("mailto:" + getUsername());
            // XXX
            //vcomp.addProperty(new ZCalendar.ZProperty(ZCalendar.ICalTok.ATTENDEE, "urn:resource:resource01"));
            vcomp.toICalendar(wr, true);
            wr.flush();
            buf.append(wr.toCharArray());
            wr.close();
            
        }
        buf.append("END:VCALENDAR\r\n");
    	String etag = dsItem.md.get(METADATA_KEY_ETAG, null);
    	if (recipients.isEmpty())
    		recipients = null;
        Appointment appt = new Appointment(dsItem.remoteId, etag, buf.toString(), recipients);
        return getClient().sendCalendarData(appt);
    }
    private List<RemoteItem> getRemoteItems(Folder folder) throws ServiceException, IOException, DavException {
		ZimbraLog.datasource.debug("Refresh folder %s", folder.getPath());
    	DataSource ds = getDataSource();
    	DataSourceItem item = DbDataSource.getMapping(ds, folder.getId());
    	if (item.md == null)
            throw ServiceException.FAILURE("Mapping for folder "+folder.getPath()+" not found", null);

    	// CalDAV doesn't provide delete tombstone.  in order to check for deleted appointments
    	// we need to cross reference the current result with what we have from last sync
    	// and check for any appointment that has disappeared since last sync.
    	HashMap<String,DataSourceItem> allItems = new HashMap<String,DataSourceItem>();
    	for (DataSourceItem localItem : DbDataSource.getAllMappingsInFolder(getDataSource(), folder.getId()))
    		allItems.put(localItem.remoteId, localItem);
    	
    	ArrayList<RemoteItem> ret = new ArrayList<RemoteItem>();
    	CalDavClient client = getClient();
    	Collection<Appointment> appts = client.getEtags(item.remoteId);
    	for (Appointment a : appts) {
    		ret.add(new RemoteCalendarItem(a.href, a.etag));
    		allItems.remove(a.href);
    	}
    	for (DataSourceItem deletedItem : allItems.values()) {
    		// what's left in the collection are previous mapping that has disappeared.
    		// we need to delete the appointments that are mapped locally.
			RemoteCalendarItem rci = new RemoteCalendarItem(deletedItem.remoteId, null);
			rci.status = Status.deleted;
    		ret.add(rci);
    	}
    	return ret;
    }
    private MailItem applyRemoteItem(RemoteItem remoteItem, Folder where) throws ServiceException, IOException {
    	if (!(remoteItem instanceof RemoteCalendarItem)) {
    		ZimbraLog.datasource.warn("applyRemoteItem: not a calendar item: ", remoteItem);
    		return null;
    	}
    	RemoteCalendarItem item = (RemoteCalendarItem) remoteItem;
    	DataSource ds = getDataSource();
    	DataSourceItem dsItem = DbDataSource.getReverseMapping(ds, item.href);
    	OperationContext octxt = new OperationContext(mbox);
    	MailItem mi = null;
    	boolean isStale = false;
    	if (dsItem.md == null) {
    		dsItem.md = new Metadata();
    		dsItem.md.put(METADATA_KEY_TYPE, METADATA_TYPE_APPOINTMENT);
    	}
    	if (dsItem.itemId == 0) {
    		isStale = true;
    	} else {
        	String etag = dsItem.md.get(METADATA_KEY_ETAG, null);
        	try {
        		mi = mbox.getItemById(octxt, dsItem.itemId, MailItem.TYPE_UNKNOWN);
        	} catch (MailServiceException.NoSuchItemException se) {
        		ZimbraLog.datasource.warn("applyRemoteItem: calendar item not found: ", remoteItem);
        	}
        	if (item.etag == null) {
        		ZimbraLog.datasource.warn("No Etag returned for item %s", item.href);
        		isStale = true;
        	} else if (etag == null) {
        		ZimbraLog.datasource.warn("Empty etag for item %d", dsItem.itemId);
        		isStale = true;
        	} else {
        		isStale = !item.etag.equals(etag);
        	}
        	if (mi == null)
        		isStale = true;
    	}
    	if (isStale && item.status == Status.deleted) {
        	ZimbraLog.datasource.debug("Deleting appointment %s", item.href);
        	mi = mbox.getItemById(octxt, dsItem.itemId, MailItem.TYPE_UNKNOWN);
        	mbox.delete(octxt, dsItem.itemId, MailItem.TYPE_UNKNOWN);
    	} else if (isStale) {
        	ZimbraLog.datasource.debug("Updating stale appointment %s", item.href);
    		ZCalendar.ZVCalendar vcalendar;
			SetCalendarItemData main = new SetCalendarItemData();
			SetCalendarItemData exceptions[] = null;
			CalDavClient client = null;
    		try {
    			client = getClient();
    		} catch (DavException e) {
           		throw ServiceException.FAILURE("error creating CalDAV client", e);
    		}
    		
			Appointment appt = client.getCalendarData(new Appointment(item.href, item.etag));
			if (appt.data == null) {
	        	ZimbraLog.datasource.warn("No appointment found at "+item.href);
	        	return null;
			}
    		dsItem.md.put(METADATA_KEY_ETAG, appt.etag);
    		
    		try {
    			vcalendar = ZCalendar.ZCalendarBuilder.build(new StringReader(appt.data));
    			List<Invite> invites = Invite.createFromCalendar(mbox.getAccount(), null, vcalendar, true);
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
    		} catch (Exception e) {
	        	ZimbraLog.datasource.warn("Error parsing appointment ", e);
	        	return null;
    		}
    		mi = mbox.setCalendarItem(octxt, where.getId(), 0, 0,
    				main, exceptions, null, CalendarItem.NEXT_ALARM_KEEP_CURRENT);
    		dsItem.itemId = mi.getId();
    		dsItem.folderId = mi.getFolderId();
    		DbDataSource.addMapping(ds, dsItem);
    	} else {
        	ZimbraLog.datasource.debug("Appointment up to date %s", item.href);
    	}
    	return mi;
    }
    private void sync(OperationContext octxt, Folder syncFolder) throws ServiceException, IOException, DavException {
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
    		List<Integer> deleted = new ArrayList<Integer>();
    		for (int itemId : mbox.getTombstones(lastSync).getAll()) {
    			if (deletedFromRemote.contains(itemId)) {
    				continue;  // was just deleted from sync
    			}
    			deleted.add(itemId);
    		}

			// move to trash is equivalent to delete
        	HashSet<Integer> fid = new HashSet<Integer>();
        	fid.add(Mailbox.ID_FOLDER_TRASH);
    		List<Integer> trashed = mbox.getModifiedItems(octxt, lastSync, MailItem.TYPE_UNKNOWN, fid).getFirst();
    		deleted.addAll(trashed);

    		if (!deleted.isEmpty()) {
    			// pushDelete returns true if one or more items were deleted
    			allDone &= !pushDelete(deleted);
    		}
    		
    		// push local modification
			fid.clear();
        	fid.add(syncFolder.getId());
    		List<Integer> modified = mbox.getModifiedItems(octxt, lastSync, MailItem.TYPE_UNKNOWN, fid).getFirst();
    		for (int itemId : modified) {
    			MailItem item = mbox.getItemById(octxt, itemId, MailItem.TYPE_UNKNOWN);
    			if (modifiedFromRemote.containsKey(itemId) &&
    					modifiedFromRemote.get(itemId).equals(item.getModifiedSequence()))
    				continue;  // was just downloaded from remote
    			try {
        			pushModify(item);
    			} catch (Exception e) {
    	        	ZimbraLog.datasource.info("Failed to push item "+item.getId(), e);
    			}
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
}
