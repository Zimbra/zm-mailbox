/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2011 Zimbra, Inc.
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

package com.zimbra.cs.store.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.FileUtil;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.cs.db.DbBlobConsistency;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.DbPool.DbConnection;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.store.StoreManager;
import com.zimbra.cs.volume.Volume;
import com.zimbra.cs.volume.VolumeManager;

public final class BlobConsistencyChecker {

    public static class BlobInfo {
        public int itemId;
        public int modContent;
        public int version;
        public long dbSize;
        public String path;
        public short volumeId;

        public int fileModContent;
        public Long fileDataSize;
        public Long fileSize;
    }

    public static class Results {
        public int mboxId;
        public Collection<BlobInfo> missingBlobs = new ArrayList<BlobInfo>();
        public Collection<BlobInfo> incorrectSize = new ArrayList<BlobInfo>();
        public Collection<BlobInfo> unexpectedBlobs = new ArrayList<BlobInfo>();
        public Collection<BlobInfo> incorrectModContent = new ArrayList<BlobInfo>();

        public Results() {
        }

        public Results(Element mboxElement)
        throws ServiceException {
            if (!mboxElement.getName().equals(AdminConstants.E_MAILBOX)) {
                throw ServiceException.INVALID_REQUEST("Unexpected element: " + mboxElement.getName(), null);
            }
            mboxId = (int) mboxElement.getAttributeLong(AdminConstants.A_ID);
            for (Element item : mboxElement.getElement(AdminConstants.E_MISSING_BLOBS).listElements(AdminConstants.E_ITEM)) {
                BlobInfo blob = new BlobInfo();
                blob.itemId = (int) item.getAttributeLong(AdminConstants.A_ID);
                blob.modContent = (int) item.getAttributeLong(AdminConstants.A_REVISION);
                blob.dbSize = item.getAttributeLong(AdminConstants.A_SIZE);
                blob.volumeId = (short) item.getAttributeLong(AdminConstants.A_VOLUME_ID);
                blob.path = item.getAttribute(AdminConstants.A_BLOB_PATH);
                missingBlobs.add(blob);
            }
            for (Element itemEl : mboxElement.getElement(AdminConstants.E_INCORRECT_SIZE).listElements(AdminConstants.E_ITEM)) {
                BlobInfo blob = new BlobInfo();
                blob.itemId = (int) itemEl.getAttributeLong(AdminConstants.A_ID);
                blob.modContent = (int) itemEl.getAttributeLong(AdminConstants.A_REVISION);
                blob.dbSize = itemEl.getAttributeLong(AdminConstants.A_SIZE);
                blob.volumeId = (short) itemEl.getAttributeLong(AdminConstants.A_VOLUME_ID);

                Element blobEl = itemEl.getElement(AdminConstants.E_BLOB);
                blob.path = blobEl.getAttribute(AdminConstants.A_PATH);
                blob.fileDataSize = blobEl.getAttributeLong(AdminConstants.A_SIZE);
                blob.fileSize = blobEl.getAttributeLong(AdminConstants.A_FILE_SIZE);
                incorrectSize.add(blob);
            }
            for (Element blobEl : mboxElement.getElement(AdminConstants.E_UNEXPECTED_BLOBS).listElements(AdminConstants.E_BLOB)) {
                BlobInfo blob = new BlobInfo();
                blob.volumeId = (short) blobEl.getAttributeLong(AdminConstants.A_VOLUME_ID);
                blob.path = blobEl.getAttribute(AdminConstants.A_PATH);
                blob.fileSize = blobEl.getAttributeLong(AdminConstants.A_FILE_SIZE);
                unexpectedBlobs.add(blob);
            }
            for (Element itemEl : mboxElement.getElement(AdminConstants.E_INCORRECT_REVISION).listElements(AdminConstants.E_ITEM)) {
                BlobInfo blob = new BlobInfo();
                blob.itemId = (int) itemEl.getAttributeLong(AdminConstants.A_ID);
                blob.modContent = (int) itemEl.getAttributeLong(AdminConstants.A_REVISION);
                blob.dbSize = itemEl.getAttributeLong(AdminConstants.A_SIZE);
                blob.volumeId = (short) itemEl.getAttributeLong(AdminConstants.A_VOLUME_ID);

                Element blobEl = itemEl.getElement(AdminConstants.E_BLOB);
                blob.path = blobEl.getAttribute(AdminConstants.A_PATH);
                blob.fileSize = blobEl.getAttributeLong(AdminConstants.A_FILE_SIZE);
                blob.fileModContent = (int) blobEl.getAttributeLong(MailConstants.A_REVISION);
                incorrectModContent.add(blob);
            }
        }

