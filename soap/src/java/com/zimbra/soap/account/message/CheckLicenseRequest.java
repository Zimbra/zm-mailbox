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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.base.MoreObjects;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.AdminConstants;

/**
 * @zm-api-command-network-edition
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required false
 * @zm-api-command-description Checks whether this account (auth token account or requested account id) is allowed
 * access to the specified feature.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AccountConstants.E_CHECK_LICENSE_REQUEST)
public class CheckLicenseRequest {

    /**
     * @zm-api-field-description The licensable feature.  These are the valid values (which are case-insensitive):
     * <ul>
     * <li> <b>MAPI</b> - Zimbra Connector For Outlook
     * <li> <b>MobileSync</b> - ActiveSync
     * <li> <b>iSync</b> - Apple iSync
     * <li> <b>SMIME</b> - Zimbra SMIME
     * <li> <b>BES</b> - Zimbra Connector for BlackBerry Enterprise Server
     * <li> <b>EWS</b> - Zimbra EWS Server
     * <li> <b>TouchClient</b> - Zimbra Touch Client
     * </ul>
     */
    @XmlAttribute(name=AdminConstants.A_FEATURE /* feature */, required=true)
    private String feature;

    public CheckLicenseRequest() {
    }

    public void setFeature(String feature) { this.feature = feature; }
    public String getFeature() { return feature; }

    public MoreObjects.ToStringHelper addToStringInfo(
                MoreObjects.ToStringHelper helper) {
        return helper
            .add("feature", feature);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this))
                .toString();
    }
}
