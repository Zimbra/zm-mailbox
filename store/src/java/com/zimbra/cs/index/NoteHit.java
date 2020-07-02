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
import com.zimbra.cs.mailbox.Note;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailItem;

/**
 * @since Nov 9, 2004
 * @author tim
 */
public final class NoteHit extends ZimbraHit {
    private Note note ;
    private final int itemId;

    NoteHit(ZimbraQueryResultsImpl results, Mailbox mbx, int id, Note note, Object sortValue) {
        super(results, mbx, sortValue);
        itemId = id;
        this.note = note;
    }

    @Override
    public MailItem getMailItem() throws ServiceException {
        return getNote();
    }

    public Note getNote() throws ServiceException {
        if (note == null) {
            note = getMailbox().getNoteById(null, getItemId());
        }
        return note;
    }

    @Override
    void setItem(MailItem item) {
        note = (Note) item;
    }

    @Override
    boolean itemIsLoaded() {
        return note != null;
    }

    @Override
    public String getName() throws ServiceException {
        if (cachedName == null) {
            cachedName = getNote().getSubject();
        }
        return cachedName;
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
    public String toString() {
        try {
            return MoreObjects.toStringHelper(this)
                .add("id", getItemId())
                .add("conv", getConversationId())
                .add("note", getNote())
                .addValue(super.toString())
                .toString();
        } catch (ServiceException e) {
            return e.toString();
        }
    }

    public int getHitType() {
        return 4;
    }

    public int doitVirt() {
        return 0;
    }

}
