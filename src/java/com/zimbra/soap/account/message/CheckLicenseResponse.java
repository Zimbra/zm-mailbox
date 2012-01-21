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
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AccountConstants;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AccountConstants.E_CHECK_LICENSE_RESPONSE)
public class CheckLicenseResponse {

    @XmlEnum
    public enum CheckLicenseStatus {
        @XmlEnumValue("ok") OK("ok"),
        @XmlEnumValue("no") NO("no"),
        @XmlEnumValue("inGracePeriod") IN_GRACE_PERIOD("inGracePeriod");
        private final String name;

        private CheckLicenseStatus(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    /**
     * @zm-api-field-description Status of access to requested licensed feature.
     */
    @XmlAttribute(name=AccountConstants.A_STATUS /* status */, required=true)
    private final CheckLicenseStatus status;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private CheckLicenseResponse() {
        this((CheckLicenseStatus) null);
    }

    public CheckLicenseResponse(CheckLicenseStatus status) {
        this.status = status;
    }

    public CheckLicenseStatus getStatus() { return status; }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("status", status);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
