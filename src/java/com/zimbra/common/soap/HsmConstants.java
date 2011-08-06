/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2009, 2010, 2011 Zimbra, Inc.
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
package com.zimbra.common.soap;

import org.dom4j.Namespace;
import org.dom4j.QName;

public final class HsmConstants {
    public static final String NAMESPACE_STR = AdminConstants.NAMESPACE_STR;
    public static final Namespace NAMESPACE = Namespace.get(NAMESPACE_STR);

    public static final String E_HSM_REQUEST = "HsmRequest";
    public static final String E_HSM_RESPONSE = "HsmResponse";

    public static final String E_GET_HSM_STATUS_REQUEST = "GetHsmStatusRequest";
    public static final String E_GET_HSM_STATUS_RESPONSE = "GetHsmStatusResponse";

    public static final String E_ABORT_HSM_REQUEST = "AbortHsmRequest";
    public static final String E_ABORT_HSM_RESPONSE = "AbortHsmResponse";

    public static final String E_MOVE_BLOBS_REQUEST = "MoveBlobsRequest";
    public static final String E_MOVE_BLOBS_RESPONSE = "MoveBlobsResponse";

    public static final String E_GET_APPLIANCE_HSM_FS_REQUEST = "GetApplianceHSMFSRequest";
    public static final String E_GET_APPLIANCE_HSM_FS_RESPONSE = "GetApplianceHSMFSResponse";

    public static final QName HSM_REQUEST = QName.get(E_HSM_REQUEST, NAMESPACE);
    public static final QName HSM_RESPONSE = QName.get(E_HSM_RESPONSE, NAMESPACE);

    public static final QName GET_HSM_STATUS_REQUEST = QName.get(E_GET_HSM_STATUS_REQUEST, NAMESPACE);
    public static final QName GET_HSM_STATUS_RESPONSE = QName.get(E_GET_HSM_STATUS_RESPONSE, NAMESPACE);

    public static final QName ABORT_HSM_REQUEST = QName.get(E_ABORT_HSM_REQUEST, NAMESPACE);
    public static final QName ABORT_HSM_RESPONSE = QName.get(E_ABORT_HSM_RESPONSE, NAMESPACE);

    public static final QName MOVE_BLOBS_REQUEST = QName.get(E_MOVE_BLOBS_REQUEST, NAMESPACE);
    public static final QName MOVE_BLOBS_RESPONSE = QName.get(E_MOVE_BLOBS_RESPONSE, NAMESPACE);

    public static final QName GET_APPLIANCE_HSM_FS_REQUEST = QName.get(E_GET_APPLIANCE_HSM_FS_REQUEST, NAMESPACE);
    public static final QName GET_APPLIANCE_HSM_FS_RESPONSE = QName.get(E_GET_APPLIANCE_HSM_FS_RESPONSE, NAMESPACE);

    public static final String A_START_DATE = "startDate";
    public static final String A_END_DATE = "endDate";
    public static final String A_RUNNING = "running";
    public static final String A_ABORTED = "aborted";
    public static final String A_WAS_ABORTED = "wasAborted";
    public static final String A_ABORTING = "aborting";
    public static final String A_NUM_MOVED = "numMoved";
    public static final String A_ERROR = "error";
    public static final String A_NUM_BLOBS_MOVED = "numBlobsMoved";
    public static final String A_NUM_MAILBOXES = "numMailboxes";
    public static final String A_TOTAL_MAILBOXES = "totalMailboxes";
    public static final String A_DEST_VOLUME_ID = "destVolumeId";
    public static final String A_SOURCE_VOLUME_IDS = "sourceVolumeIds";
    public static final String A_MAX_BYTES = "maxBytes";
    public static final String A_NUM_BYTES_MOVED = "numBytesMoved";
    public static final String A_QUERY = "query";

    // GetApplianceHSMFS
    public static final String E_FS = "fs";
    public static final String A_SIZE = "size";
}
