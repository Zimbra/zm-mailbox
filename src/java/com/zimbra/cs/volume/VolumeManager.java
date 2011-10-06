/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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
package com.zimbra.cs.volume;

import java.io.File;
import java.util.List;
import java.util.Map;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.DbVolume;
import com.zimbra.cs.db.DbPool.DbConnection;
import com.zimbra.cs.redolog.op.CreateVolume;
import com.zimbra.cs.redolog.op.DeleteVolume;
import com.zimbra.cs.redolog.op.ModifyVolume;
import com.zimbra.cs.redolog.op.RedoableOp;
import com.zimbra.cs.redolog.op.SetCurrentVolume;
import com.zimbra.cs.store.IncomingDirectory;

public final class VolumeManager {

    private static final VolumeManager SINGLETON = new VolumeManager();

    private final Map<Short, Volume> id2volume = Maps.newHashMap();
    private Volume currentMessageVolume;
    private Volume currentSecondaryMessageVolume;
    private Volume currentIndexVolume;

    private VolumeManager() {
        try {
            load();
        } catch (ServiceException e){
            ZimbraLog.store.error("Failed to initialize VolumeManager", e);
        }
    }

    public static VolumeManager getInstance() {
        return SINGLETON;
    }

    private void load() throws ServiceException {
        DbConnection conn = DbPool.getConnection();
        try {
            Map<Short, Volume> volumes = DbVolume.getAll(conn);
            DbVolume.CurrentVolumes current = DbVolume.getCurrentVolumes(conn);
            if (current == null) {
                throw VolumeServiceException.BAD_CURRVOL_CONFIG("Missing current volumes info from configuration");
            }

            Volume msgVol = volumes.get(current.msgVolId);
            if (msgVol == null) {
                throw VolumeServiceException.BAD_CURRVOL_CONFIG("Unknown current message volume " + current.msgVolId);
            }

            Volume secondaryMsgVol = null;
            if (current.secondaryMsgVolId != Volume.ID_NONE) {
                secondaryMsgVol = volumes.get(current.secondaryMsgVolId);
                if (secondaryMsgVol == null) {
                    throw VolumeServiceException.BAD_CURRVOL_CONFIG("Unknown current secondary message volume " +
                            current.secondaryMsgVolId);
                }
            }

            Volume indexVol = volumes.get(current.indexVolId);
            if (indexVol == null) {
                throw VolumeServiceException.BAD_CURRVOL_CONFIG("Unknown current index volume " + current.indexVolId);
            }

            // All looks good.  Update current values.
            id2volume.clear();
            id2volume.putAll(volumes);
            currentMessageVolume = msgVol;
            currentSecondaryMessageVolume = secondaryMsgVol;
            currentIndexVolume = indexVol;
            updateSweptDirectories();
        } finally {
            conn.closeQuietly();
        }
    }

    private void updateSweptDirectories() {
        List<IncomingDirectory> dirs = Lists.newArrayListWithCapacity(id2volume.size());
        for (Volume vol : id2volume.values()) {
            IncomingDirectory dir = vol.getIncomingDirectory();
            if (dir != null) {
                dirs.add(dir);
            }
        }
        IncomingDirectory.setSweptDirectories(dirs);
    }

    public Volume create(Volume volume) throws ServiceException {
        return create(volume, false);
    }

    public Volume create(Volume volume, boolean noRedo) throws ServiceException {
        CreateVolume redoRecorder = null;
        if (!noRedo) {
            redoRecorder = new CreateVolume(volume);
            redoRecorder.start(System.currentTimeMillis());
        }

        boolean success = false;
        DbConnection conn = DbPool.getConnection();
        try {
            volume = DbVolume.create(conn, volume);
            success = true;
            if (!noRedo) {
                redoRecorder.setId(volume.getId());
                redoRecorder.log();
            }
            return volume;
        } finally {
            endTransaction(success, conn, redoRecorder);
            if (success) {
                synchronized (this) {
                    id2volume.put(volume.getId(), volume);
                    updateSweptDirectories();
                }
            }
        }
    }

