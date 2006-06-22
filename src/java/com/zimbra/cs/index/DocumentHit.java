/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.index;

import org.apache.lucene.document.Document;

import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.service.ServiceException;

public class DocumentHit extends ZimbraHit {

    protected com.zimbra.cs.mailbox.Document mDocument;
    protected int mMessageId;
    protected Document mDoc;

    protected DocumentHit(ZimbraQueryResultsImpl results, Mailbox mbx, float score, int mailItemId, MailItem.UnderlyingData underlyingData, Document d) throws ServiceException {
        this(results, mbx, score, mailItemId, underlyingData);
        mDoc = d;
    }

    protected DocumentHit(ZimbraQueryResultsImpl results, Mailbox mbx, float score, int mailItemId, MailItem.UnderlyingData underlyingData) throws ServiceException {
        this(results, mbx, score);
        mMessageId = mailItemId;
        if (underlyingData != null) {
            MailItem item = mbx.getItemFromUnderlyingData(underlyingData);
            assert(item instanceof com.zimbra.cs.mailbox.Document);
            mDocument = (com.zimbra.cs.mailbox.Document) item;
        }
    }

    protected DocumentHit(ZimbraQueryResultsImpl results, Mailbox mbx, float score) {
        super(results, mbx, score);
    }

    public long getDate() throws ServiceException {
        return mDocument.getDate();
    }

    public int getSize() throws ServiceException {
        return (int)mDocument.getSize();
    }

    public int getConversationId() throws ServiceException {
        return 0;
    }

    public int getItemId() throws ServiceException {
        return mMessageId;
    }

    public byte getItemType() throws ServiceException {
        return mDocument.getType();
    }

    void setItem(MailItem item) throws ServiceException {
        if (item instanceof com.zimbra.cs.mailbox.Document)
            mDocument = (com.zimbra.cs.mailbox.Document) item;
    }

    boolean itemIsLoaded() throws ServiceException {
        return mDocument != null;
    }

    public String getSubject() throws ServiceException {
        return mDocument.getSubject();
    }

    public String getName() throws ServiceException {
        return getSubject();
    }

    public MailItem getMailItem() throws ServiceException { return getDocument(); }

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
