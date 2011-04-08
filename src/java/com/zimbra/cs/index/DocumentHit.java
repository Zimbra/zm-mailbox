/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010, 2011 Zimbra, Inc.
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

    private final int itemId;
    private final Document luceneDoc;
    private com.zimbra.cs.mailbox.Document docItem;

    DocumentHit(ZimbraQueryResultsImpl results, Mailbox mbx, int id,
            com.zimbra.cs.mailbox.Document docItem, Document luceneDoc, Object sortKey) {
        super(results, mbx, sortKey);
        itemId = id;
        this.luceneDoc = luceneDoc;
        this.docItem = docItem;
    }

    @Override
    public int getConversationId() {
        return 0;
    }

    @Override
    public int getItemId() {
        return itemId;
    }

    public MailItem.Type getItemType() {
        return docItem.getType();
    }

    @Override
    void setItem(MailItem item) {
        if (item instanceof com.zimbra.cs.mailbox.Document) {
            docItem = (com.zimbra.cs.mailbox.Document) item;
        }
    }

    @Override
    boolean itemIsLoaded() {
        return docItem != null;
    }

    @Override
    public String getName() {
        return docItem.getName();
    }

    @Override
    public MailItem getMailItem() {
        return getDocument();
    }

    public com.zimbra.cs.mailbox.Document getDocument() {
        return docItem;
    }

    public int getVersion() {
        if (docItem != null) {
            String verStr = luceneDoc.get(LuceneFields.L_VERSION);
            if (verStr != null) {
                return Integer.parseInt(verStr);
            }
        }
        // if there is no lucene Document, only the db search was done. then just match the latest version.
        return docItem.getVersion();
    }
}
