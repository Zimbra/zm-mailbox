/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

import com.zimbra.common.service.ServiceException;

public final class VolumeServiceException extends ServiceException {
    private static final long serialVersionUID = -3596326510079311719L;

    public static final String BAD_CURRVOL_CONFIG                           = "volume.BAD_CURRVOL_CONFIG";
    public static final String NO_SUCH_VOLUME                               = "volume.NO_SUCH_VOLUME";
    public static final String NO_SUCH_PATH                                 = "volume.NO_SUCH_PATH";
    public static final String ALREADY_EXISTS                               = "volume.ALREADY_EXISTS";
    public static final String ID_OUT_OF_RANGE                              = "volume.ID_OUT_OF_RANGE";
    public static final String CANNOT_DELETE_VOLUME_IN_USE                  = "volume.CANNOT_DELETE_VOLUME_IN_USE";
    public static final String WRONG_TYPE_CURRVOL                           = "volume.WRONG_TYPE_CURRVOL";
    public static final String CANNOT_DELETE_CURRVOL                        = "volume.CANNOT_DELETE_CURRVOL";
    public static final String CANNOT_CHANGE_TYPE_OF_CURRVOL                = "volume.CANNOT_CHANGE_TYPE_OF_CURRVOL";
    public static final String INVALID_REQUEST                              = "volume.INVALID_REQUEST";
    public static final String NOT_ABSOLUTE_PATH                            = "volume.NOT_ABSOLUTE_PATH";
    public static final String SUBDIR_OF_ANOTHER_VOLUME                     = "volume.SUBDIR_OF_ANOTHER_VOLUME";
    public static final String INVALID_METADATA                             = "volume.INVALID_METADATA";
    public static final String BAD_VOLUME_STORE_TYPE                        = "volume.BAD_STORE_TYPE";
    public static final String BAD_VOLUME_PATH                              = "volume.BAD_PATH";
    public static final String BAD_VOLUME_NAME                              = "volume.BAD_NAME";
    public static final String BAD_VOLUME_TYPE                              = "volume.BAD_TYPE";
    public static final String BAD_VOLUME_COMPRESSION_THRESHOLD             = "volume.BAD_COMPRESSION_THRESHOLD";
    public static final String BAD_VOLUME_CURRENT                           = "volume.BAD_CURRENT";
    public static final String BAD_VOLUME_COMPRESS_BLOBS                    = "volume.BAD_COMPRESS_BLOBS";
    public static final String BAD_VOLUME_USE_IN_FREQUENT_ACCESS            = "volume.BAD_USE_IN_FREQUENT_ACCESS";
    public static final String BAD_VOLUME_USE_INTELLIGENT_TIERING           = "volume.BAD_USE_INTELLIGENT_TIERING";
    public static final String BAD_VOLUME_STORAGE_TYPE                      = "volume.BAD_STORAGE_TYPE";
    public static final String BAD_VOLUME_STORE_MANAGER_CLASS               = "volume.STORE_MANAGER_CLASS";
    public static final String BAD_VOLUME_USE_IN_FREQUENT_ACCESS_THRESHOLD  = "volume.BAD_USE_IN_FREQUENT_ACCESS_THRESHOLD";
    public static final String BAD_VOLUME_GLOBAL_BUCKET_ID                  = "volume.BAD_GLOBAL_BUCKET_ID";

    private VolumeServiceException(String message, String code, boolean isReceiversFault) {
        super(message, code, isReceiversFault);
    }

    VolumeServiceException(String message, String code, boolean isReceiversFault, Throwable cause) {
        super(message, code, isReceiversFault, cause);
    }

    public static VolumeServiceException BAD_CURRVOL_CONFIG(String msg) {
        return new VolumeServiceException("invalid current volumes config: " + msg,
                BAD_CURRVOL_CONFIG, RECEIVERS_FAULT, null);
    }

    public static VolumeServiceException NO_SUCH_VOLUME(int id) {
        return new VolumeServiceException("no such volume: " + id, NO_SUCH_VOLUME, SENDERS_FAULT, null);
    }

    public static VolumeServiceException NO_SUCH_PATH(String path) {
        return new VolumeServiceException("directory does not exist or is not writable: " + path,
                NO_SUCH_PATH, SENDERS_FAULT, null);
    }

    public static VolumeServiceException ALREADY_EXISTS(int id, String name, String path, Throwable t) {
        return new VolumeServiceException("volume with the same id, name, or path already exists: (id=" +
                id + ", name=\"" + name + "\", path=" + path + ")", ALREADY_EXISTS, SENDERS_FAULT, t);
    }

    public static VolumeServiceException ID_OUT_OF_RANGE(int id) {
        return new VolumeServiceException("id " + id + " is out of range [0, " + Volume.ID_MAX + "]",
                ID_OUT_OF_RANGE, SENDERS_FAULT, null);
    }

    public static VolumeServiceException CANNOT_DELETE_VOLUME_IN_USE(int id, Throwable t) {
        return new VolumeServiceException("volume id " + id + " is in use by one or more mailboxes and cannot be deleted",
                CANNOT_DELETE_VOLUME_IN_USE, SENDERS_FAULT, t);
    }

    public static VolumeServiceException CANNOT_DELETE_CURRVOL(int id, String volType) {
        return new VolumeServiceException("volume " + id + " cannot be deleted because it is a current volume: " +
                volType, CANNOT_DELETE_CURRVOL, SENDERS_FAULT, null);
    }

    public static VolumeServiceException WRONG_TYPE_CURRVOL(int id, short currVolType) {
        return new VolumeServiceException("volume " + id + " cannot be used as current volume of type " + currVolType,
                WRONG_TYPE_CURRVOL, SENDERS_FAULT, null);
    }

    public static VolumeServiceException CANNOT_CHANGE_TYPE_OF_CURRVOL(Volume vol, short newType) {
        return new VolumeServiceException("cannot change type of volume \"" + vol.getName() + "\" (id=" + vol.getId() +
                ") to " + newType + " because it is the current " + vol.getType() + " volume",
                CANNOT_CHANGE_TYPE_OF_CURRVOL, SENDERS_FAULT, null);
    }

    public static VolumeServiceException INVALID_REQUEST(String msg) {
        return new VolumeServiceException("invalid request: " + msg, INVALID_REQUEST, SENDERS_FAULT, null);
    }

    public static VolumeServiceException NOT_ABSOLUTE_PATH(String path) {
        return new VolumeServiceException("\"" + path + "\" is not an absolute path",
                NOT_ABSOLUTE_PATH, SENDERS_FAULT, null);
    }

    public static VolumeServiceException SUBDIR_OF_ANOTHER_VOLUME(String path, Volume anotherVol) {
        return new VolumeServiceException("the path \"" + path + "\" is a subdirectory of another volume (id=" +
                anotherVol.getId() + ", path=" + anotherVol.getRootPath() + ")",
                SUBDIR_OF_ANOTHER_VOLUME, SENDERS_FAULT, null);
    }

    public static VolumeServiceException INVALID_METADATA(Throwable cause) {
        return new VolumeServiceException("could not decode metadata", INVALID_METADATA, true, cause);
    }

    public static VolumeServiceException INVALID_REQUEST(String msg, String error) {
        return new VolumeServiceException("invalid request: " + msg, error, SENDERS_FAULT, null);
    }
}
