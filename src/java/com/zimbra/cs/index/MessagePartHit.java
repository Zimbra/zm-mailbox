/*
 * Created on Oct 15, 2004
 */
package com.zimbra.cs.index;

import org.apache.lucene.document.Document;

import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.service.ServiceException;


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

    int mMailboxBlobId = -1;
    
    protected MessagePartHit(ZimbraQueryResultsImpl res, Mailbox mbx, Document d, float score) {
        super(res, mbx, score);
        mDoc = d;
    }

    public long getDate() throws ServiceException {
        if (mCachedDate == -1) {
            mCachedDate = getMessageResult().getDate();
        }
        return mCachedDate;
    }
    
    /* (non-Javadoc)
     * @see com.zimbra.cs.index.ZimbraHit#inTrashOrSpam()
     */
    boolean inMailbox() throws ServiceException {
        return getMessageResult().inMailbox();
    }
    boolean inTrash() throws ServiceException {
        return getMessageResult().inTrash();
    }
    boolean inSpam() throws ServiceException {
        return getMessageResult().inSpam();
    }
    

    public int getConversationId() throws ServiceException {
        return getMessageResult().getConversationId();
    }

    public String getSubject() throws ServiceException {
        if (mDoc != null) {
            mCachedSubj = mDoc.get(LuceneFields.L_SORT_SUBJECT);
        } else {
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
        if (mMailboxBlobId != -1) {
            return mMailboxBlobId;
        }
        String mbid = mDoc.get(LuceneFields.L_MAILBOX_BLOB_ID);
        try {
            if (mbid != null) {
                mMailboxBlobId = Integer.parseInt(mbid);
            }
            return mMailboxBlobId;
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return 0;
        }
    }
    
    void setItem(MailItem item) {
        MessageHit mh = getMessageResult();
        mh.setItem(item);
    }
    
    boolean itemIsLoaded() {
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

    /**
     * @return Message that contains this document
     */
    public MessageHit getMessageResult() {
        if (mMessage == null) {
            mMessage = 
                getResults().getMessageHit(getMailbox(), new Integer(getItemId()), mDoc, getScore());
            mMessage.addPart(this);
        }
        return mMessage;
    }

}