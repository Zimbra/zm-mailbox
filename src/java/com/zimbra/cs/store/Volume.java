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
 * The Initial Developer of the Original Code is Zimbra, Inc.  Portions
 * created by Zimbra are Copyright (C) 2005 Zimbra, Inc.  All Rights
 * Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.store;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.DbVolume;
import com.zimbra.cs.db.DbPool.Connection;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.util.Zimbra;

/**
 * @author jhahm
 */
public class Volume {

	public static final int TYPE_MESSAGE = 1;
    public static final int TYPE_INDEX = 2;

    private static final String SUBDIR_MESSAGE = "msg";
    private static final String SUBDIR_INDEX = "index";

    private static final String INCOMING_DIR = "incoming";

    // sVolumeMap, sCurrMsgVolume, and sCurrIndexVolume are all
    // synchronized on sVolumeMap.
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
            synchronized (sVolumeMap) {
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

    public static Volume getById(short id) throws ServiceException {
    	Volume v = null;
        Short key = new Short(id);
        synchronized (sVolumeMap) {
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
            	synchronized (sVolumeMap) {
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
        synchronized (sVolumeMap) {
        	volumes = new ArrayList(sVolumeMap.values());
        }
        return volumes;
    }

    public static Volume getCurrentMessageVolume() {
    	synchronized (sVolumeMap) {
    		return sCurrMsgVolume;
        }
    }

    public static Volume getCurrentIndexVolume() {
        synchronized (sVolumeMap) {
            return sCurrIndexVolume;
        }
    }





    private short mId;
    private String mName;
    private String mRootPath;  // root of the volume
    private String mIncomingMsgDir;

    private int mMboxGroupBits;
    private int mMboxBits;
    private int mFileGroupBits;
    private int mFileBits;

    private int mMboxGroupBitMask;
    private int mFileGroupBitMask;

    public Volume(short id, String name, String rootPath,
                  int mboxGroupBits, int mboxBits,
                  int fileGroupBits, int fileBits) {
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

    public StringBuffer getMailboxDirStringBuffer(int mboxId, int type,
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
