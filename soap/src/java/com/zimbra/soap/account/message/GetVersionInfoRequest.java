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

package com.zimbra.soap.account.message;

import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AccountConstants;

/**
 * @zm-api-command-auth-required false - if version information shouldn't be exposed a fault will be thrown
 * @zm-api-command-admin-auth-required false
 * @zm-api-command-description Get Version information
 * <br>
 * Note: This request will return a SOAP fault if the <b>zimbraSoapExposeVersion</b> server/globalconfig attribute is
 * set to FALSE.
 */
@XmlRootElement(name=AccountConstants.E_GET_VERSION_INFO_REQUEST)
public class GetVersionInfoRequest {
}
