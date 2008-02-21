/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006 Zimbra, Inc.
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

package com.zimbra.cs.store;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.DbVolume;
import com.zimbra.cs.db.DbPool.Connection;
import com.zimbra.cs.redolog.op.CreateVolume;
import com.zimbra.cs.redolog.op.DeleteVolume;
import com.zimbra.cs.redolog.op.ModifyVolume;
import com.zimbra.cs.redolog.op.RedoableOp;
import com.zimbra.cs.redolog.op.SetCurrentVolume;
import com.zimbra.cs.util.Zimbra;

/**
 * @author jhahm
 */
public class Volume {

    public static final short ID_AUTO_INCREMENT = -1;
    public static final short ID_NONE           = -2;
    public static final short ID_MAX            = 255;

    public static final short TYPE_MESSAGE           =  1;
    public static final short TYPE_MESSAGE_SECONDARY =  2;
    public static final short TYPE_INDEX             = 10;

    private static final String SUBDIR_MESSAGE = "msg";
    private static final String SUBDIR_INDEX = "index";

    private static final String INCOMING_DIR = "incoming";

    private static final short DEFAULT_MBOX_GROUP_BITS = 8;
    private static final short DEFAULT_MBOX_BITS = 12;
    private static final short DEFAULT_FILE_GROUP_BITS = 8;
    private static final short DEFAULT_FILE_BITS = 12;

    // sVolumeMap, sCurrMsgVolume, sCurrSecondaryMsgVolume, and
    // sCurrIndexVolume are all synchronized on sVolumeGuard.
    private static final Object sVolumeGuard = new Object();
    private static Map<Short, Volume> sVolumeMap = new HashMap<Short, Volume>();
    private static Volume sCurrMsgVolume;
    private static Volume sCurrSecondaryMsgVolume;
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
            Map<Short, Volume> volumes = DbVolume.getAll(conn);
            DbVolume.CurrentVolumes currVols = DbVolume.getCurrentVolumes(conn);
            if (currVols == null)
                throw VolumeServiceException.BAD_CURRVOL_CONFIG("Missing current volumes info from configuration");

            Volume currMsgVol = volumes.get(new Short(currVols.msgVolId));
            if (currMsgVol == null)
                throw VolumeServiceException.BAD_CURRVOL_CONFIG("Unknown current message volume " + currVols.msgVolId);
            Volume currSecondaryMsgVol = null;
            if (currVols.secondaryMsgVolId != ID_NONE) {
                currSecondaryMsgVol = volumes.get(new Short(currVols.secondaryMsgVolId));
                if (currSecondaryMsgVol == null)
                    throw VolumeServiceException.BAD_CURRVOL_CONFIG("Unknown current secondary message volume " + currVols.secondaryMsgVolId);
            }
            Volume currIndexVol = volumes.get(new Short(currVols.indexVolId));
            if (currIndexVol == null)
                throw VolumeServiceException.BAD_CURRVOL_CONFIG("Unknown current index volume " + currVols.indexVolId);

