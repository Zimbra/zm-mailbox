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
package com.zimbra.cs.mailbox;

import java.util.Collection;
import java.util.Map;
import java.util.TreeSet;

import javax.mail.Address;
import javax.mail.internet.InternetAddress;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.MultiTreeMap;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.ContactAutoComplete.ContactEntry;

public class ContactRankings {
	private static final String CONFIG_KEY_CONTACT_RANKINGS = "CONTACT_RANKINGS";
	private static final String KEY_NAME = "n";
	private static final String KEY_FOLDER = "o";
	private static final String KEY_RANKING = "r";
	
	private int mTableSize;
	private String mAccountId;
	private MultiTreeMap<String,ContactEntry> mEntryMap;
	private TreeSet<ContactEntry> mEntrySet;
	public ContactRankings(String accountId) throws ServiceException {
		mAccountId = accountId;
		mEntryMap = new MultiTreeMap<String,ContactEntry>();
		mEntrySet = new TreeSet<ContactEntry>();
		mTableSize = Provisioning.getInstance().get(Provisioning.AccountBy.id, mAccountId).getIntAttr(Provisioning.A_zimbraContactRankingTableSize, 40);
		readFromDatabase();
	}
	public static void increment(String accountId, Collection<? extends Address> addrs) throws ServiceException {
		ContactRankings rankings = new ContactRankings(accountId);
		for (Address addr : addrs)
			if (addr instanceof InternetAddress)
				rankings.increment(((InternetAddress)addr).getAddress(), 
								   ((InternetAddress)addr).getPersonal());
		
		rankings.writeToDatabase();
	}
	public synchronized void increment(String email, String displayName) {
		ContactEntry entry = mEntryMap.getFirst(email.toLowerCase());
		if (entry == null) {
			entry = new ContactEntry();
			entry.mEmail = email;
			entry.mRanking = 1;
			entry.setName(displayName);
			entry.mFolderId = ContactAutoComplete.FOLDER_ID_UNKNOWN;
			updateContactInfo(entry);
			
			if (mEntrySet.size() < mTableSize)
				add(entry);
			else {
				while (mEntrySet.size() > mTableSize)
					remove(mEntrySet.first());
				ContactEntry firstEntry = mEntrySet.first();
				if (firstEntry.mRanking == 0) {
					remove(firstEntry);
					add(entry);
				} else {
					for (ContactEntry e : mEntrySet)
						e.mRanking--;
				}
			}
		} else {
			entry.mRanking++;
			if (entry.mFolderId == ContactAutoComplete.FOLDER_ID_UNKNOWN ||
					entry.mDisplayName.length() == 0)
				updateContactInfo(entry);
		}
	}
	private void updateContactInfo(ContactEntry entry) {
		ContactAutoComplete auto = new ContactAutoComplete(mAccountId);
		ContactEntry storedContact = null;
		try {
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
		ZimbraLog.gal.debug("querying ranking database");
		TreeSet<ContactEntry> entries = new TreeSet<ContactEntry>();
		str = str.toLowerCase();
		for (String k : mEntryMap.tailMap(str).keySet()) {
			if (k.startsWith(str)) {
				for (ContactEntry entry : mEntryMap.get(k)) {
					if (entry.mFolderId == ContactAutoComplete.FOLDER_ID_UNKNOWN ||
							folders.contains(entry.mFolderId)) {
						entries.add(entry);
						ZimbraLog.gal.debug("adding "+entry.toString());
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
			ZimbraLog.gal.debug("creating new contact ranking list for account "+mAccountId);
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
			config.put(entry.mEmail, m);
		}
		mbox.setConfig(null, CONFIG_KEY_CONTACT_RANKINGS, config);
		dump("writing");
	}
	private synchronized void add(ContactEntry entry) {
		mEntryMap.add(entry.mEmail.toLowerCase(), entry);
		if (entry.mDisplayName.length() > 0)
			mEntryMap.add(entry.mDisplayName.toLowerCase(), entry);
		if (entry.mLastName.length() > 0)
			mEntryMap.add(entry.mLastName.toLowerCase(), entry);
		mEntrySet.add(entry);
	}
	private synchronized void remove(ContactEntry entry) {
		mEntryMap.remove(entry.mEmail.toLowerCase(), entry);
		if (entry.mDisplayName.length() > 0)
			mEntryMap.remove(entry.mDisplayName.toLowerCase(), entry);
		if (entry.mLastName.length() > 0)
			mEntryMap.remove(entry.mLastName.toLowerCase(), entry);
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

