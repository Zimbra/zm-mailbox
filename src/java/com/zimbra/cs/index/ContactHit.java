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
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Nov 8, 2004
 */
package com.zimbra.cs.index;

import org.apache.lucene.document.Document;

import com.zimbra.cs.mailbox.Contact;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.service.ServiceException;

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
    
    public int getSize() throws ServiceException {
        Contact c = getContact();
        return (int) c.getSize();
    }
    
    /* (non-Javadoc)
     * @see com.zimbra.cs.index.ZimbraHit#getConversationId()
     */
    public int getConversationId() throws ServiceException {
        // TODO Auto-generated method stub
        return 0;
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.index.ZimbraHit#getMessageId()
     */
    public int getItemId() throws ServiceException {
        return mItemId;
    }
    public byte getItemType() throws ServiceException {
        return MailItem.TYPE_CONTACT;
    }
    void setItem(MailItem item) {
        mContact = (Contact)item;
    }
    
    boolean itemIsLoaded() {
        return mContact!=null;
    }
    
    public String getSubject() throws ServiceException
    {
        if (mCachedSubj == null) {
            mCachedSubj = getContact().getSubject();
        }
        return mCachedSubj;
    }
    
    public String getName() throws ServiceException 
    {
        if (mCachedName == null) {
            mCachedName = getContact().getFileAsString();
        }
        return mCachedName;
    }
    

    public String toString() {
        int convId = 0;
        try {
            convId = getConversationId();
        } catch (ServiceException e) {
            e.printStackTrace();
        }
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