            // All looks good.  Update current values.
            synchronized (sVolumeGuard) {
            	sVolumeMap.clear();
                sVolumeMap.putAll(volumes);
                sCurrMsgVolume = currMsgVol;
                sCurrSecondaryMsgVolume = currSecondaryMsgVol;
                sCurrIndexVolume = currIndexVol;
            }
        } finally {
            if (conn != null)
                DbPool.quietClose(conn);
        }
    }

    public static void validateID(long id)
    throws VolumeServiceException {
        validateID(id, false);
    }

    public static void validateID(long id, boolean allowReservedVals)
    throws VolumeServiceException {
        if (allowReservedVals && (id == ID_AUTO_INCREMENT || id == ID_NONE))
            return;
        if (id < 1 || id > ID_MAX)
            throw VolumeServiceException.INVALID_REQUEST(
                    "Volume ID " + id + " is outside the range of [1, " +
                    ID_MAX + "]");
    }

    private static void validateType(short type)
    throws VolumeServiceException {
        if (type != TYPE_MESSAGE &&
            type != TYPE_MESSAGE_SECONDARY &&
            type != TYPE_INDEX)
            throw VolumeServiceException.INVALID_REQUEST(
                    "Invalid volume type " + type);
    }

    private static void validatePath(String path)
    throws VolumeServiceException {
        if (path == null || path.length() < 1)
            throw VolumeServiceException.INVALID_REQUEST(
                    "Missing volume path");

        // Make sure path is not a subdirectory of an existing volume.
        // Note this check doesn't work on Windows when path contains
        // drive letter and the volume being compared to doesn't, or
        // vice versa.
        String pathSlashed = path.replaceAll("\\\\", "/");
        synchronized (sVolumeGuard) {
            for (Volume v : getAll()) {
                String vpath = v.getRootPath().replaceAll("\\\\", "/");
                int vpathLen = vpath.length();
                if (vpathLen > 0 && vpath.charAt(vpathLen - 1) != '/')
                    vpath = vpath + "/";
                if (pathSlashed.indexOf(vpath) == 0)
                    throw VolumeServiceException.SUBDIR_OF_ANOTHER_VOLUME(path, v);
            }
        }

        File root = new File(path);
        if (!root.exists() || !root.isDirectory() || !root.canWrite())
            throw VolumeServiceException.NO_SUCH_PATH(path);
    }

    private static void validateArgs(short id, short type,
                                     String name, String path,
                                     short mboxGroupBits, short mboxBits,
                                     short fileGroupBits, short fileBits,
                                     long compressionThreshold)
    throws VolumeServiceException {
        validateID(id, true);
        validateType(type);
        if (name == null || name.length() < 1)
            throw VolumeServiceException.INVALID_REQUEST(
                    "Missing volume name");
        validatePath(path);
        if (compressionThreshold < 0)
            throw VolumeServiceException.INVALID_REQUEST(
                    "compressionThreshold cannot be a negative number");

        // no validation on the bits params for now
    }

    public static Volume create(short id, short type,
            String name, String path,
            short mboxGroupBits, short mboxBits,
            short fileGroupBits, short fileBits,
            boolean compressBlobs, long compressionThreshold)
    throws ServiceException {
        return create(id, type, name, path,
                      mboxGroupBits, mboxBits, fileGroupBits, fileBits,
                      compressBlobs, compressionThreshold, false);
    }

    public static Volume create(short id, short type,
                                String name, String path,
                                short mboxGroupBits, short mboxBits,
                                short fileGroupBits, short fileBits,
                                boolean compressBlobs, long compressionThreshold,
                                boolean noRedo)
    throws ServiceException {
        path = normalizePath(path);
        validateArgs(id, type, name, path,
                     mboxGroupBits, mboxBits, fileGroupBits, fileBits,
                     compressionThreshold);

        // TODO: For now we don't allow non-default values.
        mboxGroupBits = DEFAULT_MBOX_GROUP_BITS;
        mboxBits = DEFAULT_MBOX_BITS;
        fileGroupBits = DEFAULT_FILE_GROUP_BITS;
        fileBits = DEFAULT_FILE_BITS;

        CreateVolume redoRecorder = null;
        if (!noRedo) {
            redoRecorder = new CreateVolume(type, name, path,
                                            mboxGroupBits, mboxBits,
                                            fileGroupBits, fileBits,
                                            compressBlobs, compressionThreshold);
            redoRecorder.start(System.currentTimeMillis());
        }

        Short key = null;
        Volume vol = null;
        Connection conn = null;
        boolean success = false;
        try {
            conn = DbPool.getConnection();
            vol = DbVolume.create(conn, id, type, name, path,
                                  mboxGroupBits, mboxBits,
                                  fileGroupBits, fileBits,
                                  compressBlobs, compressionThreshold);
            success = true;
            if (!noRedo) {
                redoRecorder.setId(vol.getId());
                redoRecorder.log();
            }
            key = new Short(vol.getId());
            return vol;
        } finally {
            endTransaction(success, conn, redoRecorder);
            if (success) {
                synchronized (sVolumeGuard) {
                    sVolumeMap.put(key, vol);
                }
            }
        }
    }

    public static Volume update(short id, short type,
            String name, String path,
            short mboxGroupBits, short mboxBits,
            short fileGroupBits, short fileBits,
            boolean compressBlobs, long compressionThreshold)
    throws ServiceException {
        return update(id, type, name, path,
                      mboxGroupBits, mboxBits, fileGroupBits, fileBits,
                      compressBlobs, compressionThreshold, false);
    }

    public static Volume update(short id, short type,
                                String name, String path,
                                short mboxGroupBits, short mboxBits,
                                short fileGroupBits, short fileBits,
                                boolean compressBlobs, long compressionThreshold,
                                boolean noRedo)
    throws ServiceException {
        path = normalizePath(path);
        validateArgs(id, type, name, path,
                mboxGroupBits, mboxBits, fileGroupBits, fileBits,
                compressionThreshold);

        // TODO: For now we don't allow non-default values.
        mboxGroupBits = DEFAULT_MBOX_GROUP_BITS;
        mboxBits = DEFAULT_MBOX_BITS;
        fileGroupBits = DEFAULT_FILE_GROUP_BITS;
        fileBits = DEFAULT_FILE_BITS;

        // Don't allow changing type of a current volume.  The volume must be
        // first made non-current before its type can be changed.  A volume
        // can be made non-current when another volume is made current for
        // the volume type.
        Volume vol = getById(id);
        if (type != vol.getType()) {
            synchronized (sVolumeGuard) {
                if ((sCurrMsgVolume != null && id == sCurrMsgVolume.getId()) ||
                    (sCurrSecondaryMsgVolume != null && id == sCurrSecondaryMsgVolume.getId()) ||
                    (sCurrIndexVolume != null && id == sCurrIndexVolume.getId())) {
                    throw VolumeServiceException.CANNOT_CHANGE_TYPE_OF_CURRVOL(vol, type);
                }
            }
        }

        ModifyVolume redoRecorder = null;
        if (!noRedo) {
            redoRecorder = new ModifyVolume(id, type, name, path,
                                            mboxGroupBits, mboxBits,
                                            fileGroupBits, fileBits,
                                            compressBlobs, compressionThreshold);
            redoRecorder.start(System.currentTimeMillis());
        }

        Connection conn = null;
        boolean success = false;
        try {
            conn = DbPool.getConnection();
            vol = DbVolume.update(conn, id, type, name, path,
                                  mboxGroupBits, mboxBits,
                                  fileGroupBits, fileBits,
                                  compressBlobs, compressionThreshold);
            success = true;
            if (!noRedo)
                redoRecorder.log();
            return vol;
        } finally {
            endTransaction(success, conn, redoRecorder);
            if (success) {
                Short key = new Short(id);
                synchronized (sVolumeGuard) {
                    sVolumeMap.put(key, vol);
                }
            }
        }
    }

    public static boolean delete(short id) throws ServiceException {
        return delete(id, false);
    }

    /**
     * Remove the volume from the system.  Files on the volume being deleted
     * are not removed.
     * 
     * @param id volume ID
     * @return true if actual deletion occurred
     * @throws ServiceException
     */
    public static boolean delete(short id, boolean noRedo)
    throws ServiceException {
        validateID(id);
        DeleteVolume redoRecorder = null;
        if (!noRedo) {
            redoRecorder = new DeleteVolume(id);
            redoRecorder.start(System.currentTimeMillis());
        }

        Volume vol = null;
        Short key = new Short(id);

        // Don't allow deleting the current message/index volume.
        synchronized (sVolumeGuard) {
            if (id == sCurrMsgVolume.getId())
                throw VolumeServiceException.CANNOT_DELETE_CURRVOL(id, "message");
            if (sCurrSecondaryMsgVolume != null &&
                id == sCurrSecondaryMsgVolume.getId())
                throw VolumeServiceException.CANNOT_DELETE_CURRVOL(id, "secondary message");
            if (id == sCurrIndexVolume.getId())
                throw VolumeServiceException.CANNOT_DELETE_CURRVOL(id, "index");

            // Remove from map now.
            vol = sVolumeMap.remove(key);
        }

        Connection conn = null;
        boolean success = false;
        try {
            conn = DbPool.getConnection();
            boolean deleted = DbVolume.delete(conn, id);
            success = true;
            if (!noRedo)
                redoRecorder.log();
            return deleted;
        } finally {
            endTransaction(success, conn, redoRecorder);
            if (vol != null && !success) {
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
        	v = sVolumeMap.get(key);
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
                throw VolumeServiceException.NO_SUCH_VOLUME(id);
            }
        } finally {
            if (conn != null)
                DbPool.quietClose(conn);
        }
    }

    /**
     * Returns a new <code>List</code> of <code>Volume</code>s
     * that match the specified type.
     */
    public static List /*<Volume>*/ getByType(short type) {
        List volumes = getAll();
        Iterator i = volumes.iterator();
        while (i.hasNext()) {
            Volume v = (Volume) i.next();
            if (v.getType() != type) {
                i.remove();
            }
        }
        return volumes;
    }
    
    /**
     * Returns a new <code>List</code> that contains all <code>Volume</code>s.
     */
    public static List<Volume> getAll() {
        List<Volume> volumes;
        synchronized (sVolumeGuard) {
        	volumes = new ArrayList<Volume>(sVolumeMap.values());
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
    public static Volume getCurrentSecondaryMessageVolume() {
        synchronized (sVolumeGuard) {
            return sCurrSecondaryMsgVolume;
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

    public static void setCurrentVolume(short volType, short id)
    throws ServiceException {
        setCurrentVolume(volType, id, false);
    }

    /**
     * Set the current volume of given type.  Pass ID_NONE for id to unset.
     * @param volType
     * @param id
     * @throws ServiceException
     */
    public static void setCurrentVolume(short volType, short id, boolean noRedo)
    throws ServiceException {
        validateType(volType);
        validateID(id, true);

        SetCurrentVolume redoRecorder = null;
        if (!noRedo) {
            redoRecorder = new SetCurrentVolume(volType, id);
            redoRecorder.start(System.currentTimeMillis());
        }

        Volume vol = null;
        if (id != ID_NONE) {
            vol = getById(id);
            if (vol.getType() != volType)
                throw VolumeServiceException.WRONG_TYPE_CURRVOL(id, volType);
        }

        Connection conn = null;
        boolean success = false;
        try {
            conn = DbPool.getConnection();
            DbVolume.updateCurrentVolume(conn, volType, id);

            synchronized (sVolumeGuard) {
                if (volType == TYPE_MESSAGE)
                    sCurrMsgVolume = vol;
                else if (volType == TYPE_MESSAGE_SECONDARY)
                    sCurrSecondaryMsgVolume = vol;
                else
                    sCurrIndexVolume = vol;
            }

            success = true;
            if (!noRedo)
                redoRecorder.log();
        } finally {
            endTransaction(success, conn, redoRecorder);
        }
    }

    private static void endTransaction(boolean success,
                                       Connection conn,
                                       RedoableOp redoRecorder)
    throws ServiceException {
        if (success) {
            if (conn != null) {
                conn.commit();
                DbPool.quietClose(conn);
            }
            if (redoRecorder != null)
                redoRecorder.commit();
        } else {
            if (conn != null) {
                conn.rollback();
                DbPool.quietClose(conn);
            }
            if (redoRecorder != null)
                redoRecorder.abort();
        }
    }

    /**
     * Make sure the path is an absolute path, and remove all "." and ".."
     * from it.  This is similar to File.getCanonicalPath() except symbolic
     * links are not resolved.
     *
     * If there are too many ".." in the path, navigating to parent directory
     * stops at root.
     *
     * Supports unix absolute path, windows absolute path with or without
     * drive letter, and windows UNC share.
     *
     * @param path
     * @return
     * @throws VolumeServiceException if path is not absolute
     */
    private static String normalizePath(String path)
    throws VolumeServiceException {
        if (path == null || path.length() < 1)
            throw VolumeServiceException.INVALID_REQUEST("Missing volume path");
        StringBuffer newPath = new StringBuffer();
        if (path.matches("^[a-zA-Z]:[/\\\\].*$")) {
            // windows path with drive letter
            newPath.append(path.substring(0, 2)).append(File.separator);
            path = path.substring(3);
        } else if (path.length() >= 2 && path.substring(0, 2).equals("\\\\")) {
            // windows UNC share
            newPath.append("\\\\");
            String original = path;
            path = path.substring(2);
            int slash = path.indexOf('/');
            int backslash = path.indexOf('\\');
            if (slash < 0)
                slash = path.length();
            if (backslash < 0)
                backslash = path.length();
            backslash = Math.min(slash, backslash);
            if (backslash >= 0) {
                newPath.append(path.substring(0, backslash)).append(File.separator);
                path = path.substring(backslash + 1);
            } else {
                throw VolumeServiceException.NOT_ABSOLUTE_PATH(original);
            }
        } else if (path.charAt(0) == '/' || path.charAt(0) == '\\') {
            newPath.append(File.separator);
            path = path.substring(1);
        } else {
            throw VolumeServiceException.NOT_ABSOLUTE_PATH(path);
        }

        String dirs[] = path.split("[/\\\\]");
        if (dirs.length == 0)
            return newPath.toString();

        String newDirs[] = new String[dirs.length];
        int numDirs = 0;
        for (int i = 0; i < dirs.length; i++) {
            String dir = dirs[i];
            if (dir.length() == 0 ||                          // empty
                dir.equals(".") ||                            // single dot
                (dir.length() > 2 && dir.matches("^\\.+$")))  // 3+ dots
                continue;  // ignore
            if (dir.equals("..")) {
                if (numDirs > 0)
                    numDirs--;
            } else {
                newDirs[numDirs] = dir;
                numDirs++;
            }
        }

        for (int i = 0; i < numDirs; i++) {
            newPath.append(newDirs[i]);
            if (i < numDirs - 1)
                newPath.append(File.separator);
        }

        return newPath.toString();
    }



    private short mId;
    private short mType;
    private String mName;
    private String mRootPath;  // root of the volume
    private String mIncomingMsgDir;

    private short mMboxGroupBits;
    private short mMboxBits;
    private short mFileGroupBits;
    private short mFileBits;

    private int mMboxGroupBitMask;
    private int mFileGroupBitMask;
    private boolean mCompressBlobs;
    private long mCompressionThreshold;

    public Volume(short id, short type, String name, String rootPath,
                  short mboxGroupBits, short mboxBits,
                  short fileGroupBits, short fileBits,
                  boolean compressBlobs, long compressionThreshold) {
        mId = id;
        mType = type;
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
        
        mCompressBlobs = compressBlobs;
        mCompressionThreshold = compressionThreshold;
    }

    public short getId() { return mId; }
    public short getType() { return mType; }
    public String getName() { return mName; }
    public String getRootPath() { return mRootPath; }
    public String getIncomingMsgDir() { return mIncomingMsgDir; }
    public short getMboxGroupBits() { return mMboxGroupBits; }
    public short getMboxBits() { return mMboxBits; }
    public short getFileGroupBits() { return mFileGroupBits; }
    public short getFileBits() { return mFileBits; }
    public boolean getCompressBlobs() { return mCompressBlobs; }
    public long getCompressionThreshold() { return mCompressionThreshold; }

    private StringBuffer getMailboxDirStringBuffer(int mboxId, String subdir, int extraCapacity) {
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
        String subdir = type == TYPE_INDEX ? SUBDIR_INDEX : SUBDIR_MESSAGE;
        return getMailboxDirStringBuffer(mboxId, subdir, 0).toString();
    }

    public String getBlobDir(int mboxId, int itemId) {
        return getItemDir(mboxId, itemId, SUBDIR_MESSAGE);
    }

    public String getItemDir(int mboxId, int itemId, String subdir) {
        long dir = itemId >> mFileBits;
        dir &= mFileGroupBitMask;

        StringBuffer sb = getMailboxDirStringBuffer(mboxId, subdir, 10);
        sb.append(File.separator).append(dir);
        return sb.toString();
    }

    public String toString() {
        StringBuffer sb = new StringBuffer("Volume: { id=").append(mId);
        sb.append(", type=").append(mType);
        sb.append(", name=\"").append(mName);
        sb.append("\", rootpath=").append(mRootPath);
        sb.append(", mgbits=").append(mMboxGroupBits);
        sb.append(", mbits=").append(mMboxBits);
        sb.append(", fgbits=").append(mFileGroupBits);
        sb.append(", fbits=").append(mFileBits);
        sb.append(", compressBlobs=").append(mCompressBlobs);
        sb.append(", compressionThreshold=").append(mCompressionThreshold).append(" }");
        return sb.toString();
    }
}
