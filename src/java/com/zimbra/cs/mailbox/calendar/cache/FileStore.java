/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.mailbox.calendar.cache;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.FileUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.Metadata;

public class FileStore {
    private static final String FILE_EXT = ".dat";
    private static final int MBOX_GROUP_BITS = 8;
    private static final int MBOX_BITS = 12;
    private static final int FILE_GROUP_BITS = 8;
    private static final int FILE_BITS = 12;

    private static final int CURRENT_VERSION = 1;
    private static final String FN_VERSION = "ver";
    private static final String FN_CALDATA = "calData";
    private static final String FN_MODSEQ = "modSeq";

    private static File getCalFolderFile(int mboxId, int folderId) {
        long mdir = mboxId >> MBOX_BITS;
        mdir &= MBOX_GROUP_BITS;
        long fdir = folderId >> FILE_BITS;
        fdir &= FILE_GROUP_BITS;

        StringBuilder sb = new StringBuilder(LC.calendar_cache_directory.value());
        sb.append(File.separator).append(mdir).append(File.separator).append(mboxId);
        sb.append(File.separator).append(mdir).append(File.separator).append(folderId).append(FILE_EXT);
        return new File(sb.toString());
    }

    static void deleteCalendarData(int mboxId, int folderId)
    throws ServiceException {
        File file = getCalFolderFile(mboxId, folderId);
        if (file.exists())
            file.delete();
    }

    static void saveCalendarData(int mboxId, CalendarData calData)
    throws ServiceException {
        File file = getCalFolderFile(mboxId, calData.getFolderId());
        try {
            FileUtil.ensureDirExists(file.getParentFile());
        } catch (IOException e) {
            throw ServiceException.FAILURE(
                    "Unable to create directory " + file.getParentFile().getAbsolutePath(), e);
        }
        Metadata meta = new Metadata();
        meta.put(FN_VERSION, CURRENT_VERSION);
        meta.put(FN_MODSEQ, calData.getModSeq());
        meta.put(FN_CALDATA, calData.encodeMetadata());
        String encoded = meta.toString();
        saveToFile(file, encoded);
    }

    static CalendarData loadCalendarData(int mboxId, int folderId, int modSeq)
    throws ServiceException {
        File file = getCalFolderFile(mboxId, folderId);
        String encoded = loadFromFile(file);
        if (encoded == null)
            return null;

        Metadata meta = new Metadata(encoded);
        if (!meta.containsKey(FN_VERSION)) {
            ZimbraLog.calendar.warn("Cache file missing version field: path=" + file.getAbsolutePath());
            return null;
        }
        int ver = (int) meta.getLong(FN_VERSION);
        if (ver < CURRENT_VERSION) {
            if (ZimbraLog.calendar.isDebugEnabled())
                ZimbraLog.calendar.debug(
                        "Cached data's version too old: cached=" + ver + ", expected=" + CURRENT_VERSION +
                        ", path=" + file.getAbsolutePath());
            return null;
        }

        int modSeqSaved = (int) meta.getLong(FN_MODSEQ);
        if (modSeqSaved < modSeq) {
            if (ZimbraLog.calendar.isDebugEnabled())
                ZimbraLog.calendar.debug(
                        "Cached data stale: saved modseq=" + modSeqSaved + ", needed modseq=" + modSeq +
                        ", path=" + file.getAbsolutePath());
            return null;
        }

        Metadata metaCalData = meta.getMap(FN_CALDATA, true);
        if (metaCalData == null) {
            ZimbraLog.calendar.warn("Cache file missing actual data: path=" + file.getAbsolutePath());
            return null;
        }

        return new CalendarData(metaCalData);
    }

    private static void saveToFile(File file, String str) throws ServiceException {
        File tmpFile = new File(file.getAbsolutePath() + ".tmp");
        try {
            FileWriter fw = null;
            try {
                fw = new FileWriter(tmpFile);
                fw.write(str);
            } finally {
                if (fw != null)
                    fw.close();
            }
            tmpFile.renameTo(file);
        } catch (IOException e) {
            throw ServiceException.FAILURE("IOException while saving to " + file.getAbsolutePath(), e);
        }
    }

    private static String loadFromFile(File file) throws ServiceException {
        if (file.exists()) {
            try {
                long length = file.length();
                char[] buf = new char[(int) length];
                FileReader fr = null;
                try {
                    fr = new FileReader(file);
                    int bytesRead = fr.read(buf);
                    if (bytesRead < length)
                        throw ServiceException.FAILURE(
                                "Read " + bytesRead + " bytes when expecting " + length +
                                " bytes, from file " + file.getAbsolutePath(), null);
                    return new String(buf);
                } finally {
                    if (fr != null)
                        fr.close();
                }
            } catch (FileNotFoundException e) {
                return null;
            } catch (IOException e) {
                throw ServiceException.FAILURE("IOException while reading from " + file.getAbsolutePath(), e);
            }
        }
        return null;
    }
}
