/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.index;

import com.google.common.base.MoreObjects;
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

    /**
     * Returns the sort value.
     *
     * @throws ServiceException failed to get the sort field
     */
    @Override
    public Object getSortField(SortBy sort) throws ServiceException {
        Object sortField = super.getSortField(sort);
        if ((sortField == null || "".equals(sortField.toString())) && (sort != null)) {
            switch (sort) {
                case NAME_ASC:
                case NAME_DESC:
                case NAME_LOCALIZED_ASC:
                case NAME_LOCALIZED_DESC:
                    sortField = getName();
            }
        }
        return sortField;
    }

    @Override
    public String toString() {
        try {
            return MoreObjects.toStringHelper(this)
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
