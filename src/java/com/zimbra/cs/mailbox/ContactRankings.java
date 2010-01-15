/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.mailbox;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeSet;

import javax.mail.Address;
import javax.mail.internet.InternetAddress;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Constants;
import com.zimbra.common.util.MultiTreeMap;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.ContactAutoComplete.ContactEntry;

public class ContactRankings {
	private static final String CONFIG_KEY_CONTACT_RANKINGS = "CONTACT_RANKINGS";
	private static final String KEY_NAME = "n";
	private static final String KEY_FOLDER = "o";
	private static final String KEY_RANKING = "r";
	private static final String KEY_LAST_ACCESSED = "t";
	
	private int mTableSize;
	private String mAccountId;
	private MultiTreeMap<String,ContactEntry> mEntryMap;
	private TreeSet<ContactEntry> mEntrySet;
	public ContactRankings(String accountId) throws ServiceException {
		mAccountId = accountId;
		mEntryMap = new MultiTreeMap<String,ContactEntry>(
				new Comparator<String>() { 
					public int compare(String left, String right) {
						return left.compareToIgnoreCase(right);
					}
				});
		mEntrySet = new TreeSet<ContactEntry>();
		mTableSize = Provisioning.getInstance().get(Provisioning.AccountBy.id, mAccountId).getIntAttr(Provisioning.A_zimbraContactRankingTableSize, 40);
		if (!LC.contact_ranking_enabled.booleanValue())
			return;
		readFromDatabase();
	}
    public static void reset(String accountId) throws ServiceException {
        if (!LC.contact_ranking_enabled.booleanValue())
            return;
        ContactRankings rankings = new ContactRankings(accountId);
        rankings.mEntryMap.clear();
        rankings.mEntrySet.clear();
        rankings.writeToDatabase();
    }
	public static void increment(String accountId, Collection<? extends Address> addrs) throws ServiceException {
		if (!LC.contact_ranking_enabled.booleanValue())
			return;
		ContactRankings rankings = new ContactRankings(accountId);
		for (Address addr : addrs)
			if (addr instanceof InternetAddress)
				rankings.increment(((InternetAddress)addr).getAddress(), 
								   ((InternetAddress)addr).getPersonal());
		
		rankings.writeToDatabase();
	}
	public synchronized void increment(String email, String displayName) {
		long now = System.currentTimeMillis();
		ContactEntry entry = mEntryMap.getFirst(email);
		if (entry == null) {
			entry = new ContactEntry();
			entry.mEmail = email;
			entry.mRanking = 1;
			entry.setName(displayName);
			entry.mFolderId = ContactAutoComplete.FOLDER_ID_UNKNOWN;
			entry.mLastAccessed = now;
			updateContactInfo(entry);
			
			if (mEntrySet.size() >= mTableSize) {
				ContactEntry firstEntry = mEntrySet.first();
				if (firstEntry.mRanking <= 1)
					remove(firstEntry);
			}
			if (mEntrySet.size() < mTableSize) {
				add(entry);
			} else {
				for (ContactEntry e : mEntrySet) {
					int weeksOld = (int) ((now - e.mLastAccessed) / Constants.MILLIS_PER_WEEK) + 1;
					e.mRanking -= weeksOld;
					if (e.mRanking < 0)
						e.mRanking = 0;
				}
			}
		} else {
			long refreshInterval = Constants.MILLIS_PER_WEEK;
			try {
				refreshInterval = Provisioning.getInstance().getConfig().getContactRankingTableRefreshInterval();
			} catch (ServiceException se) {
				ZimbraLog.gal.warn("can't get zimbraContactRankingTableRefreshInterval", se);
			}
			if (refreshInterval == 0)
				return;
			long age = now - entry.mLastAccessed;
			entry.mRanking++;
			if (entry.mRanking <= 0)
				entry.mRanking = 1;
			entry.mLastAccessed = now;
			// if the contact info in ranking table is incomplete check once a week
			// to fill out the full name and folder info.
			
			if (age > refreshInterval && 
					(entry.mFolderId == ContactAutoComplete.FOLDER_ID_UNKNOWN ||
					 entry.mDisplayName.length() == 0))
				updateContactInfo(entry);
		}
	}
	private void updateContactInfo(ContactEntry entry) {
		ContactAutoComplete auto = new ContactAutoComplete(mAccountId);
		ContactEntry storedContact = null;
		try {
			auto.setIncludeRankingResults(false);
			ContactAutoComplete.AutoCompleteResult res = auto.query(entry.mEmail, null, 1);
			if (res.entries.size() == 0)
				return;
			storedContact = res.entries.iterator().next();
		} catch (ServiceException se) {
			ZimbraLog.gal.warn("error searching for contact "+entry, se);
		}

		if (storedContact == null)
			return;
		
		// check if the contact has been added to the addressbook since
		// it entered the ranking database.
		entry.mFolderId = storedContact.mFolderId;
		
		// update display name if a better one is available.
		if (storedContact.mDisplayName.length() > 0 && entry.mDisplayName.length() == 0)
			entry.mDisplayName = storedContact.mDisplayName;
	}
	public synchronized Collection<ContactEntry> query(String str, Collection<Integer> folders) {
		TreeSet<ContactEntry> entries = new TreeSet<ContactEntry>(Collections.reverseOrder());
		int len = str.length();
		for (String k : mEntryMap.tailMap(str).keySet()) {
			if (k.length() >= len &&
					k.substring(0, len).equalsIgnoreCase(str)) {
				for (ContactEntry entry : mEntryMap.get(k)) {
					if (entry.mFolderId == ContactAutoComplete.FOLDER_ID_UNKNOWN ||
							folders == null ||
							folders.contains(entry.mFolderId)) {
						entries.add(entry);
					}
				}
			} else
				break;
		}
		return entries;
	}
	private synchronized void readFromDatabase() throws ServiceException {
		Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(mAccountId);
		Metadata config = mbox.getConfig(null, CONFIG_KEY_CONTACT_RANKINGS);
        if (config == null) {
        	config = new Metadata();
        	mbox.setConfig(null, CONFIG_KEY_CONTACT_RANKINGS, config);
        }
        for (Object k : config.mMap.keySet()) {
        	Object v = config.mMap.get(k);
        	if (v instanceof Map) {
        		@SuppressWarnings("unchecked")
        		Map<Object,Object> m = (Map<Object,Object>) v;
        		ContactEntry entry = new ContactEntry();
        		entry.mEmail = (String) k;
        		entry.setName((String) m.get(KEY_NAME));
        		Long num = (Long)m.get(KEY_FOLDER);
        		entry.mFolderId = num.intValue();
        		num = (Long)m.get(KEY_RANKING);
        		entry.mRanking = num.intValue();
        		num = (Long)m.get(KEY_LAST_ACCESSED);
        		entry.mLastAccessed = num.longValue();
        		add(entry);
        	}
        }
        dump("reading");
	}
	private synchronized void writeToDatabase() throws ServiceException {
		Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(mAccountId);
		Metadata config = new Metadata();
		for (ContactEntry entry : mEntrySet) {
			Metadata m = new Metadata();
			m.put(KEY_NAME, entry.mDisplayName);
			m.put(KEY_FOLDER, entry.mFolderId);
			m.put(KEY_RANKING, entry.mRanking);
			m.put(KEY_LAST_ACCESSED, entry.mLastAccessed);
			config.put(entry.mEmail, m);
		}
		mbox.setConfig(null, CONFIG_KEY_CONTACT_RANKINGS, config);
		dump("writing");
	}
	private synchronized void add(ContactEntry entry) {
		mEntryMap.add(entry.mEmail, entry);
		if (entry.mDisplayName.length() > 0)
			mEntryMap.add(entry.mDisplayName, entry);
		if (entry.mLastName.length() > 0)
			mEntryMap.add(entry.mLastName, entry);
		mEntrySet.add(entry);
	}
	private synchronized void remove(ContactEntry entry) {
		mEntryMap.remove(entry.mEmail, entry);
		if (entry.mDisplayName.length() > 0)
			mEntryMap.remove(entry.mDisplayName, entry);
		if (entry.mLastName.length() > 0)
			mEntryMap.remove(entry.mLastName, entry);
		mEntrySet.remove(entry);
	}
	private void dump(String action) {
		if (ZimbraLog.gal.isDebugEnabled()) {
			StringBuilder buf = new StringBuilder(action);
			buf.append("\n");
			for (ContactEntry entry : mEntrySet) {
				entry.toString(buf);
				buf.append("\n");
			}
			ZimbraLog.gal.debug(buf.toString());
		}
	}
}

