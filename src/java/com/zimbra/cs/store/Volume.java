/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.1
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.store;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.DbVolume;
import com.zimbra.cs.db.DbPool.Connection;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.util.Zimbra;

/**
 * @author jhahm
 */
public class Volume {

    private static Log sLog = LogFactory.getLog(Volume.class);

    private static void dumpCache(String title) {
        StringBuffer sb = new StringBuffer();
        synchronized (sVolumeGuard) {
            for (Iterator iter = sVolumeMap.entrySet().iterator(); iter.hasNext(); ) {
                Map.Entry entry = (Map.Entry) iter.next();
                Object k = entry.getKey();
                Object v = entry.getValue();
                sb.append("key: ").append(k.toString()).append("\r\n");
                sb.append("val: ").append(v.toString()).append("\r\n");
            }
        }
        sLog.debug("VOLUME MAP DUMP - " + title + "\r\n" + sb.toString());
    }

    public static final short ID_AUTO_INCREMENT = -1;

    public static final int TYPE_MESSAGE = 1;
    public static final int TYPE_INDEX = 2;

    public static final String TYPE_MESSAGE_STR = "msg";
    public static final String TYPE_INDEX_STR = "index";

    private static final String SUBDIR_MESSAGE = "msg";
    private static final String SUBDIR_INDEX = "index";

    private static final String INCOMING_DIR = "incoming";

    // sVolumeMap, sCurrMsgVolume, and sCurrIndexVolume are all
    // synchronized on sVolumeGuard.
    private static final Object sVolumeGuard = new Object();
    private static Map sVolumeMap = new HashMap();
    private static Volume sCurrMsgVolume;
    private static Volume sCurrIndexVolume;

    static {
        try {
            reloadVolumes();
        } catch (ServiceException e) {
            Zimbra.halt("Unable to load volumes info", e);
        }
    }

    public static int parseTypeStr(String typeStr)
    throws ServiceException {
        if (TYPE_MESSAGE_STR.equals(typeStr))
            return TYPE_MESSAGE;
        else if (TYPE_INDEX_STR.equals(typeStr))
            return TYPE_INDEX;

        throw ServiceException.INVALID_REQUEST("Invalid volume type \"" +
                                               typeStr + "\"", null);
    }

    public static void reloadVolumes() throws ServiceException {
        Connection conn = null;
        try {
            conn = DbPool.getConnection();
            Map volumes = DbVolume.getAll(conn);
            DbVolume.CurrentVolumes currVols = DbVolume.getCurrentVolumes(conn);
            if (currVols == null)
                throw ServiceException.FAILURE("Missing current volumes info from configuration", null);

            Volume currMsgVol = (Volume) volumes.get(new Short(currVols.msgVolId));
            if (currMsgVol == null)
                throw ServiceException.FAILURE("Unknown current message volume " + currVols.msgVolId, null);
            Volume currIndexVol = (Volume) volumes.get(new Short(currVols.indexVolId));
            if (currIndexVol == null)
                throw ServiceException.FAILURE("Unknown current index volume " + currVols.indexVolId, null);

            // All looks good.  Update current values.
            synchronized (sVolumeGuard) {
            	sVolumeMap.clear();
                sVolumeMap.putAll(volumes);
                sCurrMsgVolume = currMsgVol;
                sCurrIndexVolume = currIndexVol;
            }
        } finally {
            if (conn != null)
                DbPool.quietClose(conn);
        }
    }

    public static Volume create(short id,
                                String name, String path,
                                short mboxGroupBits, short mboxBits,
                                short fileGroupBits, short fileBits)
    throws ServiceException {
        if (id != ID_AUTO_INCREMENT) {
            Volume v = null;
            Short key = new Short(id);
            synchronized (sVolumeGuard) {
                v = (Volume) sVolumeMap.get(key);
            }
            if (v != null)
                return v;
        }

        Connection conn = null;
        boolean success = false;
        try {
            conn = DbPool.getConnection();
            Volume v = DbVolume.create(conn, id, name, path,
                                       mboxGroupBits, mboxBits,
                                       fileGroupBits, fileBits);
            success = true;
            Short key = new Short(v.getId());
            synchronized (sVolumeGuard) {
                sVolumeMap.put(key, v);
            }
            return v;
        } finally {
            if (conn != null) {
                if (success)
                    conn.commit();
                else
                    conn.rollback();
                DbPool.quietClose(conn);
            }
        }
    }

    public static Volume update(short id,
                                String name, String path,
                                short mboxGroupBits, short mboxBits,
                                short fileGroupBits, short fileBits)
    throws ServiceException {
        Connection conn = null;
        boolean success = false;
        try {
            Volume v = null;
            Short key = new Short(id);
            conn = DbPool.getConnection();
            v = DbVolume.update(conn, id, name, path,
                                mboxGroupBits, mboxBits,
                                fileGroupBits, fileBits);
            success = true;
            synchronized (sVolumeGuard) {
                sVolumeMap.put(key, v);
            }
            return v;
        } finally {
            if (conn != null) {
                if (success)
                    conn.commit();
                else
                    conn.rollback();
                DbPool.quietClose(conn);
            }
        }
    }

