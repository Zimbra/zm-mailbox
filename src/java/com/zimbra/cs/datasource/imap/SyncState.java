/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.datasource.imap;

import com.zimbra.cs.mailclient.imap.MailboxInfo;

public class SyncState {
    private long lastUid = -1;
    private int lastModSeq;
    private MailboxInfo mailboxInfo;

    public SyncState() {}

    public long getLastUid() {
        return lastUid;
    }

    public MailboxInfo getMailboxInfo() {
        return mailboxInfo;
    }
    
    public int getLastModSeq() {
        return lastModSeq;
    }

    public void setLastUid(long lastUid) {
        this.lastUid = lastUid;
    }

    public void updateLastUid(long uid) {
        if (uid > lastUid) {
            lastUid = uid;
        }
    }

    public void setLastModSeq(int lastModSeq) {
        this.lastModSeq = lastModSeq;
    }

    public void setMailboxInfo(MailboxInfo mi) {
        mailboxInfo = mi;
    }

    public long getLastUidNext() {
        return mailboxInfo != null ? mailboxInfo.getUidNext() : -1;
    }

    public long getLastUidValidity() {
        return mailboxInfo != null ? mailboxInfo.getUidValidity() : -1;
    }

    public String toString() {
        return String.format(
            "{lastUid=%d,lastModSeq=%d,uidNext=%d,uidValidity=%d}",
            lastUid, lastModSeq, getLastUidNext(), getLastUidValidity());
    }
}
