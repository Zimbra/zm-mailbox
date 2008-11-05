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
import java.util.HashSet;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
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
		public Collection<ContactEntry> entries;
		public boolean canBeCached;
		public AutoCompleteResult() { 
			entries = new ArrayList<ContactEntry>(); 
			emails = new HashSet<String>();
			canBeCached = true; 
		}
		public void addEntry(ContactEntry entry) {
			String email;
			if (entry.isDlist())
				email = entry.mDisplayName;
			else
				email = entry.mEmail.toLowerCase();
			if (!emails.contains(email)) {
				entries.add(entry);
				emails.add(email);
			}
		}
		private HashSet<String> emails;
	}
    public static class ContactEntry implements Comparable<ContactEntry> {
        String mEmail;
        String mDisplayName;
        String mLastName;
        String mDlist;
        int mFolderId;
        int mRanking;
        public String getEmail() {
        	if (mDlist != null)
        		return mDlist;
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
        public boolean isDlist() {
        	return mDlist != null;
        }
        public String getDisplayName() {
        	return mDisplayName;
        }
        void setName(String name) {
    		if (name == null)
    			name = "";
    		mDisplayName = name;
    		mLastName = "";
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
        	buf.append(mRanking).append(" ");
        	if (isDlist())
        		buf.append(getDisplayName()).append(" (dlist)");
        	else
        		buf.append(getEmail());
        	buf.append(" (").append(mFolderId).append(")");
        }
	}
    
    public static final int FOLDER_ID_GAL = 0;
    public static final int FOLDER_ID_UNKNOWN = -1;
    
    private String mAccountId;
    private static final byte[] CONTACT_TYPES = new byte[] { MailItem.TYPE_CONTACT };
    
    private Collection<Integer> mDefaultFolders;
    private Collection<String> mEmailKeys;
    
    private static final Integer[] DEFAULT_FOLDERS = {
    	FOLDER_ID_UNKNOWN, FOLDER_ID_GAL
    };
	private static final String[] DEFAULT_EMAIL_KEYS = {
		Contact.A_email, Contact.A_email2, Contact.A_email3
	};
	
	
	public ContactAutoComplete(String accountId) {
		Provisioning prov = Provisioning.getInstance();
		try {
			Account acct = prov.get(Provisioning.AccountBy.id, accountId);
			String defaultFolders = acct.getAttr(Provisioning.A_zimbraContactAutoCompleteFolderIds);
			if (defaultFolders != null) {
				mDefaultFolders = new ArrayList<Integer>();
				for (String fid : defaultFolders.split(","))
					mDefaultFolders.add(Integer.parseInt(fid));
			}
			String emailKeys = acct.getAttr(Provisioning.A_zimbraContactAutoCompleteEmailFields);
			if (emailKeys != null)
				mEmailKeys = Arrays.asList(emailKeys.split(","));
		} catch (ServiceException se) {
			ZimbraLog.gal.warn("error initializing ContactAutoComplete", se);
		}
		mAccountId = accountId;
		if (mDefaultFolders == null)
			mDefaultFolders = Arrays.asList(DEFAULT_FOLDERS);
		if (mEmailKeys == null)
			mEmailKeys = Arrays.asList(DEFAULT_EMAIL_KEYS);
	}
	
	public void addExtraEmailKey(String key) {
		mEmailKeys.add(key);
	}
	
	public AutoCompleteResult query(String str, Collection<Integer> folders, int limit) throws ServiceException {
		ZimbraLog.gal.debug("querying "+str);
		AutoCompleteResult result = new AutoCompleteResult();
		ContactRankings rankings = new ContactRankings(mAccountId);
		if (folders == null)
			folders = mDefaultFolders;
		for (ContactEntry e : rankings.query(str, folders))
			result.addEntry(e);
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
        	for (String emailKey : mEmailKeys) {
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
			
			entry.setName((String)attrs.get(Contact.A_fullName));
        	entry.mFolderId = FOLDER_ID_GAL;
        	result.addEntry(entry);
			ZimbraLog.gal.debug("adding "+entry.getEmail());
		}
	}
	
	private void queryFolder(Mailbox mbox, Mailbox.OperationContext octxt, String str, int fid, int limit, AutoCompleteResult result) throws ServiceException {
		ZimbraLog.gal.debug("querying folder "+fid);
		str = str.toLowerCase();
		String query = generateQuery(fid, str);
        ZimbraQueryResults qres = null;
        try {
			qres = mbox.search(octxt, query, CONTACT_TYPES, MailboxIndex.SortBy.NONE, 100);
            while (qres.hasNext()) {
            	if (result.entries.size() > limit) {
                	result.canBeCached = false;
            		ZimbraLog.gal.debug("exceeded request limit "+limit);
            		return;
            	}
                ZimbraHit hit = qres.getNext();
                if (hit instanceof ContactHit) {
                	Contact c = ((ContactHit) hit).getContact();
            		ZimbraLog.gal.debug("hit: "+c.getId());
                	ContactEntry entry = new ContactEntry();
                	if (c.get(Contact.A_dlist) == null) {
                    	for (String emailKey : mEmailKeys) {
                        	String email = c.get(emailKey);
                        	if (email != null && 
                        			(entry.mEmail == null || 
                        			 email.toLowerCase().startsWith(str))) {
                        		entry.mEmail = email;
                        	}
                    	}
                    	if (entry.mEmail == null) {
                    		ZimbraLog.gal.debug("contact has empty email address");
                    		continue;
                    	}
                    	// use fullName if available
                    	String fullName = c.get(Contact.A_fullName);
                    	if (fullName != null) {
                    		entry.setName(fullName);
                    		continue;
                    	}
                    	// otherwise displayName is firstName." ".lastName
                    	entry.mLastName = c.get(Contact.A_lastName);
                    	if (entry.mLastName == null)
                    		entry.mLastName = "";
                    	String firstname = c.get(Contact.A_firstName);
                    	firstname = (firstname == null) ? "" : firstname + " ";
                    	entry.mDisplayName = firstname + entry.mLastName;
                	} else {
                		// distribution list
                		entry.mDisplayName = c.get(Contact.A_nickname);
                		entry.mDlist = c.get(Contact.A_dlist);
                	}
                	entry.mFolderId = c.getFolderId();
                	result.addEntry(entry);
        			ZimbraLog.gal.debug("adding "+entry.getEmail());
                }
            }
        } catch (IOException e) {
            throw ServiceException.FAILURE(e.getMessage(), e);
        } catch (ParseException e) {
            throw ServiceException.FAILURE(e.getMessage(), e);
        } finally {
            if (qres != null)
                qres.doneWithSearchResults();
        }
	}
	
	private String generateQuery(int fid, String query) {
		StringBuilder buf = new StringBuilder();
		if (fid > 0)
			buf.append("inid:").append(fid).append(" ");
		buf.append("(#lastName:").append(query).append("*");
		buf.append(" OR #fullName:").append(query).append("*");
		buf.append(" OR #nickName:").append(query).append("*");
		buf.append(" OR #firstName:").append(query).append("*");
		for (String emailKey : mEmailKeys)
			buf.append(" OR #").append(emailKey).append(":").append(query).append("*");
		buf.append(")");
		return buf.toString();
	}
}
