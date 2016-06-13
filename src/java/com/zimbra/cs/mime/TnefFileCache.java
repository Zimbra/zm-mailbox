/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.cs.mime;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.FileUtil;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;

public class TnefFileCache {

    private static TnefFileCache sInstance;
    private static final Log sLog = LogFactory.getLog(TnefFileCache.class);

    private File mCacheDir;
    
    /**
     * Maps the hashcode of the <tt>MimeMessage</tt> object to one or more digests
     * of TNEF parts.
     */
    private Multimap<Integer, String> mMessageToPartDigests = HashMultimap.create(); 
    
    /**
     * Maps the TNEF part digest to the file on disk.
     */
    private Map<String, File> mDigestToFile = new HashMap<String, File>();
    
    private TnefFileCache(File cacheDir) {
        mCacheDir = cacheDir;
    }
    
    public static TnefFileCache getInstance() {
        if (sInstance == null) {
            throw new IllegalStateException(TnefFileCache.class.getSimpleName() + " has not been started");
        }
        return sInstance;
    }
    
    public synchronized static void startup()
    throws IOException {
        if (sInstance == null) {
            File cacheDir = new File(LC.zimbra_tmp_directory.value() + File.separator + "tnef");
            sLog.debug("Initializing TNEF cache in %s", cacheDir);
            FileUtil.deleteDir(cacheDir);
            FileUtil.ensureDirExists(cacheDir);
            sInstance = new TnefFileCache(cacheDir);
        }
    }
    
    public synchronized static void shutdown() {
        if (sInstance != null) {
            try {
                FileUtil.deleteDir(sInstance.mCacheDir);
            } catch (IOException e) {
                sLog.warn("Unable to delete TNEF cache directory %s.", sInstance.mCacheDir, e);
            }
        }
    }
    
    public File getFile(MimeMessage msg, MimeBodyPart part)
    throws IOException, MessagingException {
        int msgHashcode = msg.hashCode();
        String digest = ByteUtil.getSHA1Digest(part.getInputStream(), true);
        sLog.debug("Getting file for MimeMessage %d, part digest %s.", msgHashcode, digest);
        
        synchronized (this) {
            // Map the message hashcode to the digest of the TNEF part.
            mMessageToPartDigests.put(msgHashcode, digest);

            // Save the TNEF part to disk and put it into the digest map
            // to indicate that the file is there.
            File file = mDigestToFile.get(digest);
            if (file == null) {
                file = new File(mCacheDir, digest);
                sLog.debug("Saving TNEF content to %s.", file);
                part.saveFile(file);
                mDigestToFile.put(digest, file);
            }
            return file;
        }
    }
    
    public synchronized void purge(MimeMessage msg) {
        if (msg == null) {
            return;
        }
        int hashCode = msg.hashCode();
        sLog.debug("Purging message with hashcode %d.", hashCode);
        
        Collection<String> digests = mMessageToPartDigests.removeAll(hashCode);
        if (digests != null && !digests.isEmpty()) {
            // Delete the file only if there isn't another message that
            // is referencing it.
            Set<String> allDigests = getAllMessagePartDigests();
            for (String digest : digests) {
                if (!allDigests.contains(digest)) {
                    File file = mDigestToFile.remove(digest);
                    sLog.debug("Deleting %s.", file);
                    if (!file.delete()) {
                        sLog.warn("Unable to delete %s.", file);
                    }
                } else {
                    sLog.debug("Not deleting file on disk because it is referenced by another message.");
                }
            }
        }
    }
    
    /**
     * Returns all part digests from the <tt>mMessageToPart</tt> map.
     */
    private synchronized Set<String> getAllMessagePartDigests() {
        Set<String> allDigests = new HashSet<String>();
        for (int hashCode : mMessageToPartDigests.keySet()) {
            allDigests.addAll(mMessageToPartDigests.get(hashCode));
        }
        return allDigests;
    }
}
