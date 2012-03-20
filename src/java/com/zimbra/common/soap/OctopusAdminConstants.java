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

public final class OctopusAdminConstants {

    // private constructor prevents instantiation
    private OctopusAdminConstants() {
    }

    public static final String NAMESPACE_STR = AdminConstants.NAMESPACE_STR;
    public static final Namespace NAMESPACE = Namespace.get(NAMESPACE_STR);

    public static final String E_GET_DEVICES_REQUEST = "GetDevicesRequest";
    public static final String E_GET_DEVICES_RESPONSE = "GetDevicesResponse";
    
    public static final String E_INJECT_STATIC_FILES_REQUEST = "InjectStaticFilesRequest";
    public static final String E_INJECT_STATIC_FILES_RESPONSE = "InjectStaticFilesResponse";

    public static final String E_UPDATE_DEVICE_STATUS_REQUEST = "UpdateDeviceStatusRequest";
    public static final String E_UPDATE_DEVICE_STATUS_RESPONSE = "UpdateDeviceStatusResponse";

    public static final QName GET_DEVICES_REQUEST = QName.get(E_GET_DEVICES_REQUEST, NAMESPACE);
    public static final QName GET_DEVICES_RESPONSE = QName.get(E_GET_DEVICES_RESPONSE, NAMESPACE);
    
    public static final QName INJECT_STATIC_FILES_REQUEST = QName.get(E_INJECT_STATIC_FILES_REQUEST, NAMESPACE);
    public static final QName INJECT_STATIC_FILES_RESPONSE = QName.get(E_INJECT_STATIC_FILES_RESPONSE, NAMESPACE);

    public static final QName UPDATE_DEVICE_STATUS_REQUEST = QName.get(E_UPDATE_DEVICE_STATUS_REQUEST, NAMESPACE);
    public static final QName UPDATE_DEVICE_STATUS_RESPONSE = QName.get(E_UPDATE_DEVICE_STATUS_RESPONSE, NAMESPACE);

}
