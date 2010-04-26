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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeSet;

import com.zimbra.common.mailbox.ContactConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.GalContact;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.GAL_SEARCH_TYPE;
import com.zimbra.cs.gal.GalSearchControl;
import com.zimbra.cs.gal.GalSearchParams;
import com.zimbra.cs.gal.GalSearchResultCallback;
import com.zimbra.cs.index.ContactHit;
import com.zimbra.cs.index.ProxiedHit;
import com.zimbra.cs.index.SortBy;
import com.zimbra.cs.index.ZimbraHit;
import com.zimbra.cs.index.ZimbraQueryResults;
import com.zimbra.cs.index.queryparser.ParseException;
import com.zimbra.cs.service.util.ItemId;

public class ContactAutoComplete {
	public static class AutoCompleteResult {
		public Collection<ContactEntry> entries;
		public boolean canBeCached;
		public int limit;
		public AutoCompleteResult(int l) { 
			entries = new TreeSet<ContactEntry>(); 
			emails = new HashSet<String>();
			canBeCached = true;
			limit = l;
		}
		public void addEntry(ContactEntry entry) {
			if (entries.size() >= limit) {
			    canBeCached = false;
                return;
			}
			String email;
			if (entry.isDlist())
				email = entry.mDisplayName;
			else
				email = entry.mEmail.toLowerCase();
			entry.mRanking = rankings.query(email);
			if (!emails.contains(email)) {
				entries.add(entry);
				emails.add(email);
			}
		}
		public void appendEntries(AutoCompleteResult result) {
		    for (ContactEntry entry : result.entries)
		        addEntry(entry);
		}
		private HashSet<String> emails;
		private ContactRankings rankings;
	}
    public static class ContactEntry implements Comparable<ContactEntry> {
        String mEmail;
        String mDisplayName;
        String mLastName;
        String mDlist;
        ItemId mId;
        int mFolderId;
        int mRanking;
        long mLastAccessed;
        
