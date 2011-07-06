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

public final class OctopusXmlConstants {

    public static final String NAMESPACE_STR = MailConstants.NAMESPACE_STR;
    public static final Namespace NAMESPACE = Namespace.get(NAMESPACE_STR);

    public static final String E_GET_ACTIVITY_STREAM_REQUEST = "GetActivityStreamRequest";
    public static final String E_GET_ACTIVITY_STREAM_RESPONSE = "GetActivityStreamResponse";

    public static final String E_REGISTER_DEVICE_REQUEST = "RegisterDeviceRequest";
    public static final String E_REGISTER_DEVICE_RESPONSE = "RegisterDeviceResponse";

    public static final String E_UPDATE_DEVICE_STATUS_REQUEST = "UpdateDeviceStatusRequest";
    public static final String E_UPDATE_DEVICE_STATUS_RESPONSE = "UpdateDeviceStatusResponse";

    public static final String E_CHECK_DEVICE_STATUS_REQUEST = "CheckDeviceStatusRequest";
    public static final String E_CHECK_DEVICE_STATUS_RESPONSE = "CheckDeviceStatusResponse";

    public static final String E_GET_ALL_DEVICES_REQUEST = "GetAllDevicesRequest";
    public static final String E_GET_ALL_DEVICES_RESPONSE = "GetAllDevicesResponse";

    public static final String E_DOCUMENT_ACTION_REQUEST = "DocumentActionRequest";
    public static final String E_DOCUMENT_ACTION_RESPONSE = "DocumentActionResponse";

    public static final QName GET_ACTIVITY_STREAM_REQUEST = QName.get(E_GET_ACTIVITY_STREAM_REQUEST, NAMESPACE);
    public static final QName GET_ACTIVITY_STREAM_RESPONSE = QName.get(E_GET_ACTIVITY_STREAM_RESPONSE, NAMESPACE);

    public static final QName REGISTER_DEVICE_REQUEST = QName.get(E_REGISTER_DEVICE_REQUEST, NAMESPACE);
    public static final QName REGISTER_DEVICE_RESPONSE = QName.get(E_REGISTER_DEVICE_RESPONSE, NAMESPACE);

    public static final QName UPDATE_DEVICE_STATUS_REQUEST = QName.get(E_UPDATE_DEVICE_STATUS_REQUEST, NAMESPACE);
    public static final QName UPDATE_DEVICE_STATUS_RESPONSE = QName.get(E_UPDATE_DEVICE_STATUS_RESPONSE, NAMESPACE);

    public static final QName CHECK_DEVICE_STATUS_REQUEST = QName.get(E_CHECK_DEVICE_STATUS_REQUEST, NAMESPACE);
    public static final QName CHECK_DEVICE_STATUS_RESPONSE = QName.get(E_CHECK_DEVICE_STATUS_RESPONSE, NAMESPACE);

    public static final QName GET_ALL_DEVICES_REQUEST = QName.get(E_GET_ALL_DEVICES_REQUEST, NAMESPACE);
    public static final QName GET_ALL_DEVICES_RESPONSE = QName.get(E_GET_ALL_DEVICES_RESPONSE, NAMESPACE);

    public static final QName DOCUMENT_ACTION_REQUEST = QName.get(E_DOCUMENT_ACTION_REQUEST, NAMESPACE);
    public static final QName DOCUMENT_ACTION_RESPONSE = QName.get(E_DOCUMENT_ACTION_RESPONSE, NAMESPACE);

    public static final String E_OPERATION = "op";
}
