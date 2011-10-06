/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011 Zimbra, Inc.
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

import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.zimbra.common.localconfig.LC;
import com.zimbra.cs.store.IncomingDirectory;
import com.zimbra.soap.admin.type.VolumeInfo;

/**
 * Based on default settings, there are 256 directories. We write blobs for id's 0-4095 to directory 0, 4096-8191 to
 * directory 1, etc. After filling directory 255, we wrap around and write 1048576-1052671 to directory 0 and so on.
 *
 * Number of dirs: 2 ^ groupBits (2 ^ 8 = 256 by default)
 * Number of files per dir before wraparound: 2 ^ bits (2 ^ 12 = 4096 by default)
 */
public final class Volume {

    public static final short ID_AUTO_INCREMENT = -1;
    public static final short ID_NONE = -2;
    public static final short ID_MAX = 255;

    public static final short TYPE_MESSAGE =  1;
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

        public Builder setPath(String path, boolean normalize) throws VolumeServiceException {
            if (normalize) {
                path = volume.normalizePath(path);
                VolumeManager.getInstance().validatePath(path);
            }
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

    public static String getAbsolutePath(String path) {
        return LC.zimbra_relative_volume_path.booleanValue() ? LC.zimbra_home.value() + File.separator + path : path;
    }

    private StringBuilder getMailboxDir(int mboxId, String subdir) {
        StringBuilder result = new StringBuilder();
        int dir = (mboxId >> mboxBits) & mboxGroupBitmask;
        result.append(rootPath).append(File.separator).append(dir).append(File.separator).append(mboxId);
        if (subdir != null) {
            result.append(File.separator).append(subdir);
        }
        return result;
    }

    public String getMailboxDir(int mboxId, short type) {
        return getMailboxDir(mboxId, type == TYPE_INDEX ? SUBDIR_INDEX : SUBDIR_MESSAGE).toString();
    }

    public String getBlobDir(int mboxId, int itemId) {
        long dir = (itemId >> fileBits) & fileGroupBitmask;
        return getMailboxDir(mboxId, SUBDIR_MESSAGE).append(File.separator).append(dir).toString();
    }

    public String getMessageRootDir(int mboxId) {
        return getMailboxDir(mboxId, null).toString();
    }

    /**
     * Make sure the path is an absolute path, and remove all "." and ".." from it. This is similar to
     * {@link File#getCanonicalPath()} except symbolic links are not resolved.
     *
     * If there are too many ".." in the path, navigating to parent directory stops at root.
     *
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
        return Objects.toStringHelper(this)
                .add("id", id).add("type", type).add("name", name).add("path", rootPath)
                .add("mboxGroupBits", mboxGroupBits).add("mboxBits", mboxBits)
                .add("fileGroupBits", fileGroupBits).add("fileBits", fileBits)
                .add("compressBlobs", compressBlobs).add("compressionThreshold",compressionThreshold)
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
