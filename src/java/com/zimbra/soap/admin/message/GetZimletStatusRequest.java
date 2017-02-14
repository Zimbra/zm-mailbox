/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.soap.admin.message;

import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;

// Note: soap-admin.txt implies there is an attrs attribute but the handler doesn't appear to get it.
/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required true
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
