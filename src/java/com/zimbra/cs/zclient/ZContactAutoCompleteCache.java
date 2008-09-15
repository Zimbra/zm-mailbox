/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
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
package com.zimbra.cs.zclient;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.Contact;
import com.zimbra.cs.zclient.ZMailbox.GalEntryType;
import com.zimbra.cs.zclient.ZMailbox.ZSearchGalResult;
import com.zimbra.cs.zclient.event.ZCreateContactEvent;
import com.zimbra.cs.zclient.event.ZCreateEvent;
import com.zimbra.cs.zclient.event.ZDeleteEvent;
import com.zimbra.cs.zclient.event.ZEventHandler;
import com.zimbra.cs.zclient.event.ZModifyContactEvent;
import com.zimbra.cs.zclient.event.ZModifyEvent;
import com.zimbra.cs.zclient.event.ZRefreshEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

public class ZContactAutoCompleteCache extends ZEventHandler {

    // tree ("first last", "last", "email", "email2", "email3" to contact)
    // workemail1 is comcast specific
    private TreeMap<String, List<ZContact>> mCache;

    // id to contact map of all contacts in our cache
    private Map<String,ZContact> mContacts;

    private Map<String, ZSearchGalResult> mGalCache;

    /** true if we have been cleared and need to refetch contacts on next auto-compleete */
    private boolean mCleared;

    public ZContactAutoCompleteCache() {
        mCache = new TreeMap<String, List<ZContact>>();
        mContacts = new HashMap<String, ZContact>();
        mGalCache = new HashMap<String, ZSearchGalResult>();
        mCleared = true;
    }

    private void addKey(String key, ZContact contact) {
        if (key == null) return;
        key = key.toLowerCase();
        List<ZContact> contacts = mCache.get(key);
        if (contacts == null) {
            contacts = new ArrayList<ZContact>();
            mCache.put(key, contacts);
        }
        if (contacts.size() == 0 || contacts.get(contacts.size()-1) != contact)
            contacts.add(contact);
    }

    private synchronized void addContact(ZContact contact) {
        mContacts.put(contact.getId(), contact);
        Map<String,String> attrs = contact.getAttrs();
        addKey(attrs.get(Contact.A_email), contact);
        addKey(attrs.get(Contact.A_email2), contact);
        addKey(attrs.get(Contact.A_email3), contact);
        addKey(attrs.get(Contact.A_workEmail1), contact);
        addKey(attrs.get(Contact.A_nickname), contact);
        String ln = attrs.get(Contact.A_lastName);
        String fn = attrs.get(Contact.A_firstName);
        if (fn != null) {
            if (ln == null) addKey(fn, contact);
            else addKey(fn+" "+ln, contact);
        }
        addKey(ln, contact);
    }

    private void removeKey(String key, ZContact contact) {
        if (key == null) return;
        key = key.toLowerCase();
        List<ZContact> contacts = mCache.get(key);
        if (contacts != null)
            contacts.remove(contact);
    }

    private synchronized void removeContact(String id) {
        ZContact contact = mContacts.get(id);
        if (contact == null) return;
        
        Map<String,String> attrs = contact.getAttrs();
        removeKey(attrs.get(Contact.A_email), contact);
        removeKey(attrs.get(Contact.A_email2), contact);
        removeKey(attrs.get(Contact.A_email3), contact);
        removeKey(attrs.get(Contact.A_workEmail1), contact);

        String ln = attrs.get(Contact.A_lastName);
        String fn = attrs.get(Contact.A_firstName);
        if (fn != null) {
            if (ln ==null) removeKey(fn, contact);
            else removeKey(fn+" "+ln, contact);
        }
        removeKey(ln, contact);
    }

    public synchronized void clear() {
        mCache.clear();
        mGalCache.clear();
        mContacts.clear();
        mCleared = true;
    }

    private synchronized void init(ZMailbox mailbox) throws ServiceException {
        List<ZContact> contacts = mailbox.getContacts(null, null, false, Arrays.asList(Contact.A_email, Contact.A_email2, Contact.A_email3, Contact.A_workEmail1, Contact.A_firstName, Contact.A_lastName, Contact.A_nickname, Contact.A_dlist));
        for (ZContact contact : contacts) {
            if (!contact.getFolderId().equals(ZFolder.ID_TRASH))
                addContact(contact);
        }
        mCleared = false;
    }

    private synchronized List<ZContact> galCache(String query, ZMailbox mailbox) throws ServiceException {
        int n = query.length();
        while (n > 0) {
            ZSearchGalResult result = mGalCache.get(query.substring(0, n));
            if (result != null && (n == query.length() || !result.getHasMore())) {
                return result.getContacts();
            }
            n--;
        }
        ZSearchGalResult galResult = mailbox.autoCompleteGal(query, GalEntryType.account, 20);
        mGalCache.put(query, galResult);
        return galResult.getContacts();
    }

    public synchronized List<ZContact> autoComplete(String query, int limit, ZMailbox mailbox) throws ServiceException {
        if (mCleared)
            init(mailbox);
        SortedMap<String, List<ZContact>> hits = mCache.tailMap(query);
        List<ZContact> result = new ArrayList<ZContact>(hits.size());
        Set<String> ids = new HashSet<String>();
        for ( Entry<String, List<ZContact>> entry : hits.entrySet()) {
            if (entry.getKey().startsWith(query)) {
                for (ZContact c : entry.getValue()) {
                    if (!ids.contains(c.getId())) {
                        result.add(c);
                        ids.add(c.getId());
                    }
                }
            } else {
                break;
            }
            if (limit > 0 && result.size() >= limit) break;
        }
        if (result.size() < limit && mailbox.getFeatures().getGalAutoComplete()) {
            List<ZContact> galContacts = galCache(query, mailbox);
            int needed = limit > 0 ? result.size() - limit : 0;
            for (ZContact contact : galContacts) {
                result.add(contact);
                if (needed > 0 && result.size() >= needed) break;
            }
        }
        return result;
    }

    /**
     *
     * @param refreshEvent the refresh event
     * @param mailbox the mailbox that had the event
     */
    public void handleRefresh(ZRefreshEvent refreshEvent, ZMailbox mailbox) throws ServiceException {
        clear();
    }

    /**
     *
     * @param event the create event
     * @param mailbox the mailbox that had the event
     */
    public void handleCreate(ZCreateEvent event, ZMailbox mailbox) throws ServiceException {
        if (event instanceof ZCreateContactEvent) {
            //ZContactEvent cev = (ZContactEvent) event;
            //TODO: addContact(...);
            clear();
        }
    }

    /**
     * @param event the modify event
     * @param mailbox the mailbox that had the event
     */
    public void handleModify(ZModifyEvent event, ZMailbox mailbox) throws ServiceException {
        if (event instanceof ZModifyContactEvent) {
            // TODO: delete existing by id, add new
            clear();
        }
    }

    /**
     *
     * default implementation is a no-op
     *
     * @param event the delete event
     * @param mailbox the mailbox that had the event
     */
    public synchronized void handleDelete(ZDeleteEvent event, ZMailbox mailbox) throws ServiceException {
        for (String id : event.toList()) {
            removeContact(id);
        }
    }
}
