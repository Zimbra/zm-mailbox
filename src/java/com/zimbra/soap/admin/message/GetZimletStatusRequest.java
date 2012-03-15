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

import com.zimbra.common.soap.AdminConstants;

// Note: soap-admin.txt implies there is an attrs attribute but the handler doesn't appear to get it.
/**
 * @zm-api-command-description Get status for Zimlets
 * <br />
 * priority is listed in the global list &lt;zimlets> ... &lt;/zimlets> only.  This is because the priority value is
 * relative to other Zimlets in the list.  The same Zimlet may show different priority number depending on what
 * other Zimlets priorities are.  the same Zimlet will show priority 0 if all by itself, or priority 3 if there are
 * three other Zimlets with higher priority.
 */
@XmlRootElement(name=AdminConstants.E_GET_ZIMLET_STATUS_REQUEST)
public class GetZimletStatusRequest {
}
