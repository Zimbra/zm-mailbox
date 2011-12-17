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
package com.zimbra.common.soap;

import org.dom4j.Namespace;
import org.dom4j.QName;

public final class SyncAdminConstants {
    public static final String NAMESPACE_STR = AdminConstants.NAMESPACE_STR;
    public static final Namespace NAMESPACE = Namespace.get(NAMESPACE_STR);

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

    public static final String E_LAST_USED_DATE = "lastUsedDate";

    public static final String A_COUNT = "count";
    public static final String A_DATE = "date";
}
