/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.mailbox;

import java.util.Collection;
import java.util.Map;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.mail.Address;
import javax.mail.internet.InternetAddress;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Constants;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.ContactAutoComplete.ContactEntry;

public class ContactRankings {
	private static final String CONFIG_KEY_CONTACT_RANKINGS = "CONTACT_RANKINGS";
    private static final String KEY_NAME = "n";
	private static final String KEY_RANKING = "r";
	private static final String KEY_LAST_ACCESSED = "t";
	
	private int mTableSize;
	private String mAccountId;
	private TreeMap<String,ContactEntry> mEntryMap;
	private HashMap<String,ContactEntry> mEntries;
	public ContactRankings(String accountId) throws ServiceException {
		mAccountId = accountId;
		mEntryMap = new TreeMap<String,ContactEntry>();
		mEntries = new HashMap<String,ContactEntry>();
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
        rankings.mEntries.clear();
        rankings.writeToDatabase();
    }
    public static void remove(String accountId, String email) throws ServiceException {
        if (!LC.contact_ranking_enabled.booleanValue())
            return;
        ContactRankings rankings = new ContactRankings(accountId);
        ContactEntry entry = rankings.mEntryMap.get(email);
        if (entry != null)
            rankings.remove(entry);
        rankings.writeToDatabase();
    }
    
	public static void increment(String accountId, Collection<? extends Address> addrs) throws ServiceException {
		if (!LC.contact_ranking_enabled.booleanValue())
			return;
		ContactRankings rankings = new ContactRankings(accountId);
		for (Address addr : addrs)
			if (addr instanceof InternetAddress) {
			    InternetAddress address = (InternetAddress)addr;
                rankings.increment(address.getAddress(), address.getPersonal());
			}
		
		rankings.writeToDatabase();
	}
	public synchronized void increment(String email, String displayName) {
		long now = System.currentTimeMillis();
		email = email.toLowerCase();
		ContactEntry entry = mEntries.get(email.toLowerCase());
		if (entry == null) {
			entry = new ContactEntry();
			entry.mEmail = email;
			entry.setName(displayName);
			entry.mRanking = 1;
			entry.mFolderId = ContactAutoComplete.FOLDER_ID_UNKNOWN;
			entry.mLastAccessed = now;
			
			if (mEntries.size() >= mTableSize) {
				ContactEntry lastEntry = getSortedSet().last();
				if (lastEntry.mRanking < 1)
					remove(lastEntry);
			}
			
			if (mEntries.size() < mTableSize) {
				add(entry);
			} else {
				for (ContactEntry e : mEntries.values()) {
					int weeksOld = (int) ((now - e.mLastAccessed) / Constants.MILLIS_PER_WEEK) + 1;
					e.mRanking -= weeksOld;
					if (e.mRanking < 0)
						e.mRanking = 0;
				}
			}
		} else {
			entry.mRanking++;
			if (entry.mRanking <= 0)
				entry.mRanking = 1;
			if (displayName != null && displayName.length() > 0)
			    entry.setName(displayName);
			entry.mLastAccessed = now;
		}
	}
	public int query(String email) {
	    ContactEntry entry = mEntryMap.get(email.toLowerCase());
	    if (entry != null)
	        return entry.mRanking;
	    return 0;
	}
    public synchronized Collection<ContactEntry> search(String str) {
        TreeSet<ContactEntry> entries = new TreeSet<ContactEntry>();
        int len = str.length();
        for (String k : mEntryMap.tailMap(str).keySet()) {
            if (k.length() >= len &&
                    k.substring(0, len).equalsIgnoreCase(str)) {
                entries.add(mEntryMap.get(k));
            } else
                break;
        }
        return entries;
    }
    private synchronized TreeSet<ContactEntry> getSortedSet() {
        return new TreeSet<ContactEntry>(mEntries.values());
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
                entry.mEmail = ((String) k).toLowerCase();
                Long num = (Long)m.get(KEY_RANKING);
                entry.mRanking = num.intValue();
                num = (Long)m.get(KEY_LAST_ACCESSED);
                entry.mLastAccessed = num.longValue();
                entry.setName((String)m.get(KEY_NAME));
                entry.mFolderId = ContactAutoComplete.FOLDER_ID_UNKNOWN;
                add(entry);
            }
        }
        dump("reading");
    }
	private synchronized void writeToDatabase() throws ServiceException {
		Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(mAccountId);
		Metadata config = new Metadata();
		for (ContactEntry entry : getSortedSet()) {
			Metadata m = new Metadata();
			m.put(KEY_RANKING, entry.mRanking);
			if (entry.mDisplayName != null)
			    m.put(KEY_NAME, entry.mDisplayName);
			m.put(KEY_LAST_ACCESSED, entry.mLastAccessed);
			config.put(entry.mEmail, m);
		}
		mbox.setConfig(null, CONFIG_KEY_CONTACT_RANKINGS, config);
		dump("writing");
	}
	private synchronized void add(ContactEntry entry) {
		mEntryMap.put(entry.mEmail, entry);
		if (entry.mDisplayName.length() > 0)
	        mEntryMap.put(entry.mDisplayName, entry);
		mEntries.put(entry.mEmail.toLowerCase(), entry);
	}
	private synchronized void remove(ContactEntry entry) {
		mEntryMap.remove(entry.mEmail);
        mEntryMap.remove(entry.mDisplayName);
		mEntries.remove(entry.mEmail.toLowerCase());
	}
	private void dump(String action) {
		if (ZimbraLog.gal.isDebugEnabled()) {
			StringBuilder buf = new StringBuilder(action);
			buf.append("\n");
			for (ContactEntry entry : getSortedSet()) {
				entry.toString(buf);
				buf.append("\n");
			}
			ZimbraLog.gal.debug(buf.toString());
		}
	}
}

