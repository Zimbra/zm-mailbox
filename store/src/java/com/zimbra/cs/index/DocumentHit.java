/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;

public final class DocumentHit extends ZimbraHit {

    private final int itemId;
    private final IndexDocument indexDoc;
    private com.zimbra.cs.mailbox.Document docItem;

    DocumentHit(ZimbraQueryResultsImpl results, Mailbox mbx, int id,
            com.zimbra.cs.mailbox.Document docItem, IndexDocument indexDoc, Object sortKey) {
        super(results, mbx, sortKey);
        this.itemId = id;
        this.indexDoc = indexDoc;
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

    public MailItem.Type getItemType() throws ServiceException {
        return getDocument().getType();
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
    public String getName() throws ServiceException {
        return getDocument().getName();
    }

    @Override
    public MailItem getMailItem() throws ServiceException {
        return getDocument();
    }

    public com.zimbra.cs.mailbox.Document getDocument() throws ServiceException {
        if (docItem == null) {
            docItem = getMailbox().getDocumentById(null, itemId);
        }
        return docItem;
    }

    public int getVersion() throws ServiceException {
        if (indexDoc != null) {
            String ver = indexDoc.get(LuceneFields.L_VERSION);
            if (ver != null) {
                return Integer.parseInt(ver);
            }
        }
        // if there is no lucene Document, only the db search was done, then just match the latest version.
        return getDocument().getVersion();
    }
}
