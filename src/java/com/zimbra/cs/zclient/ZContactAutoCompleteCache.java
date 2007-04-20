package com.zimbra.cs.zclient;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.Contact;
import com.zimbra.cs.zclient.event.ZContactEvent;
import com.zimbra.cs.zclient.event.ZCreateEvent;
import com.zimbra.cs.zclient.event.ZDeleteEvent;
import com.zimbra.cs.zclient.event.ZEventHandler;
import com.zimbra.cs.zclient.event.ZModifyAppointmentEvent;
import com.zimbra.cs.zclient.event.ZModifyEvent;
import com.zimbra.cs.zclient.event.ZRefreshEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Map.Entry;

public class ZContactAutoCompleteCache extends ZEventHandler {

    private TreeMap<String, List<ZContact>> mCache;

    // map of all contacts in our cache
    private Map<String,ZContact> mContacts;

    /** true if we have been cleared and need to refetch contacts on next auto-compleete */
    private boolean mCleared;

    public ZContactAutoCompleteCache() {
        mCache = new TreeMap<String, List<ZContact>>();
        mContacts = new HashMap<String, ZContact>();
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
        addKey(attrs.get(Contact.A_firstName), contact);
        addKey(attrs.get(Contact.A_lastName), contact);
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
        Map<String,String> attrs = contact.getAttrs();
        removeKey(attrs.get(Contact.A_email), contact);
        removeKey(attrs.get(Contact.A_email2), contact);
        removeKey(attrs.get(Contact.A_email3), contact);
        removeKey(attrs.get(Contact.A_firstName), contact);
        removeKey(attrs.get(Contact.A_lastName), contact);
    }

    public synchronized void clear() {
        mCache.clear();
        mContacts.clear();
        mCleared = true;
    }

    private synchronized void init(ZMailbox mailbox) throws ServiceException {
        List<ZContact> contacts = mailbox.getContacts(null, null, false, Arrays.asList(Contact.A_email, Contact.A_email2, Contact.A_email3, Contact.A_firstName, Contact.A_lastName));
        for (ZContact contact : contacts)
            addContact(contact);
        mCleared = false;
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
        if (event instanceof ZContactEvent) {
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
        if (event instanceof ZModifyAppointmentEvent) {
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
