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

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.AdminAttrsImpl;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_GET_LICENSE_RESPONSE)
@XmlType(propOrder = {})
public class GetLicenseResponse {

    /**
     * @zm-api-field-description Block containing attributes relating to the license
     */
    @XmlElement(name=AdminConstants.E_LICENSE /* license */, required=true)
    private AdminAttrsImpl license;

    /**
     * @zm-api-field-description Block containing attributes relating to activation
     */
    @XmlElement(name=AdminConstants.E_ACTIVATION /* activation */, required=false)
    private AdminAttrsImpl activation;

    /**
     * @zm-api-field-description The info element block contains:
     * <table>
     * <tr> <td> <b>Version</b> </td> <td> ZCS version </td> </tr>
     * <tr> <td> <b>Fingerprint</b> </td> <td> System fingerprint required during activation </td> </tr>
     * <tr> <td> <b>Status</b> </td> <td> License and activation status </td> </tr>
     * <tr> <td> <b>TotalAccounts</b> </td> <td> Current number of accounts </td> </tr>
     * <tr> <td> <b>ArchivingAccounts</b> </td> <td> Current number of archiving accounts</td> </tr>
     * <tr> <td> <b>ServerTime</b> </td> <td> Current server time </td> </tr>
     * </table>
     * <br />
     * The value of <b>TotalAccounts</b> can be -1 which indicates that the account counting is still
     * in progress and the server does not have the count.  The account counting can be initiated by
     * creating an account, use of a Network feature, or by sending a CheckLicense Request.
     */
    @XmlElement(name=AdminConstants.E_INFO /* info */, required=true)
    private AdminAttrsImpl info;

    public GetLicenseResponse() {
    }

    public void setLicense(AdminAttrsImpl license) { this.license = license; }
    public void setActivation(AdminAttrsImpl activation) {
        this.activation = activation;
    }
    public void setInfo(AdminAttrsImpl info) { this.info = info; }

    public AdminAttrsImpl getLicense() { return license; }
    public AdminAttrsImpl getActivation() { return activation; }
    public AdminAttrsImpl getInfo() { return info; }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("license", license)
            .add("activation", activation)
            .add("info", info);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