        public boolean hasInconsistency() {
            return !(missingBlobs.isEmpty() && incorrectSize.isEmpty() &&
                unexpectedBlobs.isEmpty() && incorrectModContent.isEmpty());
        }

        public void toElement(Element parent) {
            Element missingEl = parent.addElement(AdminConstants.E_MISSING_BLOBS);
            Element incorrectSizeEl = parent.addElement(AdminConstants.E_INCORRECT_SIZE);
            Element unexpectedBlobsEl = parent.addElement(AdminConstants.E_UNEXPECTED_BLOBS);
            Element incorrectRevisionEl = parent.addElement(AdminConstants.E_INCORRECT_REVISION);

            for (BlobInfo blob : missingBlobs) {
                missingEl.addElement(AdminConstants.E_ITEM)
                    .addAttribute(AdminConstants.A_ID, blob.itemId)
                    .addAttribute(AdminConstants.A_REVISION, blob.modContent)
                    .addAttribute(AdminConstants.A_SIZE, blob.dbSize)
                    .addAttribute(AdminConstants.A_VOLUME_ID, blob.volumeId)
                    .addAttribute(AdminConstants.A_BLOB_PATH, blob.path);
            }
            for (BlobInfo blob : incorrectSize) {
                Element itemEl = incorrectSizeEl.addElement(AdminConstants.E_ITEM)
                    .addAttribute(AdminConstants.A_ID, blob.itemId)
                    .addAttribute(MailConstants.A_REVISION, blob.modContent)
                    .addAttribute(AdminConstants.A_SIZE, blob.dbSize)
                    .addAttribute(AdminConstants.A_VOLUME_ID, blob.volumeId);
                itemEl.addElement(AdminConstants.E_BLOB)
                    .addAttribute(AdminConstants.A_PATH, blob.path)
                    .addAttribute(AdminConstants.A_SIZE, blob.fileDataSize)
                    .addAttribute(AdminConstants.A_FILE_SIZE, blob.fileSize);
            }
            for (BlobInfo blob : unexpectedBlobs) {
                unexpectedBlobsEl.addElement(AdminConstants.E_BLOB)
                    .addAttribute(AdminConstants.A_VOLUME_ID, blob.volumeId)
                    .addAttribute(AdminConstants.A_PATH, blob.path)
                    .addAttribute(AdminConstants.A_FILE_SIZE, blob.fileSize);
            }
            for (BlobInfo blob : incorrectModContent) {
                Element itemEl = incorrectRevisionEl.addElement(AdminConstants.E_ITEM)
                    .addAttribute(AdminConstants.A_ID, blob.itemId)
                    .addAttribute(MailConstants.A_REVISION, blob.modContent)
                    .addAttribute(AdminConstants.A_SIZE, blob.dbSize)
                    .addAttribute(AdminConstants.A_VOLUME_ID, blob.volumeId);
                itemEl.addElement(AdminConstants.E_BLOB)
                    .addAttribute(AdminConstants.A_PATH, blob.path)
                    .addAttribute(AdminConstants.A_FILE_SIZE, blob.fileSize)
                    .addAttribute(MailConstants.A_REVISION, blob.fileModContent);
            }
        }
    }

    private static final Log sLog = LogFactory.getLog(BlobConsistencyChecker.class);
    private Results mResults;
    private int mMailboxId;
    private boolean mCheckSize = true;

    public BlobConsistencyChecker() {
    }

