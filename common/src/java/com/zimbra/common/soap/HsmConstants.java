/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013, 2014, 2016 Synacor, Inc.
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
    
    public static final String E_GET_SCHEDULE_SM_POLICY_REQUEST = "GetScheduleSMPolicyRequest";
    public static final String E_GET_SCHEDULE_SM_POLICY_RESPONSE = "GetScheduleSMPolicyResponse";
    
    public static final String E_SCHEDULE_SMPOLICY_REQUEST = "ScheduleSMPolicyRequest";
    public static final String E_SCHEDULE_SMPOLICY_RESPONSE = "ScheduleSMPolicyResponse";

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
    
    public static final QName GET_SCHEDULE_SM_POLICY_REQUEST = QName.get(E_GET_SCHEDULE_SM_POLICY_REQUEST, NAMESPACE);
    public static final QName GET_SCHEDULE_SM_POLICY_RESPONSE = QName.get(E_GET_SCHEDULE_SM_POLICY_RESPONSE, NAMESPACE);
    
    public static final QName SCHEDULE_SMPOLICY_REQUEST = QName.get(E_SCHEDULE_SMPOLICY_REQUEST, NAMESPACE);
    public static final QName SCHEDULE_SMPOLICY_RESPONSE = QName.get(E_SCHEDULE_SMPOLICY_RESPONSE, NAMESPACE);

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
    
    public static final String ZM_SCHEDULE_SM_POLICY = "zmschedulesmpolicy";
    public static final String A_SM_SCHEDULE_POLICY_ENABLED = "smSchedulePolicyEnabled";
    public static final String A_SM_SCHEDULE_POLICY_START_TIME = "smSchedulePolicyStartTime";
    public static final String A_SM_START = "SM_START:";
    public static final String A_SM_END = ":SM_END";
    public static final String A_SM_SCHEDULE_POLICY_ENABLED_MISSING = "No smSchedulePolicyEnabled defined.";
    public static final String A_SM_TIME_MISSING = "No time defined.";
    public static final String A_SM_INVALID_TIME_FORMAT = "Invalid time format.";
    public static final String A_SM_INVALID_HOURS_FORMAT = "Invalid hour defined.";
    public static final String A_SM_INVALID_MINUTES = "Minutes are not allowed.";
    public static final int A_EXIT_STATUS = 0;
    public static final String A_SM_SCHEDULE_POLICY_ENABLED_INVALID = "Only boolean value allowed in smSchedulePolicyEnabled";
    public static final String A_TRUE = "true";
    public static final String A_FALSE = "false";

    public static final String LICENSE_HSM_ENABLE = "HierarchicalStorageManagementEnabled";
}
