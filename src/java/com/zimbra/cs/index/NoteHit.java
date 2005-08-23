/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.1
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.  Portions
 * created by Zimbra are Copyright (C) 2005 Zimbra, Inc.  All Rights
 * Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Nov 9, 2004
 */
package com.zimbra.cs.index;

import org.apache.lucene.document.Document;

import com.zimbra.cs.mailbox.Note;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.service.ServiceException;

/**
 * @author tim
 */
public final class NoteHit extends ZimbraHit {
    
    public NoteHit(ZimbraQueryResultsImpl results, Mailbox mbx, Document d, float score) {
        super(results, mbx, score);
        
        mDocument = d;
    }
    
    private Document mDocument = null;
    private Note mNote = null;
    
    /* (non-Javadoc)
     * @see com.zimbra.cs.index.ZimbraHit#getDate()
     */
    public long getDate() throws ServiceException {
        if (mCachedDate == -1) {
            mCachedDate = getNote().getDate();
        } 
        return mCachedDate;
    }
    
    public Note getNote() throws ServiceException {
        if (mNote== null) {
            mNote= getMailbox().getNoteById(getItemId());
        }
        return mNote;
    }
    
    void setItem(MailItem item) {
        mNote = (Note)item;
    }
    
    boolean itemIsLoaded() {
        return mNote != null;
    }
    
    
    /* (non-Javadoc)
     * @see com.zimbra.cs.index.ZimbraHit#inTrashOrSpam()
     */
    boolean inMailbox() throws ServiceException {
        return getNote().inMailbox();
    }
    boolean inTrash() throws ServiceException {
        return getNote().inTrash();
    }
    boolean inSpam() throws ServiceException {
        return getNote().inSpam();
    }
    
    
    public String getSubject() throws ServiceException
    {
        if (mCachedSubj == null) {
            mCachedSubj = getNote().getSubject();
        }
        return mCachedSubj;
    }
    
    public String getName() throws ServiceException {
        if (mCachedName == null) {
            mCachedName = getNote().getSubject();
        }
        return mCachedName;
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
        String mbid = mDocument.get(LuceneFields.L_MAILBOX_BLOB_ID);
        return Integer.parseInt(mbid);
    }
    
    public byte getItemType() throws ServiceException {
        return MailItem.TYPE_NOTE;
    }
    
    public int getSize() throws ServiceException {
        Note n = this.getNote();
        return (int) n.getSize();
    }
    

    public String toString() {
        int convId = 0;
        try {
            convId = getConversationId();
        } catch (ServiceException e) {
            e.printStackTrace();
        }
        String msgStr = "";
        String noteStr = "";
        try {
            msgStr = Integer.toString(getItemId());
            noteStr = getNote().toString();
        } catch(Exception e) {
            e.printStackTrace();
        }
        return "NT: " + super.toString() + " C" + convId + " M" + msgStr + " " + noteStr;
    }


    /* (non-Javadoc)
     * @see com.zimbra.cs.index.ZimbraHit#getHitType()
     */
    public int getHitType() {
        // TODO Auto-generated method stub
        return 4;
    }
    public int doitVirt() {
        return 0;
    }    
    
}
