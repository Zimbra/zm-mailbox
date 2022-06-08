/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.volume;

import java.io.File;

import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.store.IncomingDirectory;
import com.zimbra.cs.util.Zimbra;
import com.zimbra.soap.admin.type.VolumeInfo;

/**
 * Based on default settings, there are 256 directories. We write blobs for id's 0-4095 to directory 0, 4096-8191 to
 * directory 1, etc. After filling directory 255, we wrap around and write 1048576-1052671 to directory 0 and so on.
 * <p>
 * Number of dirs: 2 ^ groupBits (2 ^ 8 = 256 by default)
 * Number of files per dir before wraparound: 2 ^ bits (2 ^ 12 = 4096 by default)
 */
public final class Volume {

    public static final short ID_AUTO_INCREMENT = -1;
    public static final short ID_NONE = -2;
    public static final short ID_MAX = 255;

    public static final short TYPE_MESSAGE = 1;
    public static final short TYPE_MESSAGE_SECONDARY = 2;
    public static final short TYPE_INDEX = 10;

    private static final String SUBDIR_MESSAGE = "msg";
    private static final String SUBDIR_INDEX = "index";

    private static final String INCOMING_DIR = "incoming";

    private short id = Volume.ID_AUTO_INCREMENT;
    private short type;
    private String name;
    private String rootPath; // root of the volume
    private String incomingMsgDir;
    private IncomingDirectory incoming;
    private short mboxGroupBits = 8;
    private short mboxBits = 12;
    private short fileGroupBits = 8;
    private short fileBits = 12;
    private int mboxGroupBitmask;
    private int fileGroupBitmask;
    private boolean compressBlobs;
    private long compressionThreshold;
    private Metadata metadata;

    private StoreType storeType;

    public static enum StoreType {
        FILE_STORE(1),
        EXTERNAL(2);

        private final int storeType;

        StoreType(final int storeType) {
            this.storeType = storeType;
        }

        public int getStoreType() {
            return storeType;
        }

        public static StoreType getStoreTypeBy(int value) {
            switch (value) {
                case 1:
                    return FILE_STORE;
                case 2:
                    return EXTERNAL;
            }
            return null;
        }
    }

    public static class VolumeMetadata {
        private int lastSyncDate;
        private int currentSyncDate;
        private int groupId;

        private static final String FN_DATE_LASTSYNC = "lsd";
        private static final String FN_DATE_CURRENTSYNC = "csd";
        private static final String FN_LAST_GROUP_ID = "gid";

        Metadata serialize() {
            Metadata meta = new Metadata();
            meta.put(FN_DATE_LASTSYNC, lastSyncDate);
            meta.put(FN_DATE_CURRENTSYNC, currentSyncDate);
            meta.put(FN_LAST_GROUP_ID, groupId);
            return meta;
        }

        public VolumeMetadata(Metadata meta) throws ServiceException {
            this.lastSyncDate = meta.getInt(FN_DATE_LASTSYNC, 0);
            this.currentSyncDate = meta.getInt(FN_DATE_CURRENTSYNC, 0);
            this.groupId = meta.getInt(FN_LAST_GROUP_ID, 0);
        }

        public VolumeMetadata(int lastSyncDate, int currentSyncDate, int groupId) {
            this.lastSyncDate = lastSyncDate;
            this.currentSyncDate = currentSyncDate;
            this.groupId = groupId;
        }

        public int getLastSyncDate() {
            return lastSyncDate;
        }

        public int getCurrentSyncDate() {
            return currentSyncDate;
        }

        public int getGroupId() {
            return groupId;
        }

        public void setLastSyncDate(int date) {
            this.lastSyncDate = date;
        }

        public void setCurrentSyncDate(int date) {
            this.currentSyncDate = date;
        }

        public void setGroupId(int id) {
            this.groupId = id;
        }

        public String toString() {
            return serialize().toString();
        }
    }

