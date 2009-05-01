/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.mailbox.calendar.cache;

import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.Mountpoint;
import com.zimbra.cs.zclient.ZFolder;
import com.zimbra.cs.zclient.ZMountpoint;

// for CalDAV
// tracks last modified "time" of a calendar folder
// includes mountpoint link
public class CtagInfo {

    private int mId;            // id of this calendar folder
    private int mFolderId;      // id of parent folder
    private String mCtag;       // "ctag" that works like a last modified timestamp
    private String mPath;

    // applicable when this calendar is a mountpoint to a remote calendar
    private boolean mIsMountpoint;
    private String mRemoteAccount;  // mountpoint target account
    private int mRemoteId;          // id of the calendar folder in target account

    public int getId() { return mId; }
    public int getFolderId() { return mFolderId; }
    public String getCtag() { return mCtag; }
    public String getPath() { return mPath; }
    public boolean isMountpoint() { return mIsMountpoint; }
    public String getRemoteAccount() { return mRemoteAccount; }
    public int getRemoteId() { return mRemoteId; }

    CtagInfo(Folder calFolder) {
        boolean isMountpoint = calFolder instanceof Mountpoint;
        String remoteAccount = null;
        int remoteId = -1;
        if (isMountpoint) {
            Mountpoint mp = (Mountpoint) calFolder;
            remoteAccount = mp.getOwnerId();
            remoteId = mp.getRemoteId();
        }
        String ctag = makeCtag(calFolder);
        init(calFolder.getId(), calFolder.getFolderId(), ctag, calFolder.getPath(),
             isMountpoint, remoteAccount, remoteId);
    }

    CtagInfo(ZFolder calFolder) {
        boolean isMountpoint = calFolder instanceof ZMountpoint;
        String remoteAccount = null;
        int remoteId = -1;
        if (isMountpoint) {
            ZMountpoint mp = (ZMountpoint) calFolder;
            remoteAccount = mp.getOwnerId();
            remoteId = Integer.parseInt(mp.getRemoteId());
        }
        String ctag = makeCtag(calFolder);
        init(Integer.parseInt(calFolder.getId()), Integer.parseInt(calFolder.getParentId()),
             ctag, calFolder.getPath(), isMountpoint, remoteAccount, remoteId);
    }

    private void init(int id, int parentFolderId, String ctag, String path,
                      boolean isMountpoint, String remoteAccountId, int remoteId) {
        mId = id;
        mFolderId = parentFolderId;
        mCtag = ctag;
        mIsMountpoint = isMountpoint;
        if (mIsMountpoint) {
            mRemoteAccount = remoteAccountId;
            mRemoteId = remoteId;
        }
        mPath = path;
    }

    void setCtag(String ctag) { mCtag = ctag; }

    private static String makeCtag(int modMetadata, int imapModseq) {
        return String.format("%d-%d", modMetadata, imapModseq);
    }

    public static String makeCtag(Folder calFolder) {
        return makeCtag(calFolder.getModifiedSequence(), calFolder.getImapMODSEQ());
    }

    public static String makeCtag(ZFolder calFolder) {
        return makeCtag(calFolder.getModifiedSequence(), calFolder.getImapMODSEQ());
    }
}
