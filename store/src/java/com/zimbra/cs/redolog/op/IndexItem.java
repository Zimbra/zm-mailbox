/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.redolog.op;

import java.io.IOException;
import java.util.List;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.index.IndexDocument;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxOperation;
import com.zimbra.cs.redolog.RedoLogInput;
import com.zimbra.cs.redolog.RedoLogOutput;

/**
 * @since 2004. 7. 21.
 */
public class IndexItem extends RedoableOp {

    private int mId;
    private MailItem.Type type;
    private boolean mDeleteFirst; // obsolete
    private boolean mCommitAllowed;
    private boolean mCommitAbortDone;

    public IndexItem() {
        super(MailboxOperation.IndexItem);
        mId = UNKNOWN_ID;
        type = MailItem.Type.UNKNOWN;
        mCommitAllowed = false;
        mCommitAbortDone = false;
    }

    public IndexItem(int mailboxId, int id, MailItem.Type type, boolean deleteFirst) {
        this();
        setMailboxId(mailboxId);
        mId = id;
        this.type = type;
        mDeleteFirst = deleteFirst;
        mCommitAllowed = false;
        mCommitAbortDone = false;
    }

    @Override
    public boolean deferCrashRecovery() {
        return true;
    }

    @Override
    protected String getPrintableData() {
        StringBuffer sb = new StringBuffer("id=");
        sb.append(mId).append(", type=").append(type);
        return sb.toString();
    }

    @Override
    protected void serializeData(RedoLogOutput out) throws IOException {
        out.writeInt(mId);
        out.writeByte(type.toByte());
        if (getVersion().atLeast(1,8)) {
            out.writeBoolean(mDeleteFirst);
        }
    }

    @Override
    protected void deserializeData(RedoLogInput in) throws IOException {
        mId = in.readInt();
        type = MailItem.Type.of(in.readByte());
        if (getVersion().atLeast(1,8)) {
            mDeleteFirst = in.readBoolean();
        } else {
            mDeleteFirst = false;
        }
    }

    @Override
    public void redo() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxById(getMailboxId());
        MailItem item;
        try {
            item = mbox.getItemById(null, mId, type);
        } catch (MailServiceException.NoSuchItemException e) {
            // Because index commits are batched, during mailbox restore
            // it's possible to see the commit record of indexing operation
            // after the delete operation on the item being indexed.
            // (delete followed by edit, for example)
            // We can't distinguish this legitimate case from a case of
            // really missing the item being indexed due to unexpected
            // problem.  So just ignore the NoSuchItemException.
            return;
        }

        try {
            mbox.index.redoIndexItem(item);
        } catch (Exception e) {
            // TODO - update the item and set the item's "unindexed" flag
            ZimbraLog.index.info("Caught exception attempting to replay IndexItem for ID "+mId+" item will not be indexed", e);
        }
    }

    /**
     * An IndexItem transaction is always created as a sub-transaction
     * of a mail item create/modify transaction.  Delicate ordering of
     * log/commit of IndexItem, log/commit of the parent transaction,
     * and database commit is required for correct handling of crash
     * recovery, i.e. to avoid redoing an Index operation before redoing
     * the creation or value modification of the item being indexed.
     *
     * In particular, the commit record for IndexItem should never be
     * written to the redo stream before the commit record for the
     * parent transaction.  The Mailbox class manages all aspects of
     * redo logging except IndexItem commit, which is done in batch by
     * indexer for performance reasons.  Typically the natural delay
     * introduced by the batching ensures IndexItem is committed some
     * time after parent transaction commit, but a boundary case exists
     * in which the batch commit occurs before the main thread writes
     * commit record for parent transaction.
     *
     * To avoid that case, an IndexItem record is created in
     * commit-blocked mode.  Batch index commit thread will skip all
     * IndexItem transactions that are not allowed to commit yet.  The
     * main thread will unblock IndexItem commit after it has written
     * the commit record for the parent transaction.
     * @return
     */
    public synchronized boolean commitAllowed() {
        return mCommitAllowed;
    }

    public synchronized void allowCommit() throws ServiceException {
        mCommitAllowed = true;
        if (mAttachedToParent) {
            commit();
        }
    }

    /**
     * Unlike other RedoableOp classes, commit/abort on IndexItem can be
     * called by two different threads.  It's necessary to remember if
     * commit/abort has been called already and ignore the subsequent
     * calls.
     * @throws ServiceException
     */
    @Override public synchronized void commit() throws ServiceException {
        if (ZimbraLog.index.isDebugEnabled())
            ZimbraLog.index.debug(this.toString()+" committed");

        // Don't check mCommitAllowed here.  It's the responsibility of
        // the caller.
        if (!mCommitAbortDone) {
            super.commit();
            mCommitAbortDone = true;

            // Prevent any thread calling commitAllowed() from spinning.
            mCommitAllowed = true;
        }
    }

    @Override public synchronized void abort() {
        if (!mCommitAbortDone) {
            super.abort();
            mCommitAbortDone = true;

            // Prevent any thread calling commitAllowed() from spinning.
            mCommitAllowed = true;
        }
    }

    private boolean mAttachedToParent;
    private RedoableOp mParentOp;

    public synchronized void setParentOp(RedoableOp op) {
        mParentOp = op;
    }

    public synchronized void attachToParent() throws ServiceException {
        if (!mCommitAllowed && !mAttachedToParent) {
            mAttachedToParent = true;
            if (mParentOp != null)
                mParentOp.addChainedOp(this);
        } else {
            if (ZimbraLog.index.isDebugEnabled()){
                if (mAttachedToParent && !mCommitAllowed) {
                    ZimbraLog.index.debug("Committing because attachToParent called twice!");
                }
            }
            commit();
        }
    }
}
