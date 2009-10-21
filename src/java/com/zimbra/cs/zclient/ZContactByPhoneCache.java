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
import com.zimbra.cs.zclient.event.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ZContactByPhoneCache extends ZEventHandler {
	public static class ContactPhone {
		private ZContact mContact;
		private String mField;
		public ContactPhone(ZContact contact, String field) {
			mContact = contact;
			mField = field;
		}
		public ZContact getContact() { return mContact; }
		public String getField() { return mField; }
	}
	private static List<String> sATTRS = Arrays.asList(
			Contact.A_homePhone, Contact.A_homePhone2, Contact.A_mobilePhone,
			Contact.A_otherPhone, Contact.A_workPhone, Contact.A_workPhone2,
			Contact.A_homeFax, Contact.A_workFax, Contact.A_workMobile,
			Contact.A_workAltPhone, Contact.A_otherFax, Contact.A_assistantPhone,
			Contact.A_companyPhone);

	// Map of phone fields to contacts
    private Map<String, ContactPhone> mCache;

    /** true if we have been cleared and need to refetch contacts on next auto-compleete */
    private boolean mCleared;

	// id to contact map of all contacts in our cache
	private Map<String,ZContact> mContacts;

    public ZContactByPhoneCache() {
        mCache = new HashMap<String, ContactPhone>();
        mCleared = true;
		mContacts = new HashMap<String, ZContact>();
    }

    private void addPhoneKey(String field, ZContact contact) {
		String key = contact.getAttrs().get(field);
        if (key == null) {
			return;
		}

        key = ZPhone.getName(key);
        mCache.put(key, new ContactPhone(contact, field));
    }

	private void removeKey(String field, ZContact contact) {
		String key = contact.getAttrs().get(field);
		if (key == null) {
			return;
		}

		key = ZPhone.getName(key);
		mCache.remove(key);
	}

	public synchronized ContactPhone getByPhone(String phone, ZMailbox mailbox) throws ServiceException {
		if (mCleared)
			init(mailbox);
		return mCache.get(ZPhone.getName(phone));
	}

    private synchronized void addContact(ZContact contact) {
		mContacts.put(contact.getId(), contact);
		for (String attr : sATTRS) {
			addPhoneKey(attr, contact);
		}
    }

    private synchronized void removeContact(String id) {
        ZContact contact = mContacts.get(id);
        if (contact == null) return;
		mContacts.remove(id);

		for (String attr : sATTRS) {
			removeKey(attr, contact);
		}
    }

    public synchronized void clear() {
        mContacts.clear();
        mCleared = true;
		mCache.clear();
    }

    private synchronized void init(ZMailbox mailbox) throws ServiceException {
		List<ZContact> contacts = mailbox.getContacts(null, null, false, null);
        for (ZContact contact : contacts) {
            if (!contact.getFolderId().equals(ZFolder.ID_TRASH))
                addContact(contact);
        }
        mCleared = false;
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