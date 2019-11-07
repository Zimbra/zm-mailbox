/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2013, 2014, 2015, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.pop3;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.index.MessageHit;
import com.zimbra.cs.index.SortBy;
import com.zimbra.cs.index.ZimbraHit;
import com.zimbra.cs.index.ZimbraQueryResults;
import com.zimbra.cs.ldap.LdapDateUtil;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.OperationContext;

/**
 * @since Nov 26, 2004
 */
final class Pop3Mailbox {
    private static final Set<MailItem.Type> POP3_TYPES = EnumSet.of(MailItem.Type.MESSAGE);

    private final int id; // id of the mailbox
    private int numDeleted; // number of messages deleted
    private long totalSize; // raw size from blob store
    private long deletedSize = 0; // raw size from blob store
    private final List<Pop3Message> messages; // array of pop messages
    private final OperationContext opContext;
    private final Provisioning.PrefPop3DeleteOption deleteOption;

    /**
     * initialize the Pop3Mailbox, without keeping a reference to either the Mailbox object or
     * any of the Message objects in the inbox.
     */
    Pop3Mailbox(Mailbox mbox, Account acct, String query) throws ServiceException {
        id = mbox.getId();
        numDeleted = 0;
        opContext = new OperationContext(acct);
        deleteOption = acct.getPrefPop3DeleteOption();

        if (Strings.isNullOrEmpty(query)) {
            Set<Integer> folderIds = acct.isPrefPop3IncludeSpam() ?
                    ImmutableSet.of(Mailbox.ID_FOLDER_INBOX, Mailbox.ID_FOLDER_SPAM) :
                        Collections.singleton(Mailbox.ID_FOLDER_INBOX);
            String dateConstraint = acct.getAttr(Provisioning.A_zimbraPrefPop3DownloadSince);
            Date popSince = dateConstraint == null ? null : LdapDateUtil.parseGeneralizedTime(dateConstraint);
            messages = mbox.openPop3Folder(opContext, folderIds, popSince);
            for (Pop3Message p3m : messages) {
                totalSize += p3m.getSize();
            }
        } else {
            messages = new ArrayList<Pop3Message>(500);
            try (ZimbraQueryResults results = mbox.index.search(opContext, query, POP3_TYPES,
                SortBy.DATE_DESC, 500)) {
                while (results.hasNext()) {
                    ZimbraHit hit = results.getNext();
                    if (hit instanceof MessageHit) {
                        MessageHit mh = (MessageHit) hit;
                        Message msg = mh.getMessage();
                        if (!msg.isTagged(Flag.FlagInfo.POPPED)) {
                            totalSize += msg.getSize();
                            messages.add(new Pop3Message(msg));
                        }
                    }
                }
            } catch (IOException e) {
            } 
        }
    }

    /**
     * Returns the zimbra mailbox id.
     */
    int getId() {
        return id;
    }

    /**
     * Returns total size of all non-deleted messages.
     */
    long getSize() {
        return totalSize - deletedSize;
    }

    /**
     * Returns number of undeleted messages
     */
    int getNumMessages() {
        return messages.size() - numDeleted;
    }

    /**
     * Returns total number of messages, including deleted.
     */
    int getTotalNumMessages() {
        return messages.size();
    }