    /**
     * @return true if actual deletion occurred
     * @throws ServiceException
     */
    public static boolean delete(short id)
    throws ServiceException {
        Volume vol = null;
        Short key = new Short(id);

        // Don't allow deleting the current message/index volume.
        synchronized (sVolumeGuard) {
            if (id == sCurrMsgVolume.getId())
                throw ServiceException.FAILURE(
                        "Volume " + id + " cannot be deleted " +
                        "because it is the current message volume", null);
            if (id == sCurrIndexVolume.getId())
                throw ServiceException.FAILURE(
                        "Volume " + id + " cannot be deleted " +
                        "because it is the current index volume", null);

            // Remove from map now.
            vol = (Volume) sVolumeMap.remove(key);
        }

        Connection conn = null;
        boolean success = false;
        try {
            conn = DbPool.getConnection();
            boolean deleted = DbVolume.delete(conn, id);
            success = true;
            return deleted;
        } finally {
            if (conn != null) {
                if (success)
                    conn.commit();
                else
                    conn.rollback();
                DbPool.quietClose(conn);
            }
            if (!success && vol != null) {
                // Ran into database error.  Undo map entry removal.
                synchronized (sVolumeGuard) {
                    sVolumeMap.put(key, vol);
                }
            }
        }
    }

    public static Volume getById(short id) throws ServiceException {
    	Volume v = null;
        Short key = new Short(id);
        synchronized (sVolumeGuard) {
        	v = (Volume) sVolumeMap.get(key);
        }
        if (v != null)
            return v;

        // Look up from db.
        Connection conn = null;
        try {
            conn = DbPool.getConnection();
            v = DbVolume.get(conn, id);
            if (v != null) {
            	synchronized (sVolumeGuard) {
            		sVolumeMap.put(key, v);
                }
                return v;
            } else {
            	throw ServiceException.FAILURE("No such volume " + id, null);
            }
        } finally {
            if (conn != null)
                DbPool.quietClose(conn);
        }
    }

    public static List /*<Volume>*/ getAll() throws ServiceException {
        List volumes;
        synchronized (sVolumeGuard) {
        	volumes = new ArrayList(sVolumeMap.values());
        }
        return volumes;
    }

    /**
     * Don't cache the returned Volume object.  Updates to volume information
     * by admin will create a different Volume object for the same volume ID.
     * @return
     */
    public static Volume getCurrentMessageVolume() {
    	synchronized (sVolumeGuard) {
    		return sCurrMsgVolume;
        }
    }

    /**
     * Don't cache the returned Volume object.  Updates to volume information
     * by admin will create a different Volume object for the same volume ID.
     * @return
     */
    public static Volume getCurrentIndexVolume() {
        synchronized (sVolumeGuard) {
            return sCurrIndexVolume;
        }
    }

    // TODO: database update
    public static void setCurrentVolume(short id, int volType)
    throws ServiceException {
        Connection conn = null;
        boolean success = false;
        try {
            conn = DbPool.getConnection();
            DbVolume.updateCurrentVolume(conn, volType, id);

            Short key = new Short(id);
            synchronized (sVolumeGuard) {
                Volume v = (Volume) sVolumeMap.get(key);
                if (v != null) {
                    if (volType == TYPE_MESSAGE)
                        sCurrMsgVolume = v;
                    else
                        sCurrIndexVolume = v;
                } else
                    throw ServiceException.FAILURE("Unknown volume ID " + id, null);
            }

            success = true;
        } finally {
            if (conn != null) {
                if (success)
                    conn.commit();
                else
                    conn.rollback();
                DbPool.quietClose(conn);
            }
        }
    }



    private short mId;
    private String mName;
    private String mRootPath;  // root of the volume
    private String mIncomingMsgDir;

    private short mMboxGroupBits;
    private short mMboxBits;
    private short mFileGroupBits;
    private short mFileBits;

    private int mMboxGroupBitMask;
    private int mFileGroupBitMask;

    public Volume(short id, String name, String rootPath,
                  short mboxGroupBits, short mboxBits,
                  short fileGroupBits, short fileBits) {
        mId = id;
        mName = name;
        mRootPath = rootPath;
        mIncomingMsgDir = mRootPath + File.separator + INCOMING_DIR;

        mMboxGroupBits = mboxGroupBits;
        mMboxBits = mboxBits;
        mFileGroupBits = fileGroupBits;
        mFileBits = fileBits;

        long mask;
        mask = (long) Math.pow(2, mMboxGroupBits) - 1;
        mMboxGroupBitMask = (int) mask;
        mask = (long) Math.pow(2, mFileGroupBits) - 1;
        mFileGroupBitMask = (int) mask;
    }

    public short getId() { return mId; }
    public String getName() { return mName; }
    public String getRootPath() { return mRootPath; }
    public String getIncomingMsgDir() { return mIncomingMsgDir; }
    public short getMboxGroupBits() { return mMboxGroupBits; }
    public short getMboxBits() { return mMboxBits; }
    public short getFileGroupBits() { return mFileGroupBits; }
    public short getFileBits() { return mFileBits; }

    private StringBuffer getMailboxDirStringBuffer(int mboxId, int type,
                                                   int extraCapacity) {
        String subdir = type == TYPE_INDEX ? SUBDIR_INDEX : SUBDIR_MESSAGE;

        StringBuffer sb;
        int capacity;

        long dir = mboxId >> mMboxBits;
        dir &= mMboxGroupBitMask;

        capacity = mRootPath.length() + 20 + extraCapacity;
        sb = new StringBuffer(capacity);
        sb.append(mRootPath)
            .append(File.separator).append(dir)
            .append(File.separator).append(mboxId)
            .append(File.separator).append(subdir);
        return sb;
    }

    public String getMailboxDir(int mboxId, int type) {
        return getMailboxDirStringBuffer(mboxId, type, 0).toString();
    }

    public String getBlobDir(int mboxId, int itemId) {
        long dir = itemId >> mFileBits;
        dir &= mFileGroupBitMask;

        StringBuffer sb = getMailboxDirStringBuffer(mboxId, TYPE_MESSAGE, 10);
        sb.append(File.separator).append(dir);
        return sb.toString();
    }
}
