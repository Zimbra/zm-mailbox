/*
 * ***** BEGIN LICENSE BLOCK *****
 *
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2023 Synacor, Inc.
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

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;

import com.google.common.base.MoreObjects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlRootElement;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name= AccountConstants.E_SEND_TWO_FACTOR_AUTH_CODE_RESPONSE)
public class SendTwoFactorAuthCodeResponse {

    @XmlEnum
    public enum SendTwoFactorAuthCodeStatus {

        @XmlEnumValue("sent") SENT("sent"),
        @XmlEnumValue("not sent") NOT_SENT("not sent"),
        @XmlEnumValue("reset succeeded") RESET_SUCCEEDED("reset succeeded"),
        @XmlEnumValue("reset failed") RESET_FAILED("reset failed");
        private final String status;
        private SendTwoFactorAuthCodeStatus(String status) {
            this.status = status;
        }

        @Override
        public String toString() {
            return status;
        }

        public static SendTwoFactorAuthCodeStatus fromString(String s) throws ServiceException {
            try {
                return SendTwoFactorAuthCodeStatus.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ServiceException.INVALID_REQUEST("unknown operation: "+s, e);
            }
        }
    }

    public SendTwoFactorAuthCodeResponse() {
        this((SendTwoFactorAuthCodeStatus) null);
    }

    public SendTwoFactorAuthCodeResponse(SendTwoFactorAuthCodeStatus status) {
        setStatus(status);
    }

    @XmlElement(name=AccountConstants.A_STATUS /* status */, required=true)
    private SendTwoFactorAuthCodeStatus status;

    public SendTwoFactorAuthCodeStatus getStatus() {
        return status;
    }

    public void setStatus(SendTwoFactorAuthCodeStatus status) {
        this.status = status;
    }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper
                .add("status", status);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this))
                .toString();
    }
}