    /**
     * Gets the message by position in the array, starting at 0, *even if it was deleted*.
     */
    Pop3Message getMsg(int index) throws Pop3CmdException {
        if (index < 0 || index >= messages.size()) {
            throw new Pop3CmdException("invalid message");
        }
        Pop3Message p3m = messages.get(index);
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
     * Gets the undeleted Pop3Message for the specified msg number (external, starting at 1 index).
     */
    Pop3Message getPop3Msg(String msg) throws Pop3CmdException {
        int index = parseInt(msg, "unable to parse msg");
        Pop3Message p3m = getMsg(index - 1);
        if (p3m.isDeleted())
            throw new Pop3CmdException("message is deleted");
        return p3m;
    }

    /**
     * Gets the undeleted Message for the specified msg number (external, starting at 1 index).
     */
    Message getMessage(String msg) throws Pop3CmdException, ServiceException {
        Pop3Message p3m = getPop3Msg(msg);
        Mailbox mbox = MailboxManager.getInstance().getMailboxById(id);
        return mbox.getMessageById(opContext, p3m.getId());
    }

    /**
     * Mark the message as deleted and update counts and mailbox size.
     */
    void delete(Pop3Message p3m) {
        if (!p3m.isDeleted()) {
            p3m.setDeleted(true);
            numDeleted++;
            deletedSize += p3m.getSize();
        }
    }

    /**
     * Unmark all messages that were marked as deleted and return the count that were deleted.
     */
    int undeleteMarked() {
        int count = 0;
        for (int i = 0; i < messages.size(); i++) {
            Pop3Message p3m = messages.get(i);
            if (p3m.isDeleted()) {
                numDeleted--;
                deletedSize -= p3m.getSize();
                p3m.setDeleted(false);
                count++;
            }
        }
        return count;
    }

    /**
     * Delete all DELE'ed messages and return number deleted. Optionally mark RETR'ed messages as read if the delete
     * option is "read".
     *
     * @throws Pop3CmdException the messages were partially deleted
     */
    int expungeDeletes() throws ServiceException, Pop3CmdException {
        Mailbox mbox = MailboxManager.getInstance().getMailboxById(id);
        int count = 0;
        int failed = 0;
        for (Pop3Message p3msg : messages) {
            if (p3msg.isDeleted()) {
                try {
                    switch (deleteOption) {
                        case keep: // Leave DELE'ed messages in Inbox, and flag them as POPED.
                            updateFlags(mbox, p3msg.getId(), true, false);
                            break;
                        case read: // Leave DELE'ed messages in Inbox, and flag them as POPED and READ.
                            updateFlags(mbox, p3msg.getId(), true, true);
                            break;
                        case trash: // Move DELE'ed messages to Trash, and flag them as POPED and READ.
                            updateFlags(mbox, p3msg.getId(), true, true);
                            mbox.move(opContext, p3msg.getId(), MailItem.Type.MESSAGE, Mailbox.ID_FOLDER_TRASH);
                            break;
                        case delete: // Hard-delete DELE'ed messages.
                            mbox.delete(opContext, p3msg.getId(), MailItem.Type.MESSAGE);
                            break;
                        default:
                            assert false : deleteOption;
                    }
                    count++;
                } catch (ServiceException e) {
                    ZimbraLog.pop.warn("Failed to expunge delete", e);
                    failed++;
                }
                numDeleted--;
                deletedSize -= p3msg.getSize();
            } else if (p3msg.isRetrieved()) {
                try {
                    switch (deleteOption) {
                        case read: // Flag RETR'ed messages as READ.
                            updateFlags(mbox, p3msg.getId(), false, true);
                            break;
                    }
                } catch (ServiceException e) {
                    ZimbraLog.pop.warn("Failed to update flags", e);
                }
            }
        }

        if (count > 0) {
            try {
                mbox.resetRecentMessageCount(opContext);
            } catch (ServiceException e) {
                ZimbraLog.pop.info("error resetting mailbox recent message count", e);
            }
        }
        if (failed > 0) {
            throw new Pop3CmdException("deleted " + count + "/" + (count + failed) + " message(s)");
        }
        return count;
    }

    private void updateFlags(Mailbox mbox, int msgId, boolean poped, boolean read) throws ServiceException {
        Message message = mbox.getMessageById(opContext, msgId);
        int flags = message.getFlagBitmask();
        if (poped) {
            flags |= Flag.BITMASK_POPPED; // set POPPED
        }
        if (read) {
            flags &= ~Flag.BITMASK_UNREAD; // unset UNREAD
        }
        mbox.setTags(opContext, msgId, MailItem.Type.MESSAGE, flags, MailItem.TAG_UNCHANGED);
    }

    long getMailboxSize() throws ServiceException {
        Mailbox mbox = MailboxManager.getInstance().getMailboxById(id);
        return mbox.getSize();
    }

    long getInboxNumMessages() throws ServiceException {
        Mailbox mbox = MailboxManager.getInstance().getMailboxById(id);
        return mbox.getFolderById(null, Mailbox.ID_FOLDER_INBOX).getItemCount();
    }
}
