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

import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;

public final class DocumentHit extends ZimbraHit {

    private int mMessageId;
    private Document mLuceneDoc;
    private com.zimbra.cs.mailbox.Document mDocItem;

    DocumentHit(ZimbraQueryResultsImpl results, Mailbox mbx, float score,
            int mailItemId, Document luceneDoc,
            com.zimbra.cs.mailbox.Document docItem) {
        super(results, mbx, score);
        mMessageId = mailItemId;
        mLuceneDoc = luceneDoc;
        mDocItem = docItem;
    }

    @Override
    public long getDate() {
        return mDocItem.getDate();
    }

    @Override
    public long getSize() {
        return mDocItem.getSize();
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
        return mDocItem.getType();
    }

    @Override
    void setItem(MailItem item) {
        if (item instanceof com.zimbra.cs.mailbox.Document) {
            mDocItem = (com.zimbra.cs.mailbox.Document) item;
        }
    }

    @Override
    boolean itemIsLoaded() {
        return mDocItem != null;
    }

    @Override
    public String getSubject() {
        return mDocItem.getName();
    }

    @Override
    public String getName() {
        return mDocItem.getName();
    }

    @Override
    public MailItem getMailItem() {
        return getDocument();
    }

    public com.zimbra.cs.mailbox.Document getDocument() {
        return mDocItem;
    }

    public int getVersion() {
        if (mDocItem != null) {
            String verStr = mLuceneDoc.get(LuceneFields.L_VERSION);
            if (verStr != null) {
                return Integer.parseInt(verStr);
            }
        }
        // if there is no lucene Document, only the db search was done.
        // then just match the latest version.
        return mDocItem.getVersion();
    }
}