    public Volume update(Volume update) throws ServiceException {
        return update(update, false);
    }

    public Volume update(Volume update, boolean noRedo) throws ServiceException {
        // Don't allow changing type of a current volume. The volume must be first made non-current before its type can
        // be changed. A volume can be made non-current when another volume is made current for the volume type.
        Volume vol = getVolume(update.getId());
        if (update.getType() != vol.getType()) {
            if (isCurrent(vol)) {
                throw VolumeServiceException.CANNOT_CHANGE_TYPE_OF_CURRVOL(vol, update.getType());
            }
        }

        ModifyVolume redoRecorder = null;
        if (!noRedo) {
            redoRecorder = new ModifyVolume(update);
            redoRecorder.start(System.currentTimeMillis());
        }

        boolean success = false;
        DbConnection conn = DbPool.getConnection();
        try {
            update = DbVolume.update(conn, update);
            success = true;
            if (!noRedo) {
                redoRecorder.log();
            }
            return update;
        } finally {
            endTransaction(success, conn, redoRecorder);
            if (success) {
                synchronized (this) {
                    id2volume.put(update.getId(), update);
                    updateSweptDirectories();
                }
            }
        }
    }

    public boolean delete(short id) throws ServiceException {
        return delete(id, false);
    }

    /**
     * Remove the volume from the system.  Files on the volume being deleted are not removed.
     *
     * @return true if actual deletion occurred
     */
    public boolean delete(short id, boolean noRedo) throws ServiceException {
        DeleteVolume redoRecorder = null;
        if (!noRedo) {
            redoRecorder = new DeleteVolume(id);
            redoRecorder.start(System.currentTimeMillis());
        }

        // Don't allow deleting the current message/index volume.
        synchronized (this) {
            if (id == currentMessageVolume.getId()) {
                throw VolumeServiceException.CANNOT_DELETE_CURRVOL(id, "message");
            }
            if (currentSecondaryMessageVolume != null && id == currentSecondaryMessageVolume.getId()) {
                throw VolumeServiceException.CANNOT_DELETE_CURRVOL(id, "secondary message");
            }
            if (id == currentIndexVolume.getId()) {
                throw VolumeServiceException.CANNOT_DELETE_CURRVOL(id, "index");
            }
        }

        boolean success = false;
        DbConnection conn = DbPool.getConnection();
        try {
            boolean deleted = DbVolume.delete(conn, id);
            if (!noRedo) {
                redoRecorder.log();
            }
            synchronized (this) {
                id2volume.remove(id);
                updateSweptDirectories();
            }
            success = true;
            return deleted;
        } finally {
            endTransaction(success, conn, redoRecorder);
        }
    }

    public Volume getVolume(String id) throws ServiceException {
        try {
            return getVolume(Short.parseShort(id));
        } catch (NumberFormatException e) {
            throw ServiceException.INVALID_REQUEST("invalid volume ID: " + id, e);
        }
    }

    public Volume getVolume(short id) throws ServiceException {
        Volume vol = null;
        synchronized (this) {
            vol = id2volume.get(id);
        }
        if (vol != null) {
            return vol;
        }
        // Look up from db.
        DbConnection conn = DbPool.getConnection();
        try {
            vol = DbVolume.get(conn, id);
            if (vol != null) {
                synchronized (this) {
                    id2volume.put(id, vol);
                }
                return vol;
            } else {
                throw VolumeServiceException.NO_SUCH_VOLUME(id);
            }
        } finally {
            conn.closeQuietly();
        }
    }

    public synchronized List<Volume> getAllVolumes() {
        return ImmutableList.copyOf(id2volume.values());
    }

    /**
     * Don't cache the returned {@link Volume} object. Updates to volume information by admin will create a different
     * {@link Volume} object for the same volume ID.
     */
    public synchronized Volume getCurrentMessageVolume() {
        return currentMessageVolume;
    }

