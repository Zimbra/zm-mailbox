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

import com.zimbra.cs.service.ServiceException;

public class VolumeServiceException extends ServiceException {

    public static final String BAD_CURRVOL_CONFIG    = "volume.BAD_CURRVOL_CONFIG";
    public static final String NO_SUCH_VOLUME        = "volume.NO_SUCH_VOLUME";
    public static final String ALREADY_EXISTS        = "volume.ALREADY_EXISTS";
    public static final String WRONG_TYPE_CURRVOL    = "volume.WRONG_TYPE_CURRVOL";
    public static final String CANNOT_DELETE_CURRVOL = "volume.CANNOT_DELETE_CURRVOL";
    public static final String INVALID_REQUEST       = "volume.INVALID_REQUEST";

    private VolumeServiceException(String message, String code, boolean isReceiversFault) {
        super(message, code, isReceiversFault);
    }

    VolumeServiceException(String message, String code, boolean isReceiversFault, Throwable cause) {
        super(message, code, isReceiversFault, cause);
    }

    public static VolumeServiceException BAD_CURRVOL_CONFIG(String msg) {
        return new VolumeServiceException("invalid current volumes config: "+ msg, BAD_CURRVOL_CONFIG, RECEIVERS_FAULT, null);
    }

    public static VolumeServiceException NO_SUCH_VOLUME(int id) {
        return new VolumeServiceException("no such volume: "+ id, NO_SUCH_VOLUME, SENDERS_FAULT, null);
    }

    public static VolumeServiceException ALREADY_EXISTS(int id, Throwable t) {
        return new VolumeServiceException("volume with that id already exists: " + id, ALREADY_EXISTS, SENDERS_FAULT, t);
    }

    public static VolumeServiceException CANNOT_DELETE_CURRVOL(int id, String volType) {
        return new VolumeServiceException("volume " + id + " cannot be deleted because it is a current volume: " + volType, CANNOT_DELETE_CURRVOL, SENDERS_FAULT, null);
    }

    public static VolumeServiceException WRONG_TYPE_CURRVOL(int id, short currVolType) {
        return new VolumeServiceException("volume " + id + " cannot be used as current volume of type " +
            currVolType + " (" + VolumeUtil.getTypeName(currVolType) + ")", WRONG_TYPE_CURRVOL, SENDERS_FAULT, null);
    }

    public static VolumeServiceException INVALID_REQUEST(String msg) {
        return new VolumeServiceException("invalid request: " + msg, INVALID_REQUEST, SENDERS_FAULT, null);
    }
}
