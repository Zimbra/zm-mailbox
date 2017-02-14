/*
 * ***** BEGIN LICENSE BLOCK *****
 *
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2015, 2016 Synacor, Inc.
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
 *
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.soap.account.message;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.AccountConstants;

@XmlRootElement(name=AccountConstants.E_CREATE_APP_SPECIFIC_PASSWORD_REQUEST)
@XmlType(propOrder = {})
public class CreateAppSpecificPasswordRequest {

    public CreateAppSpecificPasswordRequest() {}

    public CreateAppSpecificPasswordRequest(String appName) {
        setAppName(appName);
    }

    @XmlAttribute(name=AccountConstants.A_APP_NAME)
    private String appName;

    public void setAppName(String name) {this.appName = name; }
    public String getAppName() {return appName; }
}
