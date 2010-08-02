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

import org.apache.lucene.document.Document;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.Contact;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailItem;

/**
 * @since Nov 8, 2004
 * @author tim
 */
public final class ContactHit extends ZimbraHit {
    private Contact mContact = null;
    private int mItemId;

    public ContactHit(ZimbraQueryResultsImpl results, Mailbox mbx, int itemId,
            Document d, float score, MailItem.UnderlyingData ud) throws ServiceException {
        super(results, mbx, score);

        mItemId = itemId;

        if (ud != null) {
            mContact = (Contact) mbx.getItemFromUnderlyingData(ud);
        }
    }

    @Override
    public long getDate() throws ServiceException {
        if (mCachedDate == -1) {
            mCachedDate = getContact().getDate();
        }
        return mCachedDate;
    }

    @Override
    public MailItem getMailItem() throws ServiceException {
        return getContact();
    }

    public Contact getContact() throws ServiceException {
        if (mContact == null) {
            mContact = getMailbox().getContactById(null, getItemId());
        }
        return mContact;
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
        return mItemId;
    }

    public byte getItemType() {
        return MailItem.TYPE_CONTACT;
    }

    @Override
    void setItem(MailItem item) {
        mContact = (Contact) item;
    }

    @Override
    boolean itemIsLoaded() {
        return mContact != null;
    }

    @Override
    public String getSubject() throws ServiceException {
        if (mCachedSubj == null) {
            mCachedSubj = getContact().getSubject();
        }
        return mCachedSubj;
    }

    @Override
    public String getName() throws ServiceException {
        if (mCachedName == null) {
            mCachedName = getContact().getFileAsString();
        }
        return mCachedName;
    }

    @Override
    public String toString() {
        int convId = getConversationId();
        String msgStr = "";
        String contactStr = "";
        try {
            msgStr = Integer.toString(getItemId());
            contactStr = getContact().toString();
        } catch(Exception e) {
            e.printStackTrace();
        }
        return "CT: " + super.toString() + " C" + convId + " M" + msgStr + " " + contactStr;
    }

}
