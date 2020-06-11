/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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

/*
 * Created on 2004. 7. 22.
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.zimbra.cs.redolog.op;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import javax.activation.DataSource;

import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.MailboxOperation;
import com.zimbra.cs.redolog.RedoCommitCallback;
import com.zimbra.cs.redolog.RedoLogInput;
import com.zimbra.cs.redolog.RedoLogOutput;

/**
 * @author jhahm
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class CommitTxn extends ControlOp {

    private MailboxOperation mTxnOpCode;
    private RedoLogBlobData blobData = null;

    public CommitTxn() {
        super(MailboxOperation.CommitTxn);
	}

    public CommitTxn(RedoableOp changeEntry) {
    	super(MailboxOperation.CommitTxn, changeEntry.getTransactionId());
        setMailboxId(changeEntry.getMailboxId());
        mTxnOpCode = changeEntry.getOperation();
        mCommitCallback = changeEntry.getCommitCallback();
        if (changeEntry instanceof BlobRecorder) {
            try {
                this.blobData = new RedoLogBlobData((BlobRecorder) changeEntry);
            } catch (IOException e) {
                ZimbraLog.redolog.error("unable to set blob data on CommitTxn for %s (txnId=%s)", mTxnOpCode, changeEntry.getTransactionId(), e);
            }
        }
    }

    @Override
    public MailboxOperation getTxnOpCode() {
        return mTxnOpCode;
    }

    @Override
    protected String getPrintableData() {
        StringBuffer sb = new StringBuffer("txnType=");
        sb.append(mTxnOpCode.name());
        return sb.toString();
    }

    @Override
    protected void serializeData(RedoLogOutput out) throws IOException {
        out.writeInt(mTxnOpCode.getCode());
    }

    @Override
    protected void deserializeData(RedoLogInput in) throws IOException {
        mTxnOpCode = MailboxOperation.fromInt(in.readInt());
    }

    /**
     * Returns the callback object that was passed in at transaction start time.
     * @return
     */
    public RedoCommitCallback getCallback() {
        return mCommitCallback;
    }

    public BlobRecorder getBlobRecorder() {
        return blobData;
    }

    public static class RedoLogBlobData implements BlobRecorder {
        private InputStream in;
        private String digest;
        private long size;
        private Set<Integer> mboxIds;

        public RedoLogBlobData(BlobRecorder blobRecorder) throws IOException {
            this.in = blobRecorder.getBlobInputStream();
            this.size = blobRecorder.getBlobSize();
            this.digest = blobRecorder.getBlobDigest();
            this.mboxIds = blobRecorder.getReferencedMailboxIds();
        }

        @Override
        public String getBlobDigest() {
            return digest;
        }
        @Override
        public InputStream getBlobInputStream() throws IOException {
            return in;
        }

        @Override
        public void setBlobDataFromDataSource(DataSource ds, long size) {
           //unused - this is set in the parent RedoableOp
        }

        @Override
        public void setBlobDataFromFile(File file) {
          //unused  - this is set in the parent RedoableOp

        }

        @Override
        public long getBlobSize() {
            return size;

        }

        @Override
        public Set<Integer> getReferencedMailboxIds() {
            return mboxIds;
        }
    }
}
