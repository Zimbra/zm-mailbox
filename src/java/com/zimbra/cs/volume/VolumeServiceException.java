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

import com.zimbra.common.service.ServiceException;

public final class VolumeServiceException extends ServiceException {
    private static final long serialVersionUID = -3596326510079311719L;

    public static final String BAD_CURRVOL_CONFIG            = "volume.BAD_CURRVOL_CONFIG";
    public static final String NO_SUCH_VOLUME                = "volume.NO_SUCH_VOLUME";
    public static final String NO_SUCH_PATH                  = "volume.NO_SUCH_PATH";
    public static final String ALREADY_EXISTS                = "volume.ALREADY_EXISTS";
    public static final String ID_OUT_OF_RANGE               = "volume.ID_OUT_OF_RANGE";
    public static final String CANNOT_DELETE_VOLUME_IN_USE   = "volume.CANNOT_DELETE_VOLUME_IN_USE";
    public static final String WRONG_TYPE_CURRVOL            = "volume.WRONG_TYPE_CURRVOL";
    public static final String CANNOT_DELETE_CURRVOL         = "volume.CANNOT_DELETE_CURRVOL";
    public static final String CANNOT_CHANGE_TYPE_OF_CURRVOL = "volume.CANNOT_CHANGE_TYPE_OF_CURRVOL";
    public static final String INVALID_REQUEST               = "volume.INVALID_REQUEST";
    public static final String NOT_ABSOLUTE_PATH             = "volume.NOT_ABSOLUTE_PATH";
    public static final String SUBDIR_OF_ANOTHER_VOLUME      = "volume.SUBDIR_OF_ANOTHER_VOLUME";

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
        return new VolumeServiceException("volume " + id + " cannot be used as current volume of type " + currVolType +
                " (" + VolumeCLI.getTypeName(currVolType) + ")", WRONG_TYPE_CURRVOL, SENDERS_FAULT, null);
    }

    public static VolumeServiceException CANNOT_CHANGE_TYPE_OF_CURRVOL(Volume vol, short newType) {
        String newVolTypeName = VolumeCLI.getTypeName(newType);
        String currVolTypeName = VolumeCLI.getTypeName(vol.getType());
        return new VolumeServiceException("cannot change type of volume \"" + vol.getName() + "\" (id=" + vol.getId() +
                ") to " + newVolTypeName + " because it is the current " + currVolTypeName + " volume",
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
}
