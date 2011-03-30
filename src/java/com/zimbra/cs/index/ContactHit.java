/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2009, 2010, 2011 Zimbra, Inc.
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
import com.google.common.base.Strings;
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

    public ContactHit(ZimbraQueryResultsImpl results, Mailbox mbx, int itemId, Contact contact) {
        super(results, mbx);
        this.itemId = itemId;
        this.contact = contact;
    }

    @Override
    public long getDate() throws ServiceException {
        if (cachedDate == -1) {
            cachedDate = getContact().getDate();
        }
        return cachedDate;
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
    public long getSize() throws ServiceException {
        return getContact().getSize();
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
    public String getSubject() throws ServiceException {
        if (cachedSubj == null) {
            cachedSubj = getContact().getSubject();
        }
        return cachedSubj;
    }

    @Override
    public String getName() throws ServiceException {
        if (cachedName == null) {
            cachedName = getContact().getSortName();
        }
        return cachedName;
    }

    @Override
    public String getRecipients() throws ServiceException {
        return Strings.nullToEmpty(getContact().getSortRecipients());
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
