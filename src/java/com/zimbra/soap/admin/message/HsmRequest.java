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

package com.zimbra.soap.admin.message;

import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.HsmConstants;

/**
 * @zm-api-command-description Starts the HSM process, which moves blobs for older messages to the current secondary
 * message volume.  This request is asynchronous.  The progress of the last HSM process can be monitored with
 * <b>GetHsmStatusRequest</b>.  The HSM policy is read from the zimbraHsmPolicy LDAP attribute.
 */
@XmlRootElement(name=HsmConstants.E_HSM_REQUEST)
public class HsmRequest {
}
