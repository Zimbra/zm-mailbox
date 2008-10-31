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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.TreeSet;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.MultiTreeMap;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.GalContact;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.SearchGalResult;
import com.zimbra.cs.index.ContactHit;
import com.zimbra.cs.index.MailboxIndex;
import com.zimbra.cs.index.ZimbraHit;
import com.zimbra.cs.index.ZimbraQueryResults;
import com.zimbra.cs.index.queryparser.ParseException;

public class ContactAutoComplete {
	public static class AutoCompleteResult {
		public AutoCompleteResult() { entries = new ArrayList<ContactEntry>(); canBeCached = true; }
		public Collection<ContactEntry> entries;
		public boolean canBeCached;
	}
    public static class ContactEntry implements Comparable<ContactEntry> {
        String mEmail;
        String mDisplayName;
        String mLastName;
        int mFolderId;
        int mRanking;
        public String getEmail() {
        	StringBuilder buf = new StringBuilder();
        	if (mDisplayName.length() > 0) {
        		buf.append("\"");
        		buf.append(mDisplayName);
        		buf.append("\" ");
        	}
        	buf.append("<").append(mEmail).append(">");
        	return buf.toString();
		}
        public int getFolderId() {
        	return mFolderId;
        }
        public int getRanking() {
        	return mRanking;
        }
        void setName(String name) {
    		if (name == null)
    			name = "";
    		mDisplayName = name;
    		int space = name.lastIndexOf(' ');
    		if (space > 0)
    			mLastName = name.substring(space+1);
        }
        public int compareTo(ContactEntry that) {
        	int diff = this.mRanking - that.mRanking;
        	if (diff != 0)
            	return diff;
        	return this.mEmail.compareTo(that.mEmail);
        }
        public String toString() {
        	StringBuilder buf = new StringBuilder();
        	toString(buf);
        	return buf.toString();
        }
        public void toString(StringBuilder buf) {
        	buf.append(mRanking).append(" ").append(getEmail()).append(" (").append(mFolderId).append(")");
        }
	}
    
    private static class ContactRankings {
    	private static final String CONFIG_KEY_CONTACT_RANKINGS = "CONTACT_RANKINGS";
    	private static final String KEY_NAME = "n";
    	private static final String KEY_FOLDER = "o";
    	private static final String KEY_RANKING = "r";
    	
