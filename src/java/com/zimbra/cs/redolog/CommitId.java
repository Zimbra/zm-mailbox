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
 * Portions created by Zimbra are Copyright (C) 2007 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.redolog;

import com.zimbra.common.service.ServiceException;

/**
 * CommitId consists of redolog sequence number and TransactionId of a
 * redo transaction.  It helps locate an exact point in redo history,
 * by going to the redolog of the given sequence and scanning its content
 * until finding the commit record (not the log record) matching the
 * TransactionId.
 */
public class CommitId {

    private long mRedoSeq;  // sequence of redo log at transaction commit
    private TransactionId mTxnId;

    public CommitId(long seq, TransactionId txnId) {
        mRedoSeq = seq;
        mTxnId = txnId;
    }

    public boolean matches(TransactionId txnId) {
        return mTxnId.equals(txnId);
    }

    public long getRedoSeq() {
        return mRedoSeq;
    }

    public String encodeToString() {
        int time = mTxnId.getTime();
        int counter = mTxnId.getCounter();
        StringBuilder sb = new StringBuilder();
        sb.append(mRedoSeq).append('-');
        sb.append(mTxnId.encodeToString());
        return sb.toString();
    }

    public static CommitId decodeFromString(String str)
    throws ServiceException {
        Throwable cause = null;
        if (str != null) {
            String[] fields = str.split("-", 2);
            if (fields != null && fields.length == 2) {
                try {
                    long seq = Long.parseLong(fields[0]);
                    TransactionId txnId = TransactionId.decodeFromString(fields[1]);
                    return new CommitId(seq, txnId);
                } catch (NumberFormatException e) {
                    cause = e;
                } catch (ServiceException e) {
                    cause = e;
                }
            }
        }
        throw ServiceException.PARSE_ERROR("Invalid CommitId " + str, cause);
    }

    public String toString() {
        return encodeToString();
    }
}