    /**
     * Don't cache the returned {@link Volume} object. Updates to volume information by admin will create a different
     * {@link Volume} object for the same volume ID.
     */
    public synchronized Volume getCurrentSecondaryMessageVolume() {
        return currentSecondaryMessageVolume;
    }

    /**
     * Don't cache the returned {@link Volume} object. Updates to volume information by admin will create a different
     * {@link Volume} object for the same volume ID.
     */
    public synchronized Volume getCurrentIndexVolume() {
        return currentIndexVolume;
    }

    public synchronized boolean isCurrent(Volume vol) {
        return currentMessageVolume == vol || currentSecondaryMessageVolume == vol || currentIndexVolume == vol;
    }

    public void setCurrentVolume(short type, short id) throws ServiceException {
        setCurrentVolume(type, id, false);
    }

    /**
     * Set the current volume of given type. Pass ID_NONE for id to unset.
     */
    public void setCurrentVolume(short type, short id, boolean noRedo) throws ServiceException {
        SetCurrentVolume redoRecorder = null;
        if (!noRedo) {
            redoRecorder = new SetCurrentVolume(type, id);
            redoRecorder.start(System.currentTimeMillis());
        }

        Volume vol = null;
        if (id != Volume.ID_NONE) {
            vol = getVolume(id);
            if (vol.getType() != type) {
                throw VolumeServiceException.WRONG_TYPE_CURRVOL(id, type);
            }
        }

        boolean success = false;
        DbConnection conn = DbPool.getConnection();
        try {
            DbVolume.updateCurrentVolume(conn, type, id);
            synchronized (this) {
                switch (type) {
                    case Volume.TYPE_MESSAGE:
                        currentMessageVolume = vol;
                        break;
                    case Volume.TYPE_MESSAGE_SECONDARY:
                        currentSecondaryMessageVolume = vol;
                        break;
                    case Volume.TYPE_INDEX:
                        currentIndexVolume = vol;
                        break;
                    default:
                        throw new IllegalArgumentException("invalid volume type: " + type);
                }
            }
            success = true;
            if (!noRedo) {
                redoRecorder.log();
            }
        } finally {
            endTransaction(success, conn, redoRecorder);
        }
    }

    private void endTransaction(boolean success, DbConnection conn, RedoableOp redoRecorder)
            throws ServiceException {
        if (success) {
            if (conn != null) {
                conn.commit();
                conn.closeQuietly();
            }
            if (redoRecorder != null) {
                redoRecorder.commit();
            }
        } else {
            if (conn != null) {
                conn.rollback();
                conn.closeQuietly();
            }
            if (redoRecorder != null) {
                redoRecorder.abort();
            }
        }
    }

    void validatePath(String path) throws VolumeServiceException {
        if (Strings.isNullOrEmpty(path)) {
            throw VolumeServiceException.INVALID_REQUEST("Missing volume path");
        }
        path = Volume.getAbsolutePath(path);

        // Make sure path is not a subdirectory of an existing volume. Note this check doesn't work on Windows when path
        // contains drive letter and the volume being compared to doesn't, or vice versa.
        String pathSlashed = path.replaceAll("\\\\", "/");
        synchronized (this) {
            for (Volume vol : getAllVolumes()) {
                String vpath = vol.getRootPath().replaceAll("\\\\", "/");
                int len = vpath.length();
                if (len > 0 && vpath.charAt(len - 1) != '/') {
                    vpath = vpath + "/";
                }
                if (pathSlashed.indexOf(vpath) == 0) {
                    throw VolumeServiceException.SUBDIR_OF_ANOTHER_VOLUME(path, vol);
                }
            }
        }

        File root = new File(path);
        if (!root.exists() || !root.isDirectory() || !root.canWrite()) {
            throw VolumeServiceException.NO_SUCH_PATH(path);
        }
    }

}
