/*
 * Created on Nov 8, 2004
 */
package com.liquidsys.coco.index;

import org.apache.lucene.document.Document;

import com.liquidsys.coco.mailbox.Contact;
import com.liquidsys.coco.mailbox.Mailbox;
import com.liquidsys.coco.mailbox.MailItem;
import com.liquidsys.coco.service.ServiceException;

/**
 * @author tim
 */
public final class ContactHit extends LiquidHit {
    
    public ContactHit(LiquidQueryResultsImpl results, Mailbox mbx, int itemId, Document d, float score) {
        super(results, mbx, score);
        
        mItemId = itemId;
    }
    
    private Contact mContact = null;
    private int mItemId;

    /* (non-Javadoc)
     * @see com.liquidsys.coco.index.LiquidHit#getDate()
     */
    public long getDate() throws ServiceException {
        if (mCachedDate == -1) {
            mCachedDate = getContact().getDate();
        }
        return mCachedDate;
    }
    
    public Contact getContact() throws ServiceException {
        if (mContact == null) {
            mContact = getMailbox().getContactById(getItemId());
        }
        return mContact;
    }
    
    /* (non-Javadoc)
     * @see com.liquidsys.coco.index.LiquidHit#inTrashOrSpam()
     */
    boolean inMailbox() throws ServiceException {
        return getContact().inMailbox();
    }
    boolean inTrash() throws ServiceException {
        return getContact().inTrash();
    }
    boolean inSpam() throws ServiceException {
        return getContact().inSpam();
    }
    
    
    public int getSize() throws ServiceException {
        Contact c = getContact();
        return (int) c.getSize();
    }
    
    /* (non-Javadoc)
     * @see com.liquidsys.coco.index.LiquidHit#getConversationId()
     */
    public int getConversationId() throws ServiceException {
        // TODO Auto-generated method stub
        return 0;
    }

    /* (non-Javadoc)
     * @see com.liquidsys.coco.index.LiquidHit#getMessageId()
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
