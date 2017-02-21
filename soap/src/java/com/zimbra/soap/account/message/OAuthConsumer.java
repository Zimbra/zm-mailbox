/*
 * ***** BEGIN LICENSE BLOCK *****
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
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.soap.account.message;

import javax.xml.bind.annotation.XmlAttribute;
import com.zimbra.common.soap.AccountConstants;

public class OAuthConsumer {
    @XmlAttribute(name=AccountConstants.A_ACCESS_TOKEN)
    private String accessToken;

    @XmlAttribute(name=AccountConstants.A_APPROVED_ON)
    private String approvedOn;

    @XmlAttribute(name=AccountConstants.A_CONSUMER_APP_NAME)
    private String applicationName;

    @XmlAttribute(name=AccountConstants.A_CONSUMER_DEVICE)
    private String device;

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

    public String getApprovedOn() { return approvedOn; }
    public void setApprovedOn(String approvedOn) { this.approvedOn = approvedOn; }

    public String getApplicationName() { return applicationName; }
    public void setApplicationName(String applicationName) { this.applicationName = applicationName; }

    public String getDevice() { return device; }
    public void setDevice(String device) { this.device = device; }
}
