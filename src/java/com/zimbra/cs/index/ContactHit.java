/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011 Zimbra, Inc.
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

package com.zimbra.cs.index;

import com.google.common.base.Objects;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.Contact;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailItem;

/**
 * @since Nov 8, 2004
 * @author tim
 */
public final class ContactHit extends ZimbraHit {
    private final int itemId;
    private Contact contact;

    public ContactHit(ZimbraQueryResultsImpl results, Mailbox mbx, int id, Contact contact, Object sortValue) {
        super(results, mbx, sortValue);
        itemId = id;
        this.contact = contact;
    }

    @Override
    public MailItem getMailItem() throws ServiceException {
        return getContact();
    }

    public Contact getContact() throws ServiceException {
        if (contact == null) {
            contact = getMailbox().getContactById(null, getItemId());
        }
        return contact;
    }

    @Override
    public int getConversationId() {
        return 0;
    }

    @Override
    public int getItemId() {
        return itemId;
    }

    @Override
    void setItem(MailItem item) {
        contact = (Contact) item;
    }

    @Override
    boolean itemIsLoaded() {
        return contact != null;
    }

    @Override
    public String getName() throws ServiceException {
        if (cachedName == null) {
            cachedName = getContact().getSortName();
        }
        return cachedName;
    }

    @Override
    public String toString() {
        try {
            return Objects.toStringHelper(this)
                .add("id", getItemId())
                .add("conv", getConversationId())
                .add("contact", getContact())
                .addValue(super.toString())
                .toString();
        } catch (ServiceException e) {
            return e.toString();
        }
    }

}