    private Volume() {
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(Volume copy) {
        return new Builder(copy);
    }

    public static final class Builder {
        private final Volume volume = new Volume();

        private Builder() {
        }

        private Builder(Volume copy) {
            volume.id = copy.id;
            volume.type = copy.type;
            volume.name = copy.name;
            volume.rootPath = copy.rootPath;
            volume.mboxGroupBits = copy.mboxGroupBits;
            volume.mboxBits = copy.mboxBits;
            volume.fileGroupBits = copy.fileGroupBits;
            volume.fileBits = copy.fileBits;
            volume.compressBlobs = copy.compressBlobs;
            volume.compressionThreshold = copy.compressionThreshold;
            volume.metadata = copy.metadata;
            volume.storeType = copy.storeType;

        }

        public Builder setId(short id) {
            volume.id = id;
            return this;
        }

        public Builder setType(short type) {
            volume.type = type;
            return this;
        }

        public Builder setName(String name) {
            volume.name = name;
            return this;
        }

        public Builder setPath(String path, boolean normalize) throws VolumeServiceException, ServiceException {
            if (normalize) {
                path = volume.normalizePath(path);
                VolumeManager.getInstance().validatePath(path);
            }
            //volume.rootPath = getConfiguredRootPath(path);
            volume.rootPath = path;
            return this;
        }

        public Builder setMboxGroupBits(short bits) {
            volume.mboxGroupBits = bits;
            return this;
        }

        public Builder setMboxBit(short bits) {
            volume.mboxBits = bits;
            return this;
        }

        public Builder setFileGroupBits(short bits) {
            volume.fileGroupBits = bits;
            return this;
        }

        public Builder setFileBits(short bits) {
            volume.fileBits = bits;
            return this;
        }

        public Builder setCompressBlobs(boolean value) {
            volume.compressBlobs = value;
            return this;
        }

        public Builder setCompressionThreshold(long value) {
            volume.compressionThreshold = value;
            return this;
        }

        public Builder setMetadata(VolumeMetadata metadata) {
            volume.metadata = metadata.serialize();
            return this;
        }

        public Builder setStoreType(StoreType storeType) {
            volume.storeType = storeType;
            return this;
        }

        public Volume build() throws VolumeServiceException {
            switch (volume.id) {
                case Volume.ID_AUTO_INCREMENT:
                case Volume.ID_NONE:
                    break;
                default:
                    if (volume.id < 1 || volume.id > Volume.ID_MAX) {
                        throw VolumeServiceException.ID_OUT_OF_RANGE(volume.id);
                    }
                    break;
            }

            switch (volume.type) {
                case Volume.TYPE_MESSAGE:
                case Volume.TYPE_MESSAGE_SECONDARY:
                case Volume.TYPE_INDEX:
                    break;
                default:
                    throw VolumeServiceException.INVALID_REQUEST("Invalid volume type: " + volume.type);
            }

            if (Strings.isNullOrEmpty(volume.name)) {
                throw VolumeServiceException.INVALID_REQUEST("Missing volume name");
            }
            if (Strings.isNullOrEmpty(volume.rootPath)) {
                throw VolumeServiceException.INVALID_REQUEST("Missing volume path");
            }
            if (volume.compressionThreshold < 0) {
                throw VolumeServiceException.INVALID_REQUEST("compressionThreshold cannot be a negative number");
            }

            volume.incomingMsgDir = volume.rootPath + File.separator + INCOMING_DIR;
            switch (volume.type) {
                case TYPE_MESSAGE:
                case TYPE_MESSAGE_SECONDARY:
                    volume.incoming = new IncomingDirectory(volume.incomingMsgDir);
                    break;
            }
            volume.mboxGroupBitmask = (int) ((long) Math.pow(2, volume.mboxGroupBits) - 1L);
            volume.fileGroupBitmask = (int) ((long) Math.pow(2, volume.fileGroupBits) - 1L);
            if (volume.metadata == null) {
                volume.metadata = new VolumeMetadata(0, 0, 0).serialize();
            }
            return volume;
        }
    }

    public short getId() {
        return id;
    }

    public short getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getLocator() {
        return Short.toString(getId());
    }

    public String getRootPath() {
        return rootPath;
    }

    public String getIncomingMsgDir() {
        return incomingMsgDir;
    }

    public IncomingDirectory getIncomingDirectory() {
        return incoming;
    }

    public short getMboxGroupBits() {
        return mboxGroupBits;
    }

    public short getMboxBits() {
        return mboxBits;
    }

    public short getFileGroupBits() {
        return fileGroupBits;
    }

    public short getFileBits() {
        return fileBits;
    }

    public boolean isCompressBlobs() {
        return compressBlobs;
    }

    public long getCompressionThreshold() {
        return compressionThreshold;
    }

    public VolumeMetadata getMetadata() throws ServiceException {
        return new VolumeMetadata(metadata);
    }

    public StoreType getStoreType() {
        return storeType;
    }

    public static String getAbsolutePath(String path) throws ServiceException {
        //return LC.zimbra_relative_volume_path.booleanValue() ? LC.zimbra_home.value() + File.separator + getConfiguredRootPath(path) : path;
        return LC.zimbra_relative_volume_path.booleanValue() ? LC.zimbra_home.value() + File.separator + path : path;
    }

    public static String getConfiguredServerID() throws ServiceException {
        StringBuilder finalPath = new StringBuilder();
        if (Zimbra.isAlwaysOn()) {
            String alwaysOnServerClusterId = Zimbra.getAlwaysOnClusterId();
            String localServerId = Provisioning.getInstance().getLocalServer().getId();
            if (alwaysOnServerClusterId != null) {
                finalPath.append(alwaysOnServerClusterId);
            } else {
                finalPath.append(localServerId);
            }
        } else {
            String localServerId = Provisioning.getInstance().getLocalServer().getId();
            finalPath.append(localServerId);
        }
        return finalPath.toString();
    }

    private StringBuilder getMailboxDir(int mboxId, String subdir) throws ServiceException {
        StringBuilder result = new StringBuilder();
        int dir = (mboxId >> mboxBits) & mboxGroupBitmask;

        result.append(rootPath).append(File.separator);

        if (Provisioning.getInstance().getLocalServer().isConfiguredServerIDForBlobDirEnabled())
            result.append(getConfiguredServerID()).append(File.separator);

        result.append(dir).append(File.separator).append(mboxId);

        if (subdir != null) {
            result.append(File.separator).append(subdir);
        }
        return result;
    }

    public String getMailboxDir(int mboxId, short type) throws ServiceException {
        return getMailboxDir(mboxId, type == TYPE_INDEX ? SUBDIR_INDEX : SUBDIR_MESSAGE).toString();
    }

    public String getBlobDir(int mboxId, int itemId) throws ServiceException {
        long dir = (itemId >> fileBits) & fileGroupBitmask;
        return getMailboxDir(mboxId, SUBDIR_MESSAGE).append(File.separator).append(dir).toString();
    }

    public String getMessageRootDir(int mboxId) throws ServiceException {
        return getMailboxDir(mboxId, null).toString();
    }

    /**
     * Make sure the path is an absolute path, and remove all "." and ".." from it. This is similar to
     * {@link File#getCanonicalPath()} except symbolic links are not resolved.
     * <p>
     * If there are too many ".." in the path, navigating to parent directory stops at root.
     * <p>
     * Supports UNIX absolute path, Windows absolute path with or without drive letter, and windows UNC share.
     *
     * @throws VolumeServiceException if path is not absolute
     */
    private String normalizePath(String path) throws VolumeServiceException {
        // skip normalization for relative path
        if (LC.zimbra_relative_volume_path.booleanValue()) {
            return path;
        }
        StringBuilder result = new StringBuilder();
        if (path.matches("^[a-zA-Z]:[/\\\\].*$")) {
            // windows path with drive letter
            result.append(path.substring(0, 2)).append(File.separator);
            path = path.substring(3);
        } else if (path.length() >= 2 && path.substring(0, 2).equals("\\\\")) {
            // windows UNC share
            result.append("\\\\");
            String original = path;
            path = path.substring(2);
            int slash = path.indexOf('/');
            int backslash = path.indexOf('\\');
            if (slash < 0) {
                slash = path.length();
            }
            if (backslash < 0) {
                backslash = path.length();
            }
            backslash = Math.min(slash, backslash);
            if (backslash >= 0) {
                result.append(path.substring(0, backslash)).append(File.separator);
                path = path.substring(backslash + 1);
            } else {
                throw VolumeServiceException.NOT_ABSOLUTE_PATH(original);
            }
        } else if (path.charAt(0) == '/' || path.charAt(0) == '\\') {
            result.append(File.separator);
            path = path.substring(1);
        } else {
            throw VolumeServiceException.NOT_ABSOLUTE_PATH(path);
        }

        String dirs[] = path.split("[/\\\\]");
        if (dirs.length == 0) {
            return result.toString();
        }
        String newDirs[] = new String[dirs.length];
        int numDirs = 0;
        for (int i = 0; i < dirs.length; i++) {
            String dir = dirs[i];
            // single dot or triple dots
            if (dir.isEmpty() || dir.equals(".") || (dir.length() > 2 && dir.matches("^\\.+$"))) {
                continue;  // ignore
            }
            if (dir.equals("..")) {
                if (numDirs > 0) {
                    numDirs--;
                }
            } else {
                newDirs[numDirs] = dir;
                numDirs++;
            }
        }

        for (int i = 0; i < numDirs; i++) {
            result.append(newDirs[i]);
            if (i < numDirs - 1) {
                result.append(File.separator);
            }
        }
        return result.toString();
    }


    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id).add("type", type).add("name", name).add("path", rootPath)
                .add("mboxGroupBits", mboxGroupBits).add("mboxBits", mboxBits)
                .add("fileGroupBits", fileGroupBits).add("fileBits", fileBits)
                .add("compressBlobs", compressBlobs).add("compressionThreshold", compressionThreshold)
                .add("storeType", storeType)
                .toString();
    }

    public VolumeInfo toJAXB() {
        VolumeInfo jaxb = new VolumeInfo();
        jaxb.setId(id);
        jaxb.setType(type);
        jaxb.setName(name);
        jaxb.setRootPath(rootPath);
        jaxb.setMgbits(mboxGroupBits);
        jaxb.setMbits(mboxBits);
        jaxb.setFgbits(fileGroupBits);
        jaxb.setFbits(fileBits);
        jaxb.setCompressBlobs(compressBlobs);
        jaxb.setCompressionThreshold(compressionThreshold);
        jaxb.setCurrent(VolumeManager.getInstance().isCurrent(this));
        return jaxb;
    }
}
