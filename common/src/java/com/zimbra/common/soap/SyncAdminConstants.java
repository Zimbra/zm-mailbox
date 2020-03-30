/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016, 2020 Synacor, Inc.
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

public final class SyncAdminConstants {
    public static final String NAMESPACE_STR = AdminConstants.NAMESPACE_STR;
    public static final Namespace NAMESPACE = Namespace.get(NAMESPACE_STR);

    public static final String E_ALLOW_DEVICE_REQUEST = "AllowDeviceRequest";
    public static final String E_ALLOW_DEVICE_RESPONSE = "AllowDeviceResponse";
    public static final String E_GET_DEVICES_COUNT_REQUEST = "GetDevicesCountRequest";
    public static final String E_GET_DEVICES_COUNT_RESPONSE = "GetDevicesCountResponse";
    public static final String E_GET_DEVICES_COUNT_SINCE_LAST_USED_REQUEST = "GetDevicesCountSinceLastUsedRequest";
    public static final String E_GET_DEVICES_COUNT_SINCE_LAST_USED_RESPONSE = "GetDevicesCountSinceLastUsedResponse";
    public static final String E_GET_DEVICES_COUNT_USED_TODAY_REQUEST = "GetDevicesCountUsedTodayRequest";
    public static final String E_GET_DEVICES_COUNT_USED_TODAY_RESPONSE = "GetDevicesCountUsedTodayResponse";
    public static final String E_REMOTE_WIPE_REQUEST = "RemoteWipeRequest";
    public static final String E_REMOTE_WIPE_RESPONSE = "RemoteWipeResponse";
    public static final String E_CANCEL_PENDING_REMOTE_WIPE_REQUEST = "CancelPendingRemoteWipeRequest";
    public static final String E_CANCEL_PENDING_REMOTE_WIPE_RESPONSE = "CancelPendingRemoteWipeResponse";
    public static final String E_GET_DEVICE_STATUS_REQUEST = "GetDeviceStatusRequest";
    public static final String E_GET_DEVICE_STATUS_RESPONSE = "GetDeviceStatusResponse";
    public static final String E_REMOVE_DEVICE_REQUEST = "RemoveDeviceRequest";
    public static final String E_REMOVE_DEVICE_RESPONSE = "RemoveDeviceResponse";
    public static final String E_SUSPEND_DEVICE_REQUEST = "SuspendDeviceRequest";
    public static final String E_SUSPEND_DEVICE_RESPONSE = "SuspendDeviceResponse";
    public static final String E_RESUME_DEVICE_REQUEST = "ResumeDeviceRequest";
    public static final String E_RESUME_DEVICE_RESPONSE = "ResumeDeviceResponse";
    public static final String E_GET_SYNC_STATE_REQUEST = "GetSyncStateRequest";
    public static final String E_GET_SYNC_STATE_RESPONSE = "GetSyncStateResponse";
    public static final String E_REMOVE_STALE_DEVICE_METADATA_REQUEST = "RemoveStaleDeviceMetadataRequest";
    public static final String E_REMOVE_STALE_DEVICE_METADATA_RESPONSE = "RemoveStaleDeviceMetadataResponse";

    public static final QName ALLOW_DEVICE_REQUEST = QName.get(E_ALLOW_DEVICE_REQUEST, NAMESPACE);
    public static final QName ALLOW_DEVICE_RESPONSE = QName.get(E_ALLOW_DEVICE_RESPONSE, NAMESPACE);
    public static final QName GET_DEVICES_COUNT_REQUEST = QName.get(E_GET_DEVICES_COUNT_REQUEST, NAMESPACE);
    public static final QName GET_DEVICES_COUNT_RESPONSE = QName.get(E_GET_DEVICES_COUNT_RESPONSE, NAMESPACE);
    public static final QName GET_DEVICES_COUNT_SINCE_LAST_USED_REQUEST = QName.get(E_GET_DEVICES_COUNT_SINCE_LAST_USED_REQUEST, NAMESPACE);
    public static final QName GET_DEVICES_COUNT_SINCE_LAST_USED_RESPONSE = QName.get(E_GET_DEVICES_COUNT_SINCE_LAST_USED_RESPONSE, NAMESPACE);
    public static final QName GET_DEVICES_COUNT_USED_TODAY_REQUEST = QName.get(E_GET_DEVICES_COUNT_USED_TODAY_REQUEST, NAMESPACE);
    public static final QName GET_DEVICES_COUNT_USED_TODAY_RESPONSE = QName.get(E_GET_DEVICES_COUNT_USED_TODAY_RESPONSE, NAMESPACE);
    public static final QName REMOTE_WIPE_REQUEST = QName.get(E_REMOTE_WIPE_REQUEST, NAMESPACE);
    public static final QName REMOTE_WIPE_RESPONSE = QName.get(E_REMOTE_WIPE_RESPONSE, NAMESPACE);
    public static final QName CANCEL_PENDING_REMOTE_WIPE_REQUEST = QName.get(E_CANCEL_PENDING_REMOTE_WIPE_REQUEST, NAMESPACE);
    public static final QName CANCEL_PENDING_REMOTE_WIPE_RESPONSE = QName.get(E_CANCEL_PENDING_REMOTE_WIPE_RESPONSE, NAMESPACE);
    public static final QName GET_DEVICE_STATUS_REQUEST = QName.get(E_GET_DEVICE_STATUS_REQUEST, NAMESPACE);
    public static final QName GET_DEVICE_STATUS_RESPONSE = QName.get(E_GET_DEVICE_STATUS_RESPONSE, NAMESPACE);
    public static final QName REMOVE_DEVICE_REQUEST = QName.get(E_REMOVE_DEVICE_REQUEST, NAMESPACE);
    public static final QName REMOVE_DEVICE_RESPONSE = QName.get(E_REMOVE_DEVICE_RESPONSE, NAMESPACE);
    public static final QName SUSPEND_DEVICE_REQUEST = QName.get(E_SUSPEND_DEVICE_REQUEST, NAMESPACE);
    public static final QName SUSPEND_DEVICE_RESPONSE = QName.get(E_SUSPEND_DEVICE_RESPONSE, NAMESPACE);
    public static final QName RESUME_DEVICE_REQUEST = QName.get(E_RESUME_DEVICE_REQUEST, NAMESPACE);
    public static final QName RESUME_DEVICE_RESPONSE = QName.get(E_RESUME_DEVICE_RESPONSE, NAMESPACE);
    public static final QName GET_SYNC_STATE_REQUEST = QName.get(E_GET_SYNC_STATE_REQUEST, NAMESPACE);
    public static final QName GET_SYNC_STATE_RESPONSE = QName.get(E_GET_SYNC_STATE_RESPONSE, NAMESPACE);
    public static final QName REMOVE_STALE_DEVICE_METADATA_REQUEST = QName.get(E_REMOVE_STALE_DEVICE_METADATA_REQUEST, NAMESPACE);
    public static final QName REMOVE_STALE_DEVICE_METADATA_RESPONSE = QName.get(E_REMOVE_STALE_DEVICE_METADATA_RESPONSE, NAMESPACE);

    public static final String E_LAST_USED_DATE = "lastUsedDate";

    public static final String A_COUNT = "count";
    public static final String A_DATE = "date";
    public static final String A_LAST_USED_DATE_OLDER_THAN = "lastUsedDateOlderThan";
}
