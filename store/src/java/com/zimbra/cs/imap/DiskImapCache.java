/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016, 2019 Synacor, Inc.
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
package com.zimbra.cs.imap;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Comparator;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.cs.io.SecureObjectInputStream;

/**
 * IMAP cache using local disk.
 *
 * @author dkarp
 * @author ysasaki
 */
final class DiskImapCache implements ImapSessionManager.Cache<String, ImapFolder> {

    private static final String CACHE_DATA_SUBDIR =
            "data" + File.separator + "mailboxd" + File.separator + "imap" + File.separator + "cache";
    private static final File CACHE_DIR = new File(LC.zimbra_home.value(), CACHE_DATA_SUBDIR);
    private static final String IMAP_CACHEFILE_SUFFIX = ".i4c";

    DiskImapCache() {
        CACHE_DIR.mkdirs();

        // iterate over all serialized folders and delete all but the most recent
        File[] allCached = CACHE_DIR.listFiles();
        Arrays.sort(allCached, new Comparator<File>() {
            @Override public int compare(File o1, File o2)  {
                return o1.getName().compareTo(o2.getName());
            }
        });
        File previous = null;
        String lastOwner = "", lastId = "";
        for (File cached : allCached) {
            String split = ImapSessionManager.isActiveKey(cached.getName()) ? "_" : ":";
            String[] parts = cached.getName().split(split);
            if (previous != null && parts.length >= 4) {
                if (lastOwner.equals(parts[0]) && lastId.equals(parts[1])) {
                    previous.delete();
                } else {
                    removeSessionFromFilename(previous);
                }
            }
            lastOwner = parts[0];  lastId = parts[1];
            previous = cached;
        }
        removeSessionFromFilename(previous);
    }

    /**
     * Renames the passed-in {@link File} by removing everything from the {@code +} character to the extension. If the
     * filename does not contain {@code +}, does nothing.
     */
    private static void removeSessionFromFilename(File file) {
        if (file == null) {
            return;
        }
        String filename = file.getName();
        if (filename.contains("+")) {
            file.renameTo(new File(CACHE_DIR, filename.substring(0, filename.lastIndexOf("+")) + IMAP_CACHEFILE_SUFFIX));
        }
    }

    @Override
    public void put(String key, ImapFolder folder) {
        File pagefile = new File(CACHE_DIR, key + IMAP_CACHEFILE_SUFFIX);
        if (pagefile.exists()) {
            return;
        }
        FileOutputStream fos = null;
        ObjectOutputStream oos = null;
        try {
            oos = new ObjectOutputStream(fos = new FileOutputStream(pagefile));
            synchronized (folder) {
                oos.writeObject(folder);
            }
        } catch (IOException e) {
            ByteUtil.closeStream(oos);
            ByteUtil.closeStream(fos);
            pagefile.delete();
        } finally {
            ByteUtil.closeStream(oos);
            ByteUtil.closeStream(fos);
        }
    }

    @Override
    public ImapFolder get(String key) {
        File pagefile = new File(CACHE_DIR, key + IMAP_CACHEFILE_SUFFIX);
        if (!pagefile.exists()) {
            return null;
        }
        FileInputStream fis = null;
        ObjectInputStream ois = null;
        try {
            // read serialized ImapFolder from cache
            ois = new SecureObjectInputStream(fis = new FileInputStream(pagefile), ImapFolder.class.getName());
            return (ImapFolder) ois.readObject();
        } catch (Exception e) {
            ByteUtil.closeStream(ois);
            ByteUtil.closeStream(fis);
            pagefile.delete();
            return null;
        } finally {
            ByteUtil.closeStream(ois);
            ByteUtil.closeStream(fis);
        }
    }

    @Override
    public void remove(String key) {
        File pagefile = new File(CACHE_DIR, key + IMAP_CACHEFILE_SUFFIX);
        pagefile.delete();
    }

    @Override
    public void updateAccessTime(String key) {
        //nothing to do; disk cache never expires
    }
}
