/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2009, 2010 Zimbra, Inc.
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
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;

public class DocumentHit extends ZimbraHit {

    protected com.zimbra.cs.mailbox.Document mDocument;
    protected int mMessageId;
    protected Document mDoc;

    protected DocumentHit(ZimbraQueryResultsImpl results, Mailbox mbx,
            float score, int mailItemId, MailItem.UnderlyingData underlyingData,
            Document d) throws ServiceException {
        this(results, mbx, score, mailItemId, underlyingData);
        mDoc = d;
    }

    protected DocumentHit(ZimbraQueryResultsImpl results, Mailbox mbx,
            float score, int mailItemId, MailItem.UnderlyingData underlyingData)
        throws ServiceException {

        this(results, mbx, score);
        mMessageId = mailItemId;
        if (underlyingData != null) {
            MailItem item = mbx.toItem(underlyingData);
            assert(item instanceof com.zimbra.cs.mailbox.Document);
            mDocument = (com.zimbra.cs.mailbox.Document) item;
        }
    }

    protected DocumentHit(ZimbraQueryResultsImpl results, Mailbox mbx, float score) {
        super(results, mbx, score);
    }

    @Override
    public long getDate() {
        return mDocument.getDate();
    }

    @Override
    public long getSize() {
        return mDocument.getSize();
    }

    @Override
    public int getConversationId() {
        return 0;
    }

    @Override
    public int getItemId() {
        return mMessageId;
    }

    public byte getItemType() {
        return mDocument.getType();
    }

    @Override
    void setItem(MailItem item) {
        if (item instanceof com.zimbra.cs.mailbox.Document)
            mDocument = (com.zimbra.cs.mailbox.Document) item;
    }

    @Override
    boolean itemIsLoaded() {
        return mDocument != null;
    }

    @Override
    public String getSubject() {
        return mDocument.getName();
    }

    @Override
    public String getName() {
        return mDocument.getName();
    }

    @Override
    public MailItem getMailItem() {
        return getDocument();
    }

    public com.zimbra.cs.mailbox.Document getDocument() {
        return mDocument;
    }

    public int getVersion() {
        if (mDoc != null) {
            String verStr = mDoc.get(LuceneFields.L_VERSION);
            if (verStr != null) {
                return Integer.parseInt(verStr);
            }
        }
        // if there is no lucene Document, only the db search was done.
        // then just match the latest version.
        return mDocument.getVersion();
    }
}
