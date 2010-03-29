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

/*
 * Created on Oct 15, 2004
 */
package com.zimbra.cs.index;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.Conversation;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailItem;


/**
 * @author tim
 * 
 * Indirect Conversation result. Efficient Read-access to a
 * com.zimbra.cs.mailbox.Conversation object returned from a query.
 * 
 * This class may have a real Conversation under it, or it might just have a
 * Lucene Document, or something else -- the accessor APIs in this class
 * will do the most efficient possible lookup and caching for read access to
 * the data.
 */
public final class ConversationHit extends ZimbraHit {
    
    private Conversation mConversation = null;
    private Map<Long, MessageHit> mMessageHits = new LinkedHashMap<Long, MessageHit>();
    private MessageHit mLastMessageHitAdded = null;
    private int mConversationId = 0;

    protected ConversationHit(ZimbraQueryResultsImpl results, Mailbox mbx, int conversationId, float score) {
        super(results, mbx, score);
        mConversationId = conversationId;
    }
    
    public int getConversationId() {
        return getId();
    }

    public void addMessageHit(MessageHit mh) {
        mLastMessageHitAdded = mh;
        mMessageHits.put(new Long(mh.getItemId()), mh);
    }

    public Collection<MessageHit> getMessageHits() {
        return mMessageHits.values();
    }

    public int getNumMessageHits() {
        return getMessageHits().size();
    }

    public int getNumMessages() throws ServiceException {
        return getConversation().getMessageCount();
    }

    public MessageHit getMessageHit(Long mailboxBlobId) {
        return mMessageHits.get(mailboxBlobId);
    }

    public MessageHit getFirstMessageHit() {
        Iterator iter = getMessageHits().iterator();
        return iter.hasNext() ? (MessageHit) iter.next() : null;
    }

    public int getItemId() {
        return mConversationId;
    }

    public byte getItemType() {
        return MailItem.TYPE_CONVERSATION;
    }
    
    void setItem(MailItem item) {
        mConversation = (Conversation)item;
    }
    
    boolean itemIsLoaded() {
        return mConversation != null;
    }

    
    public int getId() {
        return mConversationId;
    }

    public String toString() {
        return super.toString() + " C" + Long.toString(getId());
    }

    public String getSubject() throws ServiceException {
        if (mCachedSubj == null) {
            // the subject returned here must be the SORTING subject, which is the subject
            // of the most recent hit we found. (as we iterate through results in order, the most 
            // recently added message is the order we want to track for sorting purposes)  
            mCachedSubj = mLastMessageHitAdded.getSubject();
        } 
        return mCachedSubj;
    }

    public String getName() throws ServiceException {
        /*
        // FIXME: not sure what to return here -- maybe Name from first message hit?
        return "CONV_HAS_NO_NAME";
        */
        
        if (mCachedName == null) {
            MessageHit mh = getFirstMessageHit();
            mCachedName = mh == null ? "" : mh.getName();
        }
        return mCachedName;
    }

    public long getHitDate() throws ServiceException {
        MessageHit mh = getFirstMessageHit();
        return mh == null ? 0 : mh.getDate();
    }
    
    public long getSize() throws ServiceException {
        return getConversation().getSize();
    }
    

    public long getDate() throws ServiceException {
        if (mCachedDate == -1) {
			// always use the hit date when sorting, otherwise confusion happens (since we are
			// building the conv hit by aggregating MessageHits....suddenly switching to a 
			// very different sort-field can cause sort order to be unstable.
			//
			mCachedDate = getHitDate();
			if (mCachedDate == 0) {
				mCachedDate = getConversation().getDate();
			}
        }
        return mCachedDate;
    }

    // ..... etc ......

        public MailItem getMailItem() throws ServiceException { return getConversation(); }
        
    /**
     * Returns the real com.zimbra.cs.mailbox.Conversation object. Only use this if you
     * need write access to the Conversation.
     * 
     * Depending on the type of query that was executed, this may or may not
     * result in a DB access
     * 
     * @return real Conversation object
     * @throws ServiceException
     */
    public Conversation getConversation() throws ServiceException {
        if (mConversation == null) {
            mConversation = getMailbox().getConversationById(null, mConversationId);
        }
        return mConversation;
    }
}