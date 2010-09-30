/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Message;

/**
 * Inderect result object wrapped around Lucene {@link Document}.
 * <p>
 * You wouldn't think we'd need this -- but in fact there are situations
 * where it is useful (e.g. a query that ONLY uses MySQL and therefore has
 * the real Conversation and Message objects) because the Lucene
 * {@link Document} isn't there.
 *
 * Access to the real Lucene {@link Document} is perhaps not necessary here --
 * the few writable APIs on the Lucene {@link Document} are probably not useful
 * to us.
 *
 * @since Oct 15, 2004
 * @author tim
 */
public final class MessagePartHit extends ZimbraHit {

    private Document mDoc = null;
    private MessageHit mMessage = null;
    private int mMailItemId = 0;

    protected MessagePartHit(ZimbraQueryResultsImpl res, Mailbox mbx,
            int mailItemId, Document doc, float score, Message message) {
        super(res, mbx, score);
        mMailItemId = mailItemId;
        mDoc = doc;
        if (message != null) {
            getMessageResult(message);
        }
    }

    @Override
    public long getDate() throws ServiceException {
        if (mCachedDate == -1) {
            mCachedDate = getMessageResult().getDate();
        }
        return mCachedDate;
    }

    @Override
    public int getConversationId() throws ServiceException {
        return getMessageResult().getConversationId();
    }

    @Override
    public String getSubject() throws ServiceException {
        if (mCachedSubj == null) {
            mCachedSubj = getMessageResult().getSubject();
        }
        return mCachedSubj;
    }

    @Override
    public String getName() throws ServiceException {
        if (mCachedName == null) {
            mCachedName = getMessageResult().getSender();
        }
        return mCachedName;
    }

    @Override
    public int getItemId() {
        return mMailItemId;
    }

    @Override
    void setItem(MailItem item) throws ServiceException {
        getMessageResult().setItem(item);
    }

    @Override
    boolean itemIsLoaded() throws ServiceException {
        return getMessageResult().itemIsLoaded();
    }

    public byte getItemType() {
        return MailItem.TYPE_MESSAGE;
    }

    @Override
    public String toString() {
        int convId = 0;
        try {
            convId = getConversationId();
        } catch (ServiceException e) {
            e.printStackTrace();
        }

        long size = getSize();
        return "MP: " + super.toString() +
            " C" + convId +
            " M" + getItemId() +
            " P" + Integer.toString(getItemId()) + "-" + getPartName() +
            " S=" + size;
    }

    public String getFilename() {
        if (mDoc != null) {
            return mDoc.get(LuceneFields.L_FILENAME);
        } else {
            return null;
        }
    }

    public String getType() {
        if (mDoc != null) {
            return mDoc.get(LuceneFields.L_MIMETYPE);
        } else {
            return null;
        }
    }

    public String getPartName() {
        if (mDoc != null) {
            String retVal = mDoc.get(LuceneFields.L_PARTNAME);
            if (!retVal.equals(LuceneFields.L_PARTNAME_TOP)) {
                return retVal;
            }
        }
        return "";
    }

    @Override
    public long getSize() {
        if (mDoc != null) {
            return Long.parseLong(mDoc.get(LuceneFields.L_SORT_SIZE));
        } else {
            assert(false);// should never have a parthit without a document
            return 0;
        }
    }

    public MessageHit getMessageResult() {
        return getMessageResult(null);
    }

    /**
     * @return Message that contains this document
     */
    public MessageHit getMessageResult(Message message) {
        if (mMessage == null) {
            mMessage = getResults().getMessageHit(getMailbox(),
                    getItemId(), mDoc, getScore(), message);
            mMessage.addPart(this);
            mMessage.cacheImapMessage(mCachedImapMessage);
            mMessage.cacheModifiedSequence(mCachedModseq);
            mMessage.cacheParentId(mCachedParentId);
        }
        return mMessage;
    }

    @Override
    public MailItem getMailItem() throws ServiceException {
        return getMessageResult().getMailItem();
    }

}
