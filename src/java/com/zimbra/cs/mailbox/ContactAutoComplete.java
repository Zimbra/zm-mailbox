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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.GalContact;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.SearchGalResult;
import com.zimbra.cs.index.ContactHit;
import com.zimbra.cs.index.MailboxIndex;
import com.zimbra.cs.index.ProxiedHit;
import com.zimbra.cs.index.ZimbraHit;
import com.zimbra.cs.index.ZimbraQueryResults;
import com.zimbra.cs.index.queryparser.ParseException;
import com.zimbra.cs.service.util.ItemId;

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
        ItemId mId;
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
        public ItemId getId() {
        	return mId;
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
    private boolean mIncludeGal;
    
    private static final byte[] CONTACT_TYPES = new byte[] { MailItem.TYPE_CONTACT };
    
    private Collection<Integer> mDefaultFolders;
    private Collection<String> mEmailKeys;
    
    private static final Integer[] DEFAULT_FOLDERS = {
    	Mailbox.ID_FOLDER_CONTACTS, FOLDER_ID_GAL
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
		if (mDefaultFolders.contains(FOLDER_ID_GAL)) {
			mDefaultFolders.remove(FOLDER_ID_GAL);
			mIncludeGal = true;
		}
		if (mEmailKeys == null)
			mEmailKeys = Arrays.asList(DEFAULT_EMAIL_KEYS);
	}
	
	public void addExtraEmailKey(String key) {
		mEmailKeys.add(key);
	}
	
	public boolean includeGal() {
		return mIncludeGal;
	}
	public void setIncludeGal(boolean includeGal) {
		mIncludeGal = includeGal;
	}
	
	public AutoCompleteResult query(String str, Collection<Integer> folders, int limit) throws ServiceException {
		ZimbraLog.gal.debug("querying "+str);
		AutoCompleteResult result = new AutoCompleteResult();
		if (limit <= 0)
			return result;
		if (folders == null)
			folders = mDefaultFolders;
		
		queryRankingTable(str, folders, limit, result);
		if (result.entries.size() >= limit)
			return result;
		
		// search other folders
		if (mIncludeGal)
			queryGal(str, limit, result);
		if (result.entries.size() >= limit)
			return result;
		
		queryFolders(str, folders, limit, result);
		return result;
	}
	
	private void queryRankingTable(String str, Collection<Integer> folders, int limit, AutoCompleteResult result) throws ServiceException {
		ContactRankings rankings = new ContactRankings(mAccountId);
		for (ContactEntry e : rankings.query(str, folders)) {
			result.addEntry(e);
			if (result.entries.size() == limit) {
				result.canBeCached = false;
				break;
			}
		}
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
        SearchGalResult sgr = prov.autoCompleteGal(d, str, type, limit - result.entries.size());
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
	
	private boolean matches(String query, String text) {
		if (query == null || text == null)
			return false;
		return text.toLowerCase().startsWith(query);
	}
	
	private void queryFolders(String str, Collection<Integer> folders, int limit, AutoCompleteResult result) throws ServiceException {
		str = str.toLowerCase();
		String query = generateQuery(str, folders);
		ZimbraLog.gal.debug("querying folders: "+query);
        ZimbraQueryResults qres = null;
        try {
    		Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(mAccountId);
    		Mailbox.OperationContext octxt = new Mailbox.OperationContext(mbox);
    		HashMap<ItemId,Integer> mountpoints = new HashMap<ItemId,Integer>();
    		for (int fid : folders) {
    			Folder f = mbox.getFolderById(octxt, fid);
    			if (f instanceof Mountpoint) {
    				Mountpoint mp = (Mountpoint) f;
    				mountpoints.put(new ItemId(mp.getOwnerId(), mp.getRemoteId()), fid);
    			}
    		}
			qres = mbox.search(octxt, query, CONTACT_TYPES, MailboxIndex.SortBy.NONE, 100);
            while (qres.hasNext()) {
                ZimbraHit hit = qres.getNext();
                Map<String,String> fields = null;
                ItemId id = null;
                int folderId = 0;
                if (hit instanceof ContactHit) {
                    Contact c = ((ContactHit) hit).getContact();
                    ZimbraLog.gal.debug("hit: "+c.getId());
                	fields = c.getFields();
                	id = new ItemId(c);
                	folderId = c.getFolderId();
                } else if (hit instanceof ProxiedHit) {
                    fields = new HashMap<String, String>();
                    Element top = ((ProxiedHit)hit).getElement();
                    id = new ItemId(top.getAttribute(MailConstants.A_ID), (String) null);
                    ZimbraLog.gal.debug("hit: "+id);
                    ItemId fiid = new ItemId(top.getAttribute(MailConstants.A_FOLDER), (String) null);
                    folderId = mountpoints.get(fiid);
                    for (Element elt : top.listElements(MailConstants.E_ATTRIBUTE)) {
                    	try {
                            String name = elt.getAttribute(MailConstants.A_ATTRIBUTE_NAME);
                            fields.put(name, elt.getText());
                    	} catch (ServiceException se) {
                			ZimbraLog.gal.warn("error handling proxied query result "+hit);
                    	}
                    }
                } else
                	continue;
                
            	String firstName = fields.get(Contact.A_firstName);
            	String lastName = fields.get(Contact.A_lastName);
            	String fullName = fields.get(Contact.A_fullName);
            	String nickname = fields.get(Contact.A_nickname);
                if (fields.get(Contact.A_dlist) == null) {
                	boolean nameMatches = 
                		matches(str, firstName) ||
                        matches(str, lastName) ||
                        matches(str, fullName) ||
                        matches(str, nickname);
                				
                	for (String emailKey : mEmailKeys) {
                		String email = fields.get(emailKey);
                		if (email != null && (nameMatches || matches(str, email))) {
                			ContactEntry entry = new ContactEntry();
                			entry.mEmail = email;
                			// use fullName if available
                			if (fullName != null) {
                				entry.setName(fullName);
                			} else {
                				// otherwise displayName is firstName." ".lastName
                				entry.mLastName = lastName;
                				if (entry.mLastName == null)
                					entry.mLastName = "";
                				entry.mDisplayName = (firstName == null) ? "" : firstName + " " + entry.mLastName;
                			}
                			entry.mId = id;
                			entry.mFolderId = folderId;
                			result.addEntry(entry);
                			ZimbraLog.gal.debug("adding "+entry.getEmail());
                		}
                	}
                } else {
                	// distribution list
                	ContactEntry entry = new ContactEntry();
                	entry.mDisplayName = nickname;
                	entry.mDlist = fields.get(Contact.A_dlist);
                	entry.mId = id;
                	entry.mFolderId = folderId;
                	result.addEntry(entry);
                	ZimbraLog.gal.debug("adding "+entry.getEmail());
                }
    			if (result.entries.size() == limit) {
            		ZimbraLog.gal.debug("mbox query result exceeded request limit "+limit);
    				result.canBeCached = false;
    				break;
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
	
	private String generateQuery(String query, Collection<Integer> folders) {
		StringBuilder buf = new StringBuilder();
		boolean first = true;
		buf.append("(");
		for (int fid : folders) {
			if (fid < 1)
				continue;
			if (!first)
				buf.append(" OR ");
			first = false;
			buf.append("inid:").append(fid);
		}
		buf.append(")");
		
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
