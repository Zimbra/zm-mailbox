/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Nov 8, 2004
 */
package com.zimbra.cs.index;

import org.apache.lucene.document.Document;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.Contact;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailItem;

/**
 * @author tim
 */
public final class ContactHit extends ZimbraHit {
    
    public ContactHit(ZimbraQueryResultsImpl results, Mailbox mbx, int itemId, Document d, float score, MailItem.UnderlyingData ud) throws ServiceException {
        super(results, mbx, score);
        
        mItemId = itemId;
        
        if (ud != null)
            mContact = (Contact)mbx.getItemFromUnderlyingData(ud);
    }
    
    private Contact mContact = null;
    private int mItemId;

    /* (non-Javadoc)
     * @see com.zimbra.cs.index.ZimbraHit#getDate()
     */
    public long getDate() throws ServiceException {
        if (mCachedDate == -1) {
            mCachedDate = getContact().getDate();
        }
        return mCachedDate;
    }

    public MailItem getMailItem() throws ServiceException { return getContact(); }
    
    public Contact getContact() throws ServiceException {
        if (mContact == null) {
            mContact = getMailbox().getContactById(null, getItemId());
        }
        return mContact;
    }
    
    public long getSize() throws ServiceException {
        return getContact().getSize();
    }
    
    /* (non-Javadoc)
     * @see com.zimbra.cs.index.ZimbraHit#getConversationId()
     */
    public int getConversationId() {
        // TODO Auto-generated method stub
        return 0;
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.index.ZimbraHit#getMessageId()
     */
    public int getItemId() {
        return mItemId;
    }

    public byte getItemType() {
        return MailItem.TYPE_CONTACT;
    }

    void setItem(MailItem item) {
        mContact = (Contact) item;
    }
    
    boolean itemIsLoaded() {
        return mContact != null;
    }
    
    public String getSubject() throws ServiceException {
        if (mCachedSubj == null) {
            mCachedSubj = getContact().getSubject();
        }
        return mCachedSubj;
    }
    
    public String getName() throws ServiceException {
        if (mCachedName == null) {
            mCachedName = getContact().getFileAsString();
        }
        return mCachedName;
    }
    

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
