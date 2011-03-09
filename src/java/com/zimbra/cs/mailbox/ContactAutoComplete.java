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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import com.zimbra.cs.gal.GalGroup;
import com.zimbra.cs.gal.GalGroupInfoProvider;
import com.zimbra.cs.gal.GalSearchControl;
import com.zimbra.cs.gal.GalSearchParams;
import com.zimbra.cs.gal.GalSearchResultCallback;
import com.zimbra.cs.index.ContactHit;
import com.zimbra.cs.index.ProxiedHit;
import com.zimbra.cs.index.SortBy;
import com.zimbra.cs.index.ZimbraHit;
import com.zimbra.cs.index.ZimbraQueryResults;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.soap.ZimbraSoapContext;

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
            if (entry.mRanking == 0) {
                // if the match comes from gal or folder search
                // check the ranking table for matching email
                // address
                int ranking = rankings.query(key);
                if (ranking > 0)
                    entry.mRanking = ranking;
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
        boolean mCanExpandGroupMembers;
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

        public boolean canExpandGroupMembers() {
            return mCanExpandGroupMembers;
        }

        public String getDisplayName() {
            return mDisplayName;
        }

        void setIsGalGroup(String email, Map<String,? extends Object> attrs, Account authedAcct, boolean needCanExpand) {
            setIsGalGroup(email, (String)attrs.get(ContactConstants.A_zimbraId), authedAcct, needCanExpand);
        }

        void setIsGalGroup(String email, String zimbraId, Account authedAcct, boolean needCanExpand) {
            boolean canExpand = false;
            if (needCanExpand)
                canExpand = GalSearchControl.canExpandGalGroup(email, zimbraId, authedAcct);
            setIsGalGroup(canExpand);
        }
        
        void setIsGalGroup(boolean canExpand) {
            mIsGroup = true;
            mCanExpandGroupMembers = canExpand;
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

    private boolean mIncludeGal;
    private boolean mNeedCanExpand; // whether the canExpand info is needed for GAL groups

    private static final Set<MailItem.Type> CONTACT_TYPES = EnumSet.of(MailItem.Type.CONTACT);

    private boolean mIncludeSharedFolders;
    private Collection<String> mEmailKeys;

    private GalSearchType mSearchType;
    private ZimbraSoapContext mZsc;
    private Account mAuthedAcct;
    private Account mRequestedAcct;
    

    private static final String[] DEFAULT_EMAIL_KEYS = {
        ContactConstants.A_email, ContactConstants.A_email2, ContactConstants.A_email3
    };


    public ContactAutoComplete(Account acct) {
        this (acct, null);
    }

    public ContactAutoComplete(Account acct, ZimbraSoapContext zsc) {
        mZsc = zsc;
        try {
            mRequestedAcct = acct;
            mIncludeSharedFolders = mRequestedAcct.getBooleanAttr(Provisioning.A_zimbraPrefSharedAddrBookAutoCompleteEnabled, false);
            String emailKeys = mRequestedAcct.getAttr(Provisioning.A_zimbraContactEmailFields);
            if (emailKeys != null) {
                mEmailKeys = Arrays.asList(emailKeys.split(","));
            }
            mIncludeGal = mRequestedAcct.getBooleanAttr(Provisioning.A_zimbraPrefGalAutoCompleteEnabled , false);

            if (mZsc != null) {
                String authedAcctId = mZsc.getAuthtokenAccountId();
                if (authedAcctId != null)
                    mAuthedAcct = Provisioning.getInstance().get(Provisioning.AccountBy.id, authedAcctId);
            }
            if (mAuthedAcct == null)
                mAuthedAcct = mRequestedAcct;

        } catch (ServiceException se) {
            ZimbraLog.gal.warn("error initializing ContactAutoComplete", se);
        }
        if (mEmailKeys == null) {
            mEmailKeys = Arrays.asList(DEFAULT_EMAIL_KEYS);
        }
        mSearchType = GalSearchType.account;
    }
    
    private String getRequestedAcctId() {
        return mRequestedAcct.getId();
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

    public void setNeedCanExpand(boolean needCanExpand) {
        mNeedCanExpand = needCanExpand;
    }

    public void setSearchType(GalSearchType type) {
        mSearchType = type;
    }

    public AutoCompleteResult query(String str, Collection<Integer> folders, int limit) throws ServiceException {
        ZimbraLog.gal.debug("AutoComplete querying: " + str);
        long t0 = System.currentTimeMillis();
        AutoCompleteResult result = new AutoCompleteResult(limit);
        result.rankings = new ContactRankings(getRequestedAcctId());
        if (limit <= 0) {
            return result;
        }

        if (result.entries.size() >= limit) {
            return result;
        }

        // query ranking table
        Collection<ContactEntry> rankingTableMatches = result.rankings.search(str);

        if (!rankingTableMatches.isEmpty()) {
            for (ContactEntry entry : rankingTableMatches) {
                String emailAddr = entry.getKey();
                resolveGroupInfo(entry, emailAddr);
                result.addEntry(entry);
            }
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
    
    /**
     * ranking table and local contact matches don't have group indicator persisted on them, 
     * cross-ref GAL to check if the address is a group.
     * 
     * If the address is a group, set group info in the ContactEntry object.  Also, change the 
     * folder ID to GAL.  Client relies on this to display the expand icon, otherwise it would 
     * consider the entry a local contact group and will not offer to expand it.
     * 
     * @param entry
     * @param email
     * @return true if the address is a group, false otherwise
     */
    private void resolveGroupInfo(ContactEntry entry, String email) {
        GalGroup.GroupInfo groupInfo = GalGroupInfoProvider.getInstance().getGroupInfo(email, mNeedCanExpand, mRequestedAcct, mAuthedAcct);
        if (groupInfo != null) {
            boolean canExpand = (GalGroup.GroupInfo.CAN_EXPAND == groupInfo);
            entry.setIsGalGroup(canExpand);
            
            // set folder ID to GAL, client relies on this to display the expand icon
            entry.mFolderId = FOLDER_ID_GAL;
        }
    }

    private void queryGal(String str, AutoCompleteResult result) throws ServiceException {
        ZimbraLog.gal.debug("querying gal");
        GalSearchParams params = new GalSearchParams(mRequestedAcct, mZsc);
        params.setQuery(str);
        params.setType(mSearchType);
        params.setLimit(200);
        params.setNeedCanExpand(mNeedCanExpand);
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
        int space = query.indexOf(' ');
        if (space > 0)
            return matches(query.substring(0, space).trim(), text) || matches(query.substring(space + 1).trim(), text);
        else
            return text.toLowerCase().startsWith(query.toLowerCase());
    }

    public void addMatchedContacts(String query, Map<String,? extends Object> attrs, int folderId, ItemId id, AutoCompleteResult result) {
        if (!result.canBeCached) {
            return;
        }

        String firstName = (String) attrs.get(ContactConstants.A_firstName);
        String phoneticFirstName = (String) attrs.get(ContactConstants.A_phoneticFirstName);
        String lastName = (String) attrs.get(ContactConstants.A_lastName);
        String phoneticLastName = (String) attrs.get(ContactConstants.A_phoneticLastName);
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
                matches(query, phoneticFirstName) ||
                matches(query, lastName) ||
                matches(query, phoneticLastName) ||
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
                    if (Contact.isGroup(attrs)) {
                        entry.setIsGalGroup(email, attrs, mAuthedAcct, mNeedCanExpand);
                    } else if (entry.mFolderId != FOLDER_ID_GAL) {
                        // is a local contact
                        // bug 55673, check if the addr is a group
                        resolveGroupInfo(entry, email);
                    }
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
            //
            // is a local contact group
            //

            if (mRequestedAcct.isPrefContactsDisableAutocompleteOnContactGroupMembers() &&
                    !matches(query, nickname)) {
                return;
            }
            // distribution list
            ContactEntry entry = new ContactEntry();
            entry.mDisplayName = nickname;
            entry.mDlist = (String) attrs.get(ContactConstants.A_dlist);
            entry.mId = id;
            entry.mFolderId = folderId;
            entry.mIsGroup = Contact.isGroup(attrs);
            result.addEntry(entry);
            ZimbraLog.gal.debug("adding " + entry.getEmail());
        }
    }

    private void queryFolders(String str, Collection<Integer> folderIDs, int limit, AutoCompleteResult result) throws ServiceException {
        str = str.toLowerCase();
        ZimbraQueryResults qres = null;
        try {
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(getRequestedAcctId());
            OperationContext octxt = (mZsc == null) ?
                    new OperationContext(mbox) :
                    new OperationContext(mZsc.getAuthtokenAccountId());
            List<Folder> folders = new ArrayList<Folder>();
            Map<ItemId, Mountpoint> mountpoints = new HashMap<ItemId, Mountpoint>();
            if (folderIDs == null) {
                for (Folder folder : mbox.getFolderList(octxt, SortBy.NONE)) {
                    if (folder.getDefaultView() != MailItem.Type.CONTACT || folder.inTrash()) {
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
            ZimbraLog.gal.debug("querying contact folders: " + query);
            qres = mbox.index.search(octxt, query, CONTACT_TYPES, SortBy.NONE, limit + 1);
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

        buf.append(") AND contact:\"");
        buf.append(query.replace("\"", "\\\"")); // escape quotes
        buf.append("\"");
        return buf.toString();
    }
}