        public String getEmail() {
        	if (mDlist != null)
        		return mDlist;
        	StringBuilder buf = new StringBuilder();
        	if (mDisplayName != null && mDisplayName.length() > 0) {
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
        // ascending order
        public int compareTo(ContactEntry that) {
            // check the ranking
        	int diff = that.mRanking - this.mRanking;
        	if (diff != 0)
            	return diff;
        	// make addressbook contacts more prominent than gal contact
        	if (this.mFolderId == FOLDER_ID_GAL && that.mFolderId != FOLDER_ID_GAL)
        	    return 1;
            if (this.mFolderId != FOLDER_ID_GAL && that.mFolderId == FOLDER_ID_GAL)
                return -1;
            // alphabetical
        	return this.getEmail().compareToIgnoreCase(that.getEmail());
        }
        public boolean equals(Object obj) {
        	if (obj instanceof ContactEntry)
        		return compareTo((ContactEntry)obj) == 0;
        	return false;
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
        	buf.append(" ").append(new Date(mLastAccessed));
        }
	}
    
    public static final int FOLDER_ID_GAL = 0;
    public static final int FOLDER_ID_UNKNOWN = -1;
    
    private String mAccountId;
    private boolean mIncludeGal;
    
    private static final byte[] CONTACT_TYPES = new byte[] { MailItem.TYPE_CONTACT };
    
    private boolean mIncludeSharedFolders;
    private Collection<String> mEmailKeys;
    
    private GAL_SEARCH_TYPE mSearchType;
    
	private static final String[] DEFAULT_EMAIL_KEYS = {
		ContactConstants.A_email, ContactConstants.A_email2, ContactConstants.A_email3
	};
	
	
	public ContactAutoComplete(String accountId) {
		Provisioning prov = Provisioning.getInstance();
		try {
			Account acct = prov.get(Provisioning.AccountBy.id, accountId);
			mIncludeSharedFolders = acct.getBooleanAttr(Provisioning.A_zimbraPrefSharedAddrBookAutoCompleteEnabled, false);
			String emailKeys = acct.getAttr(Provisioning.A_zimbraContactEmailFields);
			if (emailKeys != null)
				mEmailKeys = Arrays.asList(emailKeys.split(","));
	        mIncludeGal = acct.getBooleanAttr(Provisioning.A_zimbraFeatureGalAutoCompleteEnabled , false) &&
	                acct.getBooleanAttr(Provisioning.A_zimbraFeatureGalEnabled , false) &&
	                acct.getBooleanAttr(Provisioning.A_zimbraPrefGalAutoCompleteEnabled , false);
		} catch (ServiceException se) {
			ZimbraLog.gal.warn("error initializing ContactAutoComplete", se);
		}
		mAccountId = accountId;
		if (mEmailKeys == null)
			mEmailKeys = Arrays.asList(DEFAULT_EMAIL_KEYS);
		mSearchType = GAL_SEARCH_TYPE.USER_ACCOUNT;
	}
	
	public Collection<String> getEmailKeys() {
		return mEmailKeys;
	}
	public boolean includeGal() {
		return mIncludeGal;
	}
	public void setIncludeGal(boolean includeGal) {
		mIncludeGal = includeGal;
	}
	public void setSearchType(GAL_SEARCH_TYPE type) {
	    mSearchType = type;
	}
	
	public AutoCompleteResult query(String str, Collection<Integer> folders, int limit) throws ServiceException {
		ZimbraLog.gal.debug("querying "+str);
		long t0 = System.currentTimeMillis();
		AutoCompleteResult result = new AutoCompleteResult(limit);
		result.rankings = new ContactRankings(mAccountId);
		if (limit <= 0)
			return result;
		
		if (result.entries.size() >= limit)
			return result;
		long t1 = System.currentTimeMillis();
		
		// search other folders
		queryFolders(str, folders, limit, result);
		if (result.entries.size() >= limit)
			return result;
		long t2 = System.currentTimeMillis();
		
		if (mIncludeGal)
			queryGal(str, limit, result);
		long t3 = System.currentTimeMillis();
		
		ZimbraLog.gal.info("autocomplete: overall="+(t3-t0)+"ms, ranking="+(t1-t0)+"ms, folder="+(t2-t1)+"ms, gal="+(t3-t2)+"ms");
		return result;
	}
	
	private void queryGal(String str, int limit, AutoCompleteResult result) throws ServiceException {
		Provisioning prov = Provisioning.getInstance();
		Account account = prov.get(Provisioning.AccountBy.id, mAccountId);
		ZimbraLog.gal.debug("querying gal");
		GalSearchParams params = new GalSearchParams(account);
		params.setQuery(str);
		params.setType(mSearchType);
		params.setLimit(200);
		params.setResultCallback(new AutoCompleteCallback(str, result, params));
		try {
	        GalSearchControl gal = new GalSearchControl(params);
	        gal.autocomplete();
		} catch (Exception e) {
    		ZimbraLog.gal.warn("can't gal search", e);
    		return;
		}
	}
	
	private class AutoCompleteCallback extends GalSearchResultCallback {
		AutoCompleteResult result;
		String str;
		
	    public AutoCompleteCallback(String str, AutoCompleteResult result, GalSearchParams params) {
	    	super(params);
	    	this.result = result;
	    	this.str = str;
	    }
	    public void handleContactAttrs(Map<String,? extends Object> attrs) throws ServiceException {
	    	addMatchedContacts(str, attrs, FOLDER_ID_GAL, null, result);
	    }
	    public Element handleContact(Contact c) throws ServiceException {
			ZimbraLog.gal.debug("gal entry: "+""+c.getId());
	        handleContactAttrs(c.getFields());
	        return null;
	    }
	    public void visit(GalContact c) throws ServiceException {
			ZimbraLog.gal.debug("gal entry: "+""+c.getId());
	        handleContactAttrs(c.getAttrs());
	    }
	    public void handleElement(Element e) throws ServiceException {
			ZimbraLog.gal.debug("gal entry: "+""+e.getAttribute(MailConstants.A_ID));
	        handleContactAttrs(parseContactElement(e));
	    }
	    public void setSortBy(String sortBy) {
	    }
	    public void setQueryOffset(int offset) {
	    }
	    public void setHasMoreResult(boolean more) {
	    }
	}
	private boolean matches(String query, String text) {
		if (query == null || text == null)
			return false;
		return text.toLowerCase().startsWith(query);
	}
	
	public void addMatchedContacts(String query, Map<String,? extends Object> attrs, int folderId, ItemId id, AutoCompleteResult result) {
	    if (!result.canBeCached)
	        return;
	    
	    Provisioning prov = Provisioning.getInstance();
	    Account acct = null;
	    try {
            acct = prov.getAccountById(mAccountId);
        } catch (ServiceException e) {
            ZimbraLog.gal.warn("can't get owner's account for id %s", mAccountId, e);
        }
	    
    	String firstName = (String)attrs.get(ContactConstants.A_firstName);
    	String lastName = (String)attrs.get(ContactConstants.A_lastName);
        String middleName = (String)attrs.get(ContactConstants.A_middleName);
    	String fullName = (String)attrs.get(ContactConstants.A_fullName);
    	String nickname = (String)attrs.get(ContactConstants.A_nickname);
    	String firstLastName = ((firstName == null) ? "" : firstName + " ") + lastName;
    	if (fullName == null)
    	    fullName = ((firstName == null) ? "" : firstName + " ") + 
    	            ((middleName == null) ? "" : middleName + " ") +
    	            ((lastName == null) ? "" : lastName);
        if (attrs.get(ContactConstants.A_dlist) == null) {
        	boolean nameMatches = 
        		matches(query, firstName) ||
                matches(query, lastName) ||
                matches(query, fullName) ||
                matches(query, firstLastName) ||
                matches(query, nickname);
        	
        	// matching algorithm is slightly different between matching
        	// personal Contacts in the addressbook vs GAL entry if there is
        	// multiple email address associated to the entry.  multiple
        	// email address in Contact typically means alternative email
        	// address, such as work email, home email, etc.  however in GAL,
        	// multiple email address indicates an alias to the same contact
        	// object.  for Contacts we want to show all the email addresses
        	// available for the Contact entry.  but for GAL we need to show
        	// just one email address.
        		
        	for (String emailKey : mEmailKeys) {
        		String email = (String)attrs.get(emailKey);
        		if (email != null && (nameMatches || matches(query, email))) {
        			ContactEntry entry = new ContactEntry();
        			entry.mEmail = email;
                    entry.setName(fullName);
        			entry.mId = id;
        			entry.mFolderId = folderId;
        			result.addEntry(entry);
        			ZimbraLog.gal.debug("adding "+entry.getEmail());
        			if (folderId == FOLDER_ID_GAL) {
        				// we've matched the first email address for this 
        				// GAL contact.  move onto the next contact.
        				return;
        			}
        		}
        	}
        } else {
            if (acct != null && 
                    acct.isPrefContactsDisableAutocompleteOnContactGroupMembers() &&
                    !matches(query, nickname)) {
                return;
            }
        	// distribution list
        	ContactEntry entry = new ContactEntry();
        	entry.mDisplayName = nickname;
        	entry.mDlist = (String)attrs.get(ContactConstants.A_dlist);
        	entry.mId = id;
        	entry.mFolderId = folderId;
        	result.addEntry(entry);
        	ZimbraLog.gal.debug("adding "+entry.getEmail());
        }
	}
	
	private void queryFolders(String str, Collection<Integer> folders, int limit, AutoCompleteResult result) throws ServiceException {
		str = str.toLowerCase();
        ZimbraQueryResults qres = null;
        try {
    		Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(mAccountId);
    		OperationContext octxt = new OperationContext(mbox);
    		HashMap<ItemId,Integer> mountpoints = new HashMap<ItemId,Integer>();
    		if (folders == null) {
    			ArrayList<Integer> allFolders = new ArrayList<Integer>();
    			for (Folder f : mbox.getFolderList(octxt, SortBy.NONE)) {
    				boolean isMountpoint = false;
    				if (f.getDefaultView() != MailItem.TYPE_CONTACT)
    					continue;
        			if (f instanceof Mountpoint) {
        				mountpoints.put(((Mountpoint) f).getTarget(), f.getId());
        				isMountpoint = true;
        			}
    				if (!isMountpoint || mIncludeSharedFolders)
    					allFolders.add(f.getId());
    			}
    			folders = allFolders;
    		} else {
        		for (int fid : folders) {
        			Folder f = mbox.getFolderById(octxt, fid);
        			if (f instanceof Mountpoint) {
        				mountpoints.put(((Mountpoint) f).getTarget(), fid);
        			}
        		}
    		}
    		String query = generateQuery(str, folders);
    		ZimbraLog.gal.debug("querying folders: "+query);
			qres = mbox.search(octxt, query, CONTACT_TYPES, SortBy.NONE, limit + 1);
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

                addMatchedContacts(str, fields, folderId, id, result);
                if (!result.canBeCached)
                    return;
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
		buf.append(") AND contact:(").append(query).append(")");
		return buf.toString();
	}
}
