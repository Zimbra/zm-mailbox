/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxOperation;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.redolog.RedoLogInput;
import com.zimbra.cs.redolog.RedoLogOutput;
import com.zimbra.cs.store.Blob;

public class AddDocumentRevision extends SaveDocument {
    private int mDocId;

    public AddDocumentRevision() {
        mOperation = MailboxOperation.AddDocumentRevision;
    }

    public AddDocumentRevision(int mailboxId, String digest, long msgSize, int folderId) {
        super(mailboxId, digest, msgSize, folderId, 0);
        mOperation = MailboxOperation.AddDocumentRevision;
    }

    public void setDocId(int docId) {
        mDocId = docId;
    }

    public int getDocId() {
        return mDocId;
    }

    @Override protected void serializeData(RedoLogOutput out) throws IOException {
        out.writeInt(mDocId);
        super.serializeData(out);
    }

    @Override protected void deserializeData(RedoLogInput in) throws IOException {
        mDocId = in.readInt();
        super.deserializeData(in);
    }

    @Override public void redo() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxById(getMailboxId());
        OperationContext octxt = getOperationContext();
        try {
            if (mMsgBodyType == MSGBODY_LINK) {
                Blob blob = mRedoLogMgr.getBlobStore().fetchBlob(mPath);
                mbox.addDocumentRevision(octxt, mDocId, getAuthor(), getFilename(), getDescription(), isDescriptionEnabled(), blob);
            } else {
                mbox.addDocumentRevision(octxt, mDocId, getAuthor(), getFilename(), getDescription(), isDescriptionEnabled(), getAdditionalDataStream());
            }
        } catch (MailServiceException e) {
            if (e.getCode() == MailServiceException.ALREADY_EXISTS) {
                mLog.info("Document revision " + getMessageId() + " is already in mailbox " + mbox.getId());
                return;
            } else {
                throw e;
            }
        }
    }
}
