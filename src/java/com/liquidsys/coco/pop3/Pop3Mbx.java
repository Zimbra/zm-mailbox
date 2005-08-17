/*
 * Created on Nov 26, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.liquidsys.coco.pop3;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.liquidsys.coco.index.LiquidHit;
import com.liquidsys.coco.index.LiquidQueryResults;
import com.liquidsys.coco.index.MailboxIndex;
import com.liquidsys.coco.index.MessageHit;
import com.liquidsys.coco.index.queryparser.ParseException;
import com.liquidsys.coco.mailbox.MailItem;
import com.liquidsys.coco.mailbox.Mailbox;
import com.liquidsys.coco.mailbox.Message;
import com.liquidsys.coco.service.ServiceException;


class Pop3Mbx {
    int mId; // id of the mailbox
    int mNumDeleted; // number of messages deleted
    long mTotalSize; // raw size from blob store
    long mDeletedSize; // raw size from blob store    
    Pop3Msg mMessages[]; // array of pop messages
    
    /**
     * initialize the Pop3Mbx, without keeping a reference to either the Mailbox object or
     * any of the Message objects in the inbox.
     * 
     * @param mailbox
     * @throws ServiceException
     */
    Pop3Mbx(Mailbox mailbox, String query) throws ServiceException 
    {
        mId = mailbox.getId();
        mNumDeleted = 0;
        mDeletedSize = 0;
        List items = null;
        
        if (query == null || query.equals("")) {
            items = mailbox.getItemList(MailItem.TYPE_MESSAGE, Mailbox.ID_FOLDER_INBOX);
        } else {
            LiquidQueryResults results;
            try {
                results = mailbox.search(query, new byte[] { MailItem.TYPE_MESSAGE }, MailboxIndex.SEARCH_ORDER_DATE_DESC);
                items = new ArrayList();
                while (results.hasNext()) {
                    LiquidHit hit = results.getNext();
                    if (hit instanceof MessageHit) {
                        MessageHit mh = (MessageHit) hit;
                        items.add(mh.getMessage());
                    }
                }
            } catch (IOException e) {
                throw ServiceException.FAILURE(e.getMessage(), e);
            } catch (ParseException e) {
                throw ServiceException.FAILURE(e.getMessage(), e);
            }
        }
        mMessages = new Pop3Msg[items.size()];
        for (int n=0; n < items.size(); n++) {
            Message m = (Message) items.get(n);
            mMessages[n] = new Pop3Msg(m);
            mTotalSize += mMessages[n].getSize();
        }
    }
    
    /**
     * 
     * @return the liquid mailbox id
     */
    int getId() {
        return mId;
    }
    
    /**
     * @return total size of all non-deleted messages
     */        
    long getSize() {
        return mTotalSize-mDeletedSize;
    }
    
    /**
     * @return number of undeleted messages
     */
    int getNumMessages() {
        return mMessages.length - mNumDeleted;
    }
    
    /**
     * @return total number of messages, including deleted.
     */
    int getTotalNumMessages() {
        return mMessages.length;
    }
    
    /**
     * @return number of deleted messages
     */
    int getNumDeletedMessages() {
        return mNumDeleted;
    }

    /**
     * get the message by position in the array, starting at 0, even if it was deleted.
     * 
     * @param index
     * @return
     * @throws Pop3CmdException
     */
    Pop3Msg getMsg(int index) throws Pop3CmdException {
        if (index < 0 || index >= mMessages.length)
            throw new Pop3CmdException("invalid message");
        Pop3Msg pm = mMessages[index];
        //if (pm.isDeleted()) 
        //    throw new Pop3CmdException("message is deleted");
        return mMessages[index];
    }
    
    private int parseInt(String s, String message) throws Pop3CmdException {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            throw new Pop3CmdException(message);
        }
    }

    /**
     * get the undeleted Pop3Msg for the specified msg number (external, starting at 1 index).
     * @param msg
     * @return
     * @throws Pop3CmdException
     * @throws ServiceException
     */
    Pop3Msg getPop3Msg(String msg) throws Pop3CmdException, ServiceException {
        int index = parseInt(msg, "unable to parse msg");
        Pop3Msg pm = getMsg(index-1);
        if (pm.isDeleted())
            throw new Pop3CmdException("message is deleted");
        return pm;
    }

    /**
     * get the undeleted Message for the specified msg number (external, starting at 1 index).
     * @param msg
     * @return
     * @throws Pop3CmdException
     * @throws ServiceException
     */
    Message getMessage(String msg) throws Pop3CmdException, ServiceException {
        Pop3Msg pm = getPop3Msg(msg);
        Mailbox mbox = Mailbox.getMailboxById(mId);
        return mbox.getMessageById(pm.getId());
    }
    
    /**
     * Mark the message as deleted and update counts and mailbox size.
     * @param pm
     */
    public void delete(Pop3Msg pm) {
        if (!pm.isDeleted()) {
            pm.mDeleted = true;
            mNumDeleted++;
            mDeletedSize += pm.getSize();
        }
    }

    /**
     * unmark all messages that were marked as deleted and return the count that were deleted.
     */
    public int undeleteMarked() {
        int count = 0;
        for (int i=0; i < mMessages.length; i++) {
            Pop3Msg pm = mMessages[i];
            if (pm.isDeleted()) {
                mNumDeleted--;
                mDeletedSize -= pm.getSize();
                pm.mDeleted = false;
                count++;
            }
        }
        return count;
    }
    
    /**
     * delete all messages marked as deleted and return number deleted.
     * throws a Pop3CmdException on partial deletes
     * @throws ServiceException
     * @throws Pop3CmdException
     */
    public int deleteMarked(boolean hard) throws ServiceException, Pop3CmdException {
        Mailbox mbox = Mailbox.getMailboxById(mId);
        int count = 0;
        int failed = 0;
        for (int i=0; i < mMessages.length; i++) {
            Pop3Msg pm = mMessages[i];
            if (pm.isDeleted()) {
                try {
                    if (hard) {
                        mbox.delete(null, pm.getId(), MailItem.TYPE_MESSAGE);                        
                    } else {
                        mbox.move(null, pm.getId(), MailItem.TYPE_MESSAGE, Mailbox.ID_FOLDER_TRASH);
                    }
                    count++;                    
                } catch (ServiceException e) {
                    failed++;
                }
                mNumDeleted--;
                mDeletedSize -= pm.getSize();
                pm.mDeleted = false;
            }
        }
        if (failed != 0) {
            throw new Pop3CmdException("deleted "+count+"/"+(count+failed)+" message(s)");
        }
        return count;
    }
}