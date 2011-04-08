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
            return Objects.toStringHelper(this)
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
