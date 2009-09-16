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

package com.zimbra.cs.store.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.FileUtil;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.cs.db.DbBlobConsistency;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.DbPool.Connection;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.store.StoreManager;

public class BlobConsistencyChecker {

    public static class BlobInfo {
        public long itemId;
        public int modContent;
        public int version;
        public long dbSize;
        public Long fileDataSize;
        public Long fileSize;
        public String path;
        public short volumeId;
    }
    
    public static class Results {
        public Collection<BlobInfo> missingBlobs = new ArrayList<BlobInfo>();
        public Collection<File> unexpectedFiles = new ArrayList<File>();
        public Collection<BlobInfo> incorrectSize = new ArrayList<BlobInfo>();
    }

    private static final Log sLog = LogFactory.getLog(BlobConsistencyChecker.class);
    private Results mResults;
    private long mMailboxId;
    private boolean mCheckSize = true;
    
    public BlobConsistencyChecker() {
    }
    
    public Results check(Collection<Short> volumeIds, long mboxId, boolean checkSize)
    throws ServiceException {
        StoreManager sm = StoreManager.getInstance();
        if (!(sm instanceof FileBlobStore)) {
            throw ServiceException.INVALID_REQUEST(sm.getClass().getSimpleName() + " is not supported", null);
        }

        mMailboxId = mboxId;
        mCheckSize = checkSize;
        mResults = new Results();
        Mailbox mbox = MailboxManager.getInstance().getMailboxById(mMailboxId);
        Connection conn = null;

        try {
            conn = DbPool.getConnection();
            
            for (short volumeId : volumeIds) {
                Volume vol = Volume.getById(volumeId);
                if (vol.getType() == Volume.TYPE_INDEX) {
                    sLog.warn("Skipping index volume %d.  Only message volumes are supported.", vol.getId());
                    continue;
                }
                int numGroups = 1 << vol.getFileGroupBits();
                int filesPerGroup = 1 << vol.getFileBits();
                long mailboxMaxId = DbBlobConsistency.getMaxId(conn, mbox); // Maximum id for the entire mailbox

                // Iterate group directories one at a time, looking up all item id's
                // that have blobs in each directory.  Each group can have multiple blocks
                // of id's if we wrap from group 255 back to group 0.
                long minId = 0; // Minimum id for the current block
                int group = 0; // Current group number
                
                while (minId < mailboxMaxId && group < numGroups) {
                    Map<String, BlobInfo> blobsByName = new HashMap<String, BlobInfo>();
                    long maxId = minId + filesPerGroup - 1; // Maximum id for the current block
                    String blobDir = vol.getBlobDir(mbox.getId(), (int) minId);
                    
                    while (minId < mailboxMaxId) {
                        for (BlobInfo blob : DbBlobConsistency.getBlobInfo(conn, mbox, minId, maxId, volumeId)) {
                            StringBuffer buf = new StringBuffer();
                            FileBlobStore.appendFilename(buf, (int) blob.itemId, blob.modContent);
                            blobsByName.put(buf.toString(), blob);
                        }
                        minId += (numGroups * filesPerGroup);
                    }
                    try {
                        check(blobDir, blobsByName);
                    } catch (IOException e) {
                        throw ServiceException.FAILURE("Unable to check " + blobDir, e);
                    }
                    
                    group++;
                    minId = group * filesPerGroup; // Set minId to the smallest id in the next group
                }
            }
        } finally {
            DbPool.quietClose(conn);
        }
        return mResults;
    }
    
    /**
     * Reconciles blobs against the files in the given directory and adds any inconsistencies
     * to the current result set.
     */
    private void check(String blobDirPath, Map<String, BlobInfo> blobsByName)
    throws IOException {
        File blobDir = new File(blobDirPath);
        File[] files = blobDir.listFiles();
        if (files == null) {
            files = new File[0];
        }
        sLog.info("Comparing %d items to %d files in %s.", blobsByName.size(), files.length, blobDirPath);
        for (File file : files) {
            BlobInfo blob = blobsByName.remove(file.getName());
            if (blob == null) {
                mResults.unexpectedFiles.add(file);
            } else if (mCheckSize) {
                long size;
                if (FileUtil.isGzipped(file)) {
                    size = ByteUtil.getDataLength(new GZIPInputStream(new FileInputStream(file)));
                } else {
                    size = file.length();
                }
                blob.fileDataSize = size;
                blob.fileSize = file.length();
                if (blob.dbSize != blob.fileDataSize) {
                    mResults.incorrectSize.add(blob);
                }
            }
        }
        
        // Any remaining items have missing blobs.
        for (BlobInfo blob : blobsByName.values()) {
            mResults.missingBlobs.add(blob);
        }
    }
}
