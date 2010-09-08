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
import java.util.List;
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
import com.zimbra.cs.account.Provisioning.GalSearchType;
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
        public ContactRankings rankings;
        public final Collection<ContactEntry> entries;
        public boolean canBeCached;
        public final int limit;
        private final HashSet<String> keys;

        public AutoCompleteResult(int l) {
            entries = new TreeSet<ContactEntry>();
            keys = new HashSet<String>();
            canBeCached = true;
            limit = l;
        }

        public void addEntry(ContactEntry entry) {
            String key = entry.getKey();
            if (keys.contains(key)) {
                return;
            }
            if (entries.size() >= limit) {
                canBeCached = false;
                return;
            }
            int ranking = rankings.query(key);
            // if the match comes from gal or folder search
            // check the ranking table for matching email
            // address
            if (ranking > 0 && entry.mRanking == 0) {
                entry.mRanking = ranking;
                entry.mFolderId = ContactAutoComplete.FOLDER_ID_UNKNOWN;
            }
            entries.add(entry);
            keys.add(key);
        }

        public void appendEntries(AutoCompleteResult result) {
            for (ContactEntry entry : result.entries) {
                addEntry(entry);
            }
        }

    }

    public static class ContactEntry implements Comparable<ContactEntry> {
        String mEmail;
        String mDisplayName;
        String mLastName;
        String mDlist;
        boolean mIsGroup;
        ItemId mId;
        int mFolderId;
        int mRanking;
        long mLastAccessed;

        private String getKey() {
            return (mDlist != null ? mDlist : mEmail).toLowerCase();
        }

        public String getEmail() {
            if (mDlist != null) {
                return mDlist;
            }
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
        
        public boolean isGroup() {
            return mIsGroup;
        }

        public String getDisplayName() {
            return mDisplayName;
        }

        void setName(String name) {
            if (name == null) {
                name = "";
            }
            mDisplayName = name;
            mLastName = "";
            int space = name.lastIndexOf(' ');
            if (space > 0) {
                mLastName = name.substring(space+1);
            }
        }

        // ascending order
        @Override
        public int compareTo(ContactEntry that) {
            int nameCompare = this.getKey().compareToIgnoreCase(that.getKey());
            if (nameCompare == 0) {
                return 0;
            }
            // check the ranking
            int diff = that.mRanking - this.mRanking;
            if (diff != 0) {
                return diff;
            }
            // make ranked contacts more prominent, followed by
            // address book contacts then gal contacts.
            if (this.mFolderId == FOLDER_ID_GAL && that.mFolderId != FOLDER_ID_GAL ||
                    that.mFolderId == FOLDER_ID_UNKNOWN) {
                return 1;
            }
            if (this.mFolderId != FOLDER_ID_GAL && that.mFolderId == FOLDER_ID_GAL ||
                    this.mFolderId == FOLDER_ID_UNKNOWN) {
                return -1;
            }
            // alphabetical
            return nameCompare;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof ContactEntry) {
                return compareTo((ContactEntry)obj) == 0;
            }
            return false;
        }

        @Override
        public String toString() {
            StringBuilder buf = new StringBuilder();
            toString(buf);
            return buf.toString();
        }

        public void toString(StringBuilder buf) {
            buf.append(mRanking).append(" ");
            if (isDlist()) {
                buf.append(getDisplayName()).append(" (dlist)");
            } else {
                buf.append(getEmail());
            }
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

    private GalSearchType mSearchType;

    private static final String[] DEFAULT_EMAIL_KEYS = {
        ContactConstants.A_email, ContactConstants.A_email2, ContactConstants.A_email3
    };


    public ContactAutoComplete(String accountId) {
        Provisioning prov = Provisioning.getInstance();
        try {
            Account acct = prov.get(Provisioning.AccountBy.id, accountId);
            mIncludeSharedFolders = acct.getBooleanAttr(Provisioning.A_zimbraPrefSharedAddrBookAutoCompleteEnabled, false);
            String emailKeys = acct.getAttr(Provisioning.A_zimbraContactEmailFields);
            if (emailKeys != null) {
                mEmailKeys = Arrays.asList(emailKeys.split(","));
            }
            mIncludeGal = acct.getBooleanAttr(Provisioning.A_zimbraPrefGalAutoCompleteEnabled , false);
        } catch (ServiceException se) {
            ZimbraLog.gal.warn("error initializing ContactAutoComplete", se);
        }
        mAccountId = accountId;
        if (mEmailKeys == null) {
            mEmailKeys = Arrays.asList(DEFAULT_EMAIL_KEYS);
        }
        mSearchType = GalSearchType.account;
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

    public void setSearchType(GalSearchType type) {
        mSearchType = type;
    }

    public AutoCompleteResult query(String str, Collection<Integer> folders, int limit) throws ServiceException {
        ZimbraLog.gal.debug("querying " + str);
        long t0 = System.currentTimeMillis();
        AutoCompleteResult result = new AutoCompleteResult(limit);
        result.rankings = new ContactRankings(mAccountId);
        if (limit <= 0) {
            return result;
        }

        if (result.entries.size() >= limit) {
            return result;
        }

        // query ranking table
        for (ContactEntry entry : result.rankings.search(str)) {
            result.addEntry(entry);
        }
        long t1 = System.currentTimeMillis();

        // search other folders
        if (result.entries.size() < limit) {
            queryFolders(str, folders, limit, result);
        }
        long t2 = System.currentTimeMillis();

        if (mIncludeGal && result.entries.size() < limit) {
            queryGal(str, result);
        }
        long t3 = System.currentTimeMillis();

        ZimbraLog.gal.info("autocomplete: overall="+(t3-t0)+"ms, ranking="+(t1-t0)+"ms, folder="+(t2-t1)+"ms, gal="+(t3-t2)+"ms");
        return result;
    }

    private void queryGal(String str, AutoCompleteResult result) throws ServiceException {
        Provisioning prov = Provisioning.getInstance();
        Account account = prov.get(Provisioning.AccountBy.id, mAccountId);
        ZimbraLog.gal.debug("querying gal");
        GalSearchParams params = new GalSearchParams(account);
        params.setQuery(str);
        params.setType(mSearchType);
        params.setLimit(200);
        params.setResultCallback(new AutoCompleteCallback(str, result, params));
        try {
            try {
                GalSearchControl gal = new GalSearchControl(params);
                gal.autocomplete();
            } catch (ServiceException e) {
                if (ServiceException.PERM_DENIED.equals(e.getCode())) {
                    ZimbraLog.gal.debug("cannot autocomplete gal:" + e.getMessage()); // do not log stack
                } else {
                    throw e;
                }
            }
        } catch (Exception e) {
            ZimbraLog.gal.warn("cannot autocomplete gal", e);
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

        public void handleContactAttrs(Map<String,? extends Object> attrs) {
            addMatchedContacts(str, attrs, FOLDER_ID_GAL, null, result);
        }

        @Override
        public Element handleContact(Contact c) throws ServiceException {
            ZimbraLog.gal.debug("gal entry: "+""+c.getId());
            handleContactAttrs(c.getFields());
            return null;
        }

        @Override
        public void visit(GalContact c) throws ServiceException {
            ZimbraLog.gal.debug("gal entry: "+""+c.getId());
            handleContactAttrs(c.getAttrs());
        }

        @Override
        public void handleElement(Element e) throws ServiceException {
            ZimbraLog.gal.debug("gal entry: "+""+e.getAttribute(MailConstants.A_ID));
            handleContactAttrs(parseContactElement(e));
        }

        @Override
        public void setSortBy(String sortBy) {
        }

        @Override
        public void setQueryOffset(int offset) {
        }

        @Override
        public void setHasMoreResult(boolean more) {
        }
    }
    private boolean matches(String query, String text) {
        if (query == null || text == null) {
            return false;
        }
        return text.toLowerCase().startsWith(query);
    }

    public void addMatchedContacts(String query, Map<String,? extends Object> attrs, int folderId, ItemId id, AutoCompleteResult result) {
        if (!result.canBeCached) {
            return;
        }

        Provisioning prov = Provisioning.getInstance();
        Account acct = null;
        try {
            acct = prov.getAccountById(mAccountId);
        } catch (ServiceException e) {
            ZimbraLog.gal.warn("can't get owner's account for id %s", mAccountId, e);
        }

        String firstName = (String) attrs.get(ContactConstants.A_firstName);
        String lastName = (String) attrs.get(ContactConstants.A_lastName);
        String middleName = (String) attrs.get(ContactConstants.A_middleName);
        String fullName = (String) attrs.get(ContactConstants.A_fullName);
        String nickname = (String) attrs.get(ContactConstants.A_nickname);
        String firstLastName = ((firstName == null) ? "" : firstName + " ") + lastName;
        if (fullName == null) {
            fullName = ((firstName == null) ? "" : firstName + " ") +
                    ((middleName == null) ? "" : middleName + " ") +
                    ((lastName == null) ? "" : lastName);
        }
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
                String email = (String) attrs.get(emailKey);
                if (email != null && (nameMatches || matches(query, email))) {
                    ContactEntry entry = new ContactEntry();
                    entry.mEmail = email;
                    entry.setName(fullName);
                    entry.mId = id;
                    entry.mFolderId = folderId;
                    entry.mIsGroup = isGroup(attrs);
                    result.addEntry(entry);
                    ZimbraLog.gal.debug("adding " + entry.getEmail());
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
            entry.mDlist = (String) attrs.get(ContactConstants.A_dlist);
            entry.mId = id;
            entry.mFolderId = folderId;
            entry.mIsGroup = isGroup(attrs);
            result.addEntry(entry);
            ZimbraLog.gal.debug("adding " + entry.getEmail());
        }
    }
    
    private boolean isGroup(Map<String,? extends Object> attrs) {
        return ContactConstants.TYPE_GROUP.equals((String) attrs.get(ContactConstants.A_type));
    }

    private void queryFolders(String str, Collection<Integer> folderIDs, int limit, AutoCompleteResult result) throws ServiceException {
        str = str.toLowerCase();
        ZimbraQueryResults qres = null;
        try {
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(mAccountId);
            OperationContext octxt = new OperationContext(mbox);
            List<Folder> folders = new ArrayList<Folder>();
            Map<ItemId, Mountpoint> mountpoints = new HashMap<ItemId, Mountpoint>();
            if (folderIDs == null) {
                for (Folder folder : mbox.getFolderList(octxt, SortBy.NONE)) {
                    if (folder.getDefaultView() != MailItem.TYPE_CONTACT ||
                            folder.inTrash()) {
                        continue;
                    } else if (folder instanceof Mountpoint) {
                        Mountpoint mp = (Mountpoint) folder;
                        mountpoints.put(mp.getTarget(), mp);
                        if (mIncludeSharedFolders) {
                            folders.add(folder);
                        }
                    } else {
                        folders.add(folder);
                    }
                }
            } else {
                for (int fid : folderIDs) {
                    Folder folder = mbox.getFolderById(octxt, fid);
                    folders.add(folder);
                    if (folder instanceof Mountpoint) {
                        Mountpoint mp = (Mountpoint) folder;
                        mountpoints.put(mp.getTarget(), mp);
                    }
                }
            }
            String query = generateQuery(str, folders);
            ZimbraLog.gal.debug("querying folders: " + query);
            qres = mbox.search(octxt, query, CONTACT_TYPES, SortBy.NONE, limit + 1);
            while (qres.hasNext()) {
                ZimbraHit hit = qres.getNext();
                Map<String,String> fields = null;
                ItemId id = null;
                int fid = 0;
                if (hit instanceof ContactHit) {
                    Contact c = ((ContactHit) hit).getContact();
                    ZimbraLog.gal.debug("hit: " + c.getId());
                    fields = c.getFields();
                    id = new ItemId(c);
                    fid = c.getFolderId();
                } else if (hit instanceof ProxiedHit) {
                    fields = new HashMap<String, String>();
                    Element top = ((ProxiedHit) hit).getElement();
                    id = new ItemId(top.getAttribute(MailConstants.A_ID), (String) null);
                    ZimbraLog.gal.debug("hit: " + id);
                    ItemId fiid = new ItemId(top.getAttribute(MailConstants.A_FOLDER), (String) null);
                    Mountpoint mp = mountpoints.get(fiid);
                    if (mp != null) {
                        // if the hit came from a descendant folder of
                        // the mountpoint, we don't have a peer folder ID.
                        fid = mp.getId();
                    }
                    for (Element elt : top.listElements(MailConstants.E_ATTRIBUTE)) {
                        try {
                            String name = elt.getAttribute(MailConstants.A_ATTRIBUTE_NAME);
                            fields.put(name, elt.getText());
                        } catch (ServiceException se) {
                            ZimbraLog.gal.warn("error handling proxied query result " + hit);
                        }
                    }
                } else {
                    continue;
                }

                addMatchedContacts(str, fields, fid, id, result);
                if (!result.canBeCached) {
                    return;
                }
            }
        } catch (IOException e) {
            throw ServiceException.FAILURE(e.getMessage(), e);
        } catch (ParseException e) {
            throw ServiceException.FAILURE(e.getMessage(), e);
        } finally {
            if (qres != null) {
                qres.doneWithSearchResults();
            }
        }
    }

    private String generateQuery(String query, Collection<Folder> folders) {
        StringBuilder buf = new StringBuilder("(");
        boolean first = true;
        for (Folder folder : folders) {
            int fid = folder.getId();
            if (fid < 1) {
                continue;
            }
            if (first) {
                first = false;
            } else {
                buf.append(" OR ");
            }
            // include descendant folders if mountpoint
            buf.append(folder instanceof Mountpoint ? "underid:" : "inid:");
            buf.append(fid);
        }
        buf.append(") AND contact:(").append(query).append(")");
        return buf.toString();
    }
}
