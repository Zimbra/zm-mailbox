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

import org.apache.lucene.document.Document;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailItem;


/**
 * @author tim
 * 
 * Inderect result object wrapped around Lucene Document.
 * 
 * You wouldn't think we'd need this -- but in fact there are situations
 * where it is useful (e.g. a query that ONLY uses MySQL and therefore has
 * the real Conversation and Message objects) because the Lucene Doc isn't
 * there.
 * 
 * Access to the real Lucene doc is perhaps not necessary here -- the ?few
 * writable APIs on the Lucene document are probably not useful to us.
 *  
 */
public final class MessagePartHit extends ZimbraHit {
    
    private Document mDoc = null;

    private MessageHit mMessage = null;

    int mMailItemId = 0;
    
    protected MessagePartHit(ZimbraQueryResultsImpl res, Mailbox mbx, int mailItemId, Document d, float score, MailItem.UnderlyingData ud) throws ServiceException {
        super(res, mbx, score);
        mMailItemId = mailItemId;
        mDoc = d;
        if (ud != null) {
            getMessageResult(ud);
        }
    }

    public long getDate() throws ServiceException {
        if (mCachedDate == -1) {
            mCachedDate = getMessageResult().getDate();
        }
        return mCachedDate;
    }
    
    public int getConversationId() throws ServiceException {
        return getMessageResult().getConversationId();
    }

    public String getSubject() throws ServiceException {
    	if (mCachedSubj == null) {
    	    mCachedSubj = getMessageResult().getSubject();
    	}
        return mCachedSubj; 
    }
    
    public String getName() throws ServiceException {
        if (mCachedName == null) {
            mCachedName = getMessageResult().getSender();
        }
        return mCachedName;
    }

    public int getItemId() {
    	return mMailItemId;
    }
    
    void setItem(MailItem item) throws ServiceException {
        MessageHit mh = getMessageResult();
        mh.setItem(item);
    }
    
    boolean itemIsLoaded() throws ServiceException {
        return getMessageResult().itemIsLoaded();
    }
    
    
    public byte getItemType() throws ServiceException {
        return MailItem.TYPE_MESSAGE;
    }
    

    public String toString() {
        int convId = 0;
        try {
            convId = getConversationId();
        } catch (ServiceException e) {
            e.printStackTrace();
        }
        int size = 0;
        size = getSize();
        
        return "MP: " + super.toString() + " C" +convId + " M" + this.getItemId() + " P" + Integer.toString(getItemId()) + "-" + getPartName()+" S="+size;
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

    public int getSize() {
        if (mDoc != null) {
            String sizeStr = mDoc.get(LuceneFields.L_SIZE);
            long sizeLong = ZimbraAnalyzer.SizeTokenFilter.DecodeSize(sizeStr);
            return (int)sizeLong;
        } else {
            assert(false);// should never have a parthit without a document
            return 0;
        }
    }

    ////////////////////////////////////////////////////
    //
    // Hierarchy access:
    //
    public MessageHit getMessageResult() throws ServiceException {
        return getMessageResult(null);
    }

    /**
     * @return Message that contains this document
     */
    public MessageHit getMessageResult(MailItem.UnderlyingData ud) throws ServiceException {
        if (mMessage == null) {
            mMessage = getResults().getMessageHit(getMailbox(), new Integer(getItemId()), mDoc, getScore(), ud);
            mMessage.addPart(this);
            mMessage.cacheImapMessage(mCachedImapMessage);
            mMessage.cacheModifiedSequence(mCachedModseq);
        }
        return mMessage;
    }
    
    public MailItem getMailItem() throws ServiceException { return getMessageResult().getMailItem(); }
    

}