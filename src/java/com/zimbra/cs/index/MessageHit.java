/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007 Zimbra, Inc.
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
 * Created on Oct 15, 2004
 */
package com.zimbra.cs.index;

import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.document.DateField;
import org.apache.lucene.document.Document;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.Tag;
import com.zimbra.cs.mime.ParsedAddress;

import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;



/**
 * @author tim
 * 
 * Efficient Read-access to a Message returned from a query. APIs mirror the
 * APIs on com.zimbra.cs.mailbox.Message, but are read-only. The real
 * archive.mailbox.Message can be retrieved, but this should only be done if
 * write-access is necessary.
 */
public class MessageHit extends ZimbraHit {

    private static Log mLog = LogFactory.getLog(MessageHit.class);

    private Document mDoc = null;
    private Message mMessage = null;
    private List<MessagePartHit> mMatchedParts = null;
    private int mConversationId = 0;
    private int mMessageId = 0;
    private ConversationHit mConversationHit = null;

    protected MessageHit(ZimbraQueryResultsImpl results, Mailbox mbx, int mailItemId, Document d, float score, MailItem.UnderlyingData underlyingData) throws ServiceException {
        super(results, mbx, score);
        mDoc = d;
        assert (d != null);
        mMessageId = mailItemId;
        assert (mailItemId != 0);
        if (underlyingData != null) {
            mMessage = (Message)mbx.getItemFromUnderlyingData(underlyingData);
        }
    }

    protected MessageHit(ZimbraQueryResultsImpl results, Mailbox mbx, int mailItemId, float score, MailItem.UnderlyingData underlyingData) throws ServiceException {
        super(results, mbx, score);
        mMessageId = mailItemId;
        assert (mailItemId != 0);
        if (underlyingData != null) {
            mMessage = (Message)mbx.getItemFromUnderlyingData(underlyingData);
        }
    }
    
    int getFolderId() throws ServiceException {
        return getMessage().getFolderId();
    }
    
    public int getConversationId() throws ServiceException {
        if (mConversationId == 0) {
            mConversationId = getMessage().getConversationId();
        }
        return mConversationId;
    }

    public long getDate() throws ServiceException {
        if (mCachedDate == -1) {
            if (mMessage == null && mDoc != null) {
                String dateStr = mDoc.get(LuceneFields.L_SORT_DATE);
                if (dateStr != null) {
                    mCachedDate = DateField.stringToTime(dateStr);
                    
                    return mCachedDate;
                }
            }
            mCachedDate = getMessage().getDate();
        }
        return mCachedDate;
    }

    public void addPart(MessagePartHit part) {
        if (mMatchedParts == null)
            mMatchedParts = new ArrayList<MessagePartHit>();
        
        if (!mMatchedParts.contains(part)) {
            mMatchedParts.add(part);
        }
    }

    public List<MessagePartHit> getMatchedMimePartNames() {
        return mMatchedParts;
    }

    public int getItemId() {
    	return mMessageId;
    }
    
    public byte getItemType() {
        return MailItem.TYPE_MESSAGE;
    }
    

    public String toString() {
        int convId = 0;
        try {
            convId = getConversationId();
        } catch (ServiceException e) {
            e.printStackTrace();
        }
        long size = 0;
        try {
            size = getSize();
        } catch (ServiceException e) {
            e.printStackTrace();
        }
        return "MS: " + super.toString() + " C" + convId + " M" + Integer.toString(getItemId()) + " S="+size;
    }

    public long getSize() throws ServiceException {
//        if (mDoc != null) {
        if (false) {
            String sizeStr = mDoc.get(LuceneFields.L_SIZE);
            return (int) ZimbraAnalyzer.SizeTokenFilter.DecodeSize(sizeStr);
        } else {
            return getMessage().getSize();
        }
    }

    public boolean isTagged(Tag tag) throws ServiceException {
        return getMessage().isTagged(tag);
    }
    
    void setItem(MailItem item) {
        mMessage = (Message) item;
    }
    
    boolean itemIsLoaded() {
        return mMessage != null;
    }
    
    public MailItem getMailItem() throws ServiceException { return  getMessage(); }
    
    public Message getMessage() throws ServiceException {
        if (mMessage == null) {
            Mailbox mbox = MailboxManager.getInstance().getMailboxById(getMailbox().getId());
            int messageId = getItemId();
            try {
                mMessage = mbox.getMessageById(null, messageId);
            } catch (ServiceException e) {
                mLog.error("Error getting message id="+messageId+" from mailbox "+mbox.getId(),e);
                e.printStackTrace();
                throw e;
            }
        }
        return mMessage;
    }

    public String getSubject() throws ServiceException {
        if (mCachedSubj == null) {
            if (mConversationHit != null) { 
                mCachedSubj = getConversationResult().getSubject();
            } else {
                mCachedSubj = getMessage().getNormalizedSubject();
            }
        }
        return mCachedSubj;
    }
    
    public String getName() throws ServiceException {
        if (mCachedName == null) {
            mCachedName = getSender();
        }
        return mCachedName;
    }

    public long getDateHeader() throws ServiceException {
        if (mMessage == null && mDoc != null) {
            String dateStr = mDoc.get(LuceneFields.L_SORT_DATE);
            if (dateStr != null) {
                return DateField.stringToTime(dateStr);
            } else {
                return 0;
            }
        }
        return getMessage().getDate();
    }

    public String getSender() throws ServiceException {
        return new ParsedAddress(getMessage().getSender()).getSortString();
    }

    ////////////////////////////////////////////////////
    //
    // Hierarchy access:
    //

    /**
     * @return a ConversationResult corresponding to this message's
     *         conversation
     * @throws ServiceException
     */
    public ConversationHit getConversationResult() throws ServiceException {
        if (mConversationHit == null) {
            Integer cid = new Integer(getConversationId());
            mConversationHit = getResults().getConversationHit(getMailbox(), cid, getScore());
            mConversationHit.addMessageHit(this);
        }
        return mConversationHit;
    }
}