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

package com.zimbra.soap.account.message;

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.AdminConstants;

/**
 * @zm-api-command-network-edition
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
     * </ul>
     */
    @XmlAttribute(name=AdminConstants.A_FEATURE /* feature */, required=true)
    private String feature;

    public CheckLicenseRequest() {
    }

    public void setFeature(String feature) { this.feature = feature; }
    public String getFeature() { return feature; }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("feature", feature);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