    public Results check(Collection<Short> volumeIds, int mboxId, boolean checkSize)
    throws ServiceException {
        StoreManager sm = StoreManager.getInstance();
        if (!(sm instanceof FileBlobStore)) {
            throw ServiceException.INVALID_REQUEST(sm.getClass().getSimpleName() + " is not supported", null);
        }

        mMailboxId = mboxId;
        mCheckSize = checkSize;
        mResults = new Results();
        Mailbox mbox = MailboxManager.getInstance().getMailboxById(mMailboxId);
        DbConnection conn = null;

        try {
            conn = DbPool.getConnection();

            for (short volumeId : volumeIds) {
                Volume vol = VolumeManager.getInstance().getVolume(volumeId);
                if (vol.getType() == Volume.TYPE_INDEX) {
                    sLog.warn("Skipping index volume %d.  Only message volumes are supported.", vol.getId());
                    continue;
                }
                int numGroups = 1 << vol.getFileGroupBits();
                int filesPerGroup = 1 << vol.getFileBits();
                int mailboxMaxId = DbBlobConsistency.getMaxId(conn, mbox); // Maximum id for the entire mailbox

                // Iterate group directories one at a time, looking up all item id's
                // that have blobs in each directory.  Each group can have multiple blocks
                // of id's if we wrap from group 255 back to group 0.
                int minId = 0; // Minimum id for the current block
                int group = 0; // Current group number

                while (minId < mailboxMaxId && group < numGroups) {
                    // We used Multimap to make sure we store multiple BlobInfo objects for the same itemId
                    // multiple BlobInfo objects are created when there are multiple revisions of the same file
                    Multimap<Integer, BlobInfo> blobsById = HashMultimap.create();
                    int maxId = minId + filesPerGroup - 1; // Maximum id for the current block
                    String blobDir = vol.getBlobDir(mbox.getId(), minId);

                    while (minId < mailboxMaxId) {
                        for (BlobInfo blob : DbBlobConsistency.getBlobInfo(conn, mbox, minId, maxId, volumeId)) {
                            blobsById.put(blob.itemId, blob);
                        }
                        minId += (numGroups * filesPerGroup);
                    }
                    try {
                        check(volumeId, blobDir, blobsById);
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

    private static final Pattern PAT_BLOB_FILENAME = Pattern.compile("([0-9]+)-([0-9]+)\\.msg");

    /**
     * Reconciles blobs against the files in the given directory and adds any inconsistencies
     * to the current result set.
     */
    private void check(short volumeId, String blobDirPath, Multimap<Integer, BlobInfo> blobsById)
    throws IOException {
        File blobDir = new File(blobDirPath);
        File[] files = blobDir.listFiles();
        if (files == null) {
            files = new File[0];
        }
        sLog.info("Comparing %d items to %d files in %s.", blobsById.size(), files.length, blobDirPath);
        for (File file : files) {
            // Parse id and mod_content value from filename.
            Matcher matcher = PAT_BLOB_FILENAME.matcher(file.getName());
            int itemId = 0;
            int modContent = 0;
            if (matcher.matches()) {
                itemId = Integer.parseInt(matcher.group(1));
                modContent = Integer.parseInt(matcher.group(2));
            }

            BlobInfo blob = null;
            if (blobsById.containsKey(itemId)) {
                for (BlobInfo tempBlob : blobsById.get(itemId)) {
                    if (tempBlob.modContent == modContent) {
                        blob = tempBlob;
                        blobsById.remove(itemId, tempBlob);
                        break;
                    }
                }
            }

            if (blob == null) {
                BlobInfo unexpected = new BlobInfo();
                unexpected.volumeId = volumeId;
                unexpected.path = file.getAbsolutePath();
                unexpected.fileSize = file.length();
                mResults.unexpectedBlobs.add(unexpected);
            } else {
                blob.fileSize = file.length();
                blob.fileModContent = modContent;

                if (mCheckSize) {
                    blob.fileDataSize = getDataSize(file, blob.dbSize);
                    if (blob.dbSize != blob.fileDataSize) {
                        mResults.incorrectSize.add(blob);
                    }
                }

                if (blob.modContent != blob.fileModContent) {
                    blob.path = file.getAbsolutePath();
                    mResults.incorrectModContent.add(blob);
                }
            }
        }

        // Any remaining items have missing blobs.
        for (BlobInfo blob : blobsById.values()) {
            mResults.missingBlobs.add(blob);
        }
    }

    private static long getDataSize(File file, long expected)
    throws IOException {
        long fileLen = file.length();
        if (fileLen != expected && FileUtil.isGzipped(file)) {
            return ByteUtil.getDataLength(new GZIPInputStream(new FileInputStream(file)));
        } else {
            return fileLen;
        }
    }
}
