/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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
 * Created on Nov 26, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.zimbra.cs.pop3;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.DateUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.index.SortBy;
import com.zimbra.cs.index.ZimbraHit;
import com.zimbra.cs.index.ZimbraQueryResults;
import com.zimbra.cs.index.MessageHit;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.OperationContext;

class Pop3Mailbox {
    private long mId; // id of the mailbox
    private int mNumDeleted; // number of messages deleted
    private long mTotalSize; // raw size from blob store
    private long mDeletedSize; // raw size from blob store
    private List<Pop3Message> mMessages; // array of pop messages
    private OperationContext mOpContext;

    private static final byte[] POP3_TYPES = new byte[] { MailItem.TYPE_MESSAGE };

    /**
     * initialize the Pop3Mailbox, without keeping a reference to either the Mailbox object or
     * any of the Message objects in the inbox.
     * @param mbox
     * @param acct TODO
     *
     * @throws ServiceException
     */
    Pop3Mailbox(Mailbox mbox, Account acct, String query) throws ServiceException {
        mId = mbox.getId();
        mNumDeleted = 0;
        mDeletedSize = 0;
        mOpContext = new OperationContext(acct);

        if (query == null || query.equals("")) {
            String dateConstraint = acct.getAttr(Provisioning.A_zimbraPrefPop3DownloadSince);
            Date popSince = dateConstraint == null ? null : DateUtil.parseGeneralizedTime(dateConstraint);

            mMessages = mbox.openPop3Folder(mOpContext, Mailbox.ID_FOLDER_INBOX, popSince);
            for (Pop3Message p3m : mMessages)
                mTotalSize += p3m.getSize();
        } else {
            ZimbraQueryResults results = null;
            mMessages = new ArrayList<Pop3Message>(500);
            try {
                results = mbox.search(mOpContext, query, POP3_TYPES, SortBy.DATE_DESCENDING, 500);

                while (results.hasNext()) {
                    ZimbraHit hit = results.getNext();
                    if (hit instanceof MessageHit) {
                        MessageHit mh = (MessageHit) hit;
                        Message msg = mh.getMessage();
                        mTotalSize += msg.getSize();
                        mMessages.add(new Pop3Message(msg));
                    }
                }
            } catch (IOException e) {
                throw ServiceException.FAILURE(e.getMessage(), e);
            } finally {
                if (results != null)
                    results.doneWithSearchResults();
            }
        }
    }

    /**
     *
     * @return the zimbra mailbox id
     */
    long getId() {
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
        return mMessages.size() - mNumDeleted;
    }

    /**
     * @return total number of messages, including deleted.
     */
    int getTotalNumMessages() {
        return mMessages.size();
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
    Pop3Message getMsg(int index) throws Pop3CmdException {
        if (index < 0 || index >= mMessages.size())
            throw new Pop3CmdException("invalid message");
        Pop3Message p3m = mMessages.get(index);
        //if (p3m.isDeleted())
        //    throw new Pop3CmdException("message is deleted");
        return p3m;
    }

    private int parseInt(String s, String message) throws Pop3CmdException {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            throw new Pop3CmdException(message);
        }
    }

    /**
     * get the undeleted Pop3Message for the specified msg number (external, starting at 1 index).
     * @param msg
     * @return
     * @throws Pop3CmdException
     * @throws ServiceException
     */
    Pop3Message getPop3Msg(String msg) throws Pop3CmdException {
        int index = parseInt(msg, "unable to parse msg");
        Pop3Message p3m = getMsg(index - 1);
        if (p3m.isDeleted())
            throw new Pop3CmdException("message is deleted");
        return p3m;
    }

    /**
     * get the undeleted Message for the specified msg number (external, starting at 1 index).
     * @param msg
     * @return
     * @throws Pop3CmdException
     * @throws ServiceException
     */
    Message getMessage(String msg) throws Pop3CmdException, ServiceException {
        Pop3Message p3m = getPop3Msg(msg);
        Mailbox mbox = MailboxManager.getInstance().getMailboxById(mId);
        return mbox.getMessageById(mOpContext, p3m.getId());
    }

    /**
     * Mark the message as deleted and update counts and mailbox size.
     * @param p3m
     */
    public void delete(Pop3Message p3m) {
        if (!p3m.isDeleted()) {
            p3m.mDeleted = true;
            mNumDeleted++;
            mDeletedSize += p3m.getSize();
        }
    }

    /**
     * unmark all messages that were marked as deleted and return the count that were deleted.
     */
    public int undeleteMarked() {
        int count = 0;
        for (int i = 0; i < mMessages.size(); i++) {
            Pop3Message p3m = mMessages.get(i);
            if (p3m.isDeleted()) {
                mNumDeleted--;
                mDeletedSize -= p3m.getSize();
                p3m.mDeleted = false;
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
        Mailbox mbox = MailboxManager.getInstance().getMailboxById(mId);
        int count = 0;
        int failed = 0;
        for (Pop3Message p3m : mMessages) {
            if (p3m.isDeleted()) {
                try {
                    if (hard) {
                        mbox.delete(mOpContext, p3m.getId(), MailItem.TYPE_MESSAGE);
                    } else {
                        mbox.move(mOpContext, p3m.getId(), MailItem.TYPE_MESSAGE, Mailbox.ID_FOLDER_TRASH);
                    }
                    count++;
                } catch (ServiceException e) {
                    failed++;
                }
                mNumDeleted--;
                mDeletedSize -= p3m.getSize();
                p3m.mDeleted = false;
            }
        }

        if (count > 0) {
            try {
                mbox.resetRecentMessageCount(mOpContext);
            } catch (ServiceException e) {
                ZimbraLog.pop.info("error resetting mailbox recent message count", e);
            }
        }
        if (failed > 0) {
            throw new Pop3CmdException("deleted "+count+"/"+(count+failed)+" message(s)");
        }
        return count;
    }
}