    	private String mAccountId;
    	private MultiTreeMap<String,ContactEntry> mEntryMap;
    	private TreeSet<ContactEntry> mEntrySet;
    	public ContactRankings(String accountId) throws ServiceException {
    		mAccountId = accountId;
    		mEntryMap = new MultiTreeMap<String,ContactEntry>();
    		mEntrySet = new TreeSet<ContactEntry>();
    		readFromDatabase();
    	}
    	public synchronized void increment(String email, String displayName, int folderId) throws ServiceException {
    		ContactEntry entry = mEntryMap.getFirst(email.toLowerCase());
    		if (entry != null) {
    			entry.mRanking++;
    			return;
    		}
    		entry = new ContactEntry();
    		entry.mEmail = email;
			entry.mRanking = 1;
    		entry.setName(displayName);
    		if (folderId == 0)
    			folderId = FOLDER_ID_GAL;
    		entry.mFolderId = folderId;
    		
    		ContactEntry firstEntry = mEntrySet.first();
    		if (firstEntry.mRanking == 0) {
    			remove(firstEntry);
    			add(entry);
    		} else {
    			for (ContactEntry e : mEntrySet)
    				e.mRanking--;
    		}
    		writeToDatabase();
    	}
    	public synchronized Collection<ContactEntry> query(String str, Collection<Integer> folders) {
    		ZimbraLog.gal.debug("querying ranking database");
    		TreeSet<ContactEntry> entries = new TreeSet<ContactEntry>();
    		str = str.toLowerCase();
    		for (String k : mEntryMap.tailMap(str).keySet()) {
    			if (k.startsWith(str)) {
    				for (ContactEntry entry : mEntryMap.get(k)) {
    					if (folders.contains(entry.mFolderId)) {
    						entries.add(entry);
    						ZimbraLog.gal.debug("adding "+entry.toString());
    					}
    				}
    			} else
    				break;
    		}
    		return entries;
    	}
    	private void readFromDatabase() throws ServiceException {
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
            		entry.mFolderId = Integer.parseInt((String) m.get(KEY_FOLDER));
            		entry.mRanking = Integer.parseInt((String) m.get(KEY_RANKING));
            		add(entry);
            	}
            }
            dump("reading");
    	}
    	private void writeToDatabase() throws ServiceException {
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
    	private void add(ContactEntry entry) {
    		mEntryMap.add(entry.mEmail.toLowerCase(), entry);
    		mEntryMap.add(entry.mDisplayName.toLowerCase(), entry);
    		mEntryMap.add(entry.mLastName.toLowerCase(), entry);
    		mEntrySet.add(entry);
    	}
    	private void remove(ContactEntry entry) {
    		mEntryMap.remove(entry.mEmail.toLowerCase(), entry);
    		mEntryMap.remove(entry.mDisplayName.toLowerCase(), entry);
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
    
    private String mAccountId;
    private static final byte[] CONTACT_TYPES = new byte[] { MailItem.TYPE_CONTACT };
    private static final int FOLDER_ID_GAL = 0;
    private static final Collection<Integer> DEFAULT_FOLDERS = Arrays.asList(Mailbox.ID_FOLDER_CONTACTS, FOLDER_ID_GAL);
    
	private static final String[] gal_email_keys = {
		"email", "zimbraMailDeliveryAddress", "zimbraMailAlias"
	};
	private static final String[] contact_email_keys = {
		Contact.A_email, Contact.A_email2, Contact.A_email3
	};
	
	public ContactAutoComplete(String accountId) {
		mAccountId = accountId;
	}
	
	public AutoCompleteResult query(String str, Collection<Integer> folders, int limit) throws ServiceException {
		ZimbraLog.gal.debug("querying "+str);
		AutoCompleteResult result = new AutoCompleteResult();
		ContactRankings rankings = new ContactRankings(mAccountId);
		if (folders == null)
			folders = DEFAULT_FOLDERS;
		result.entries.addAll(rankings.query(str, folders));
		if (result.entries.size() < limit) {
			// search other folders
    		Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(mAccountId);
    		Mailbox.OperationContext octxt = new Mailbox.OperationContext(mbox);
			for (int fid : folders) {
				if (fid == FOLDER_ID_GAL)
					queryGal(str, limit, result);
				else
					queryFolder(mbox, octxt, str, fid, limit, result);
			}
		}
		return result;
	}
	
	private void queryGal(String str, int limit, AutoCompleteResult result) throws ServiceException {
		Provisioning prov = Provisioning.getInstance();
		Account account = prov.get(Provisioning.AccountBy.id, mAccountId);
        if (!(account.getBooleanAttr(Provisioning.A_zimbraFeatureGalAutoCompleteEnabled , false) &&
                account.getBooleanAttr(Provisioning.A_zimbraFeatureGalEnabled , false))) {
        	return;
        }
		ZimbraLog.gal.debug("querying gal");
		Provisioning.GAL_SEARCH_TYPE type = Provisioning.GAL_SEARCH_TYPE.ALL;
		Domain d = prov.getDomain(account);
        SearchGalResult sgr = prov.autoCompleteGal(d, str, type, limit);
        if (sgr.getHadMore() || sgr.getTokenizeKey() != null) {
        	result.canBeCached = false;
    		ZimbraLog.gal.debug("result can't be cached by client");
        }
		for (GalContact contact : sgr.getMatches()) {
			String id = contact.getId();
			ZimbraLog.gal.debug("gal entry: "+id);
        	ContactEntry entry = new ContactEntry();
	        Map<String, Object> attrs = contact.getAttrs();
        	for (String emailKey : gal_email_keys) {
    			Object email = attrs.get(emailKey);
    			if (email == null)
    				continue;
    			if (email instanceof String && ((String)email).toLowerCase().startsWith(str))
            		entry.mEmail = (String)email;
    			else if (email instanceof String[])
    				for (String e : ((String[])email))
    	    			if (e.toLowerCase().startsWith(str)) {
    	            		entry.mEmail = e;
    	            		break;
    	    			}

    			if (entry.mEmail != null)
    				break;
        	}
			if (entry.mEmail == null) {
        		ZimbraLog.gal.debug("gal entry has empty email address");
        		continue;
			}
			
			entry.setName((String)attrs.get("fullName"));
        	entry.mFolderId = FOLDER_ID_GAL;
        	result.entries.add(entry);
			ZimbraLog.gal.debug("adding "+entry.getEmail());
		}
	}
	
	private void queryFolder(Mailbox mbox, Mailbox.OperationContext octxt, String str, int fid, int limit, AutoCompleteResult result) throws ServiceException {
		ZimbraLog.gal.debug("querying folder "+fid);
		str = str.toLowerCase();
		String query = generateQuery(fid, str);
        ZimbraQueryResults results = null;
        try {
			results = mbox.search(octxt, query, CONTACT_TYPES, MailboxIndex.SortBy.NONE, 100);
            while (results.hasNext()) {
            	if (result.entries.size() > limit) {
                	result.canBeCached = false;
            		ZimbraLog.gal.debug("exceeded request limit "+limit);
            		return;
            	}
                ZimbraHit hit = results.getNext();
                if (hit instanceof ContactHit) {
                	Contact c = ((ContactHit) hit).getContact();
            		ZimbraLog.gal.debug("hit: "+c.getId());
                	ContactEntry entry = new ContactEntry();
                	for (String emailKey : contact_email_keys) {
                    	String email = c.get(emailKey);
                    	if (email != null && email.toLowerCase().startsWith(str)) {
                    		entry.mEmail = email;
                    		break;
                    	}
                	}
                	if (entry.mEmail == null) {
                		ZimbraLog.gal.debug("contact has empty email address");
                		continue;
                	}
                	entry.mLastName = c.get(Contact.A_lastName);
                	if (entry.mLastName == null)
                		entry.mLastName = "";
                	String firstname = c.get(Contact.A_firstName);
                	firstname = (firstname == null) ? "" : firstname + " ";
                	entry.mDisplayName = firstname + entry.mLastName;
                	entry.mFolderId = fid;
                	result.entries.add(entry);
        			ZimbraLog.gal.debug("adding "+entry.getEmail());
                }
            }
        } catch (IOException e) {
            throw ServiceException.FAILURE(e.getMessage(), e);
        } catch (ParseException e) {
            throw ServiceException.FAILURE(e.getMessage(), e);
        } finally {
            if (results != null)
                results.doneWithSearchResults();
        }
	}
	
	private String generateQuery(int fid, String query) {
		StringBuilder buf = new StringBuilder("inid:");
		buf.append(fid);
		buf.append(" (#lastName:").append(query).append("*");
		buf.append(" OR #firstName:").append(query).append("*");
		for (String emailKey : contact_email_keys)
			buf.append(" OR #").append(emailKey).append(":").append(query).append("*");
		buf.append(")");
		return buf.toString();
	}
}
