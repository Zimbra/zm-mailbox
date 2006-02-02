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
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
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
import com.zimbra.cs.mailbox.WikiItem;
import com.zimbra.cs.service.ServiceException;

public class WikiHit extends ZimbraHit {

	private WikiItem mWiki;
	private int mMessageId;
	private Document mDoc;
	
    protected WikiHit(ZimbraQueryResultsImpl results, Mailbox mbx, float score, int mailItemId, MailItem.UnderlyingData underlyingData, Document d) throws ServiceException {
        this(results, mbx, score, mailItemId, underlyingData);
        mDoc = d;
    }

    protected WikiHit(ZimbraQueryResultsImpl results, Mailbox mbx, float score, int mailItemId, MailItem.UnderlyingData underlyingData) throws ServiceException {
        this(results, mbx, score);
        mMessageId = mailItemId;
        if (underlyingData != null) {
            mWiki = (WikiItem)mbx.getItemFromUnderlyingData(underlyingData);
        }
    }
    
    WikiHit(ZimbraQueryResultsImpl results, Mailbox mbx, float score) {
    	super(results, mbx, score);
    }
    
    boolean inMailbox() throws ServiceException {
        return mWiki.inMailbox();
    }
    
    boolean inTrash() throws ServiceException {
        return mWiki.inTrash();
    }
    
    boolean inSpam() throws ServiceException {
        return mWiki.inSpam();
    }
    
    public long getDate() throws ServiceException {
    	return mWiki.getDate();
    }
    
    public int getSize() throws ServiceException {
    	return (int)mWiki.getSize();
    }
    
    public int getConversationId() throws ServiceException {
    	return 0;
    }
    
    public int getItemId() throws ServiceException {
    	return mMessageId;
    }
    
    public byte getItemType() throws ServiceException {
    	return MailItem.TYPE_WIKI;
    }
    
    void setItem(MailItem item) throws ServiceException {
    	mWiki = (WikiItem)item;
    }
    
    boolean itemIsLoaded() throws ServiceException {
    	return mWiki != null;
    }
    
    public String getSubject() throws ServiceException {
    	return mWiki.getSubject();
    }
    
    public String getName() throws ServiceException {
    	return getSubject();
    }
    
    public WikiItem getWiki() {
    	return mWiki;
    }
}
