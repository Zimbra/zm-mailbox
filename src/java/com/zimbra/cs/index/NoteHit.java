/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2009, 2010 Zimbra, Inc.
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

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.Note;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailItem;

/**
 * @since Nov 9, 2004
 * @author tim
 */
public final class NoteHit extends ZimbraHit {
    private Note mNote = null;
    private int mMailItemId;

    public NoteHit(ZimbraQueryResultsImpl results, Mailbox mbx, int mailItemId,
            float score, MailItem.UnderlyingData ud)  throws ServiceException {
        super(results, mbx, score);

        mMailItemId = mailItemId;

        if (ud != null) {
            mNote = (Note) mbx.toItem(ud);
        }
    }

    @Override
    public long getDate() throws ServiceException {
        if (mCachedDate == -1) {
            mCachedDate = getNote().getDate();
        }
        return mCachedDate;
    }

    @Override
    public MailItem getMailItem() throws ServiceException {
        return getNote();
    }

    public Note getNote() throws ServiceException {
        if (mNote == null) {
            mNote = getMailbox().getNoteById(null, getItemId());
        }
        return mNote;
    }

    @Override
    void setItem(MailItem item) {
        mNote = (Note) item;
    }

    @Override
    boolean itemIsLoaded() {
        return mNote != null;
    }

    @Override
    public String getSubject() throws ServiceException {
        if (mCachedSubj == null) {
            mCachedSubj = getNote().getSubject();
        }
        return mCachedSubj;
    }

    @Override
    public String getName() throws ServiceException {
        if (mCachedName == null) {
            mCachedName = getNote().getSubject();
        }
        return mCachedName;
    }

    @Override
    public int getConversationId() {
        return 0;
    }

    @Override
    public int getItemId() {
        return mMailItemId;
    }

    public byte getItemType() {
        return MailItem.TYPE_NOTE;
    }

    @Override
    public long getSize() throws ServiceException {
        return getNote().getSize();
    }

    @Override
    public String toString() {
        int convId = getConversationId();
        String msgStr = "";
        String noteStr = "";
        try {
            msgStr = Integer.toString(getItemId());
            noteStr = getNote().toString();
        } catch(Exception e) {
            e.printStackTrace();
        }
        return "NT: " + super.toString() + " C" + convId + " M" + msgStr + " " + noteStr;
    }

    public int getHitType() {
        return 4;
    }

    public int doitVirt() {
        return 0;
    }

}
