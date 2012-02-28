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

package com.zimbra.soap.sync.message;

import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.SyncConstants;

/**
 * @zm-api-command-description Get status for devices
 */
@XmlRootElement(name=SyncConstants.E_GET_DEVICE_STATUS_REQUEST)
public class GetDeviceStatusRequest {
}
