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
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlRootElement;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name= AccountConstants.E_SEND_TFA_CODE_RESPONSE)
public class SendTFACodeResponse {

    @XmlEnum
    public enum SendTFACodeStatus {

        @XmlEnumValue("sent") SENT("sent"),
        @XmlEnumValue("not sent") NOT_SENT("not sent"),
        @XmlEnumValue("reset succeeded") RESET_SUCCEEDED("reset succeeded"),
        @XmlEnumValue("reset failed") RESET_FAILED("reset failed");
        private final String status;
        private SendTFACodeStatus(String status) {
            this.status = status;
        }

        @Override
        public String toString() {
            return status;
        }

        public static SendTFACodeStatus fromString(String s) throws ServiceException {
            try {
                return SendTFACodeStatus.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ServiceException.INVALID_REQUEST("unknown operation: "+s, e);
            }
        }
    }

    public SendTFACodeResponse() {
        this((SendTFACodeStatus) null);
    }

    public SendTFACodeResponse(SendTFACodeStatus status) {
        setStatus(status);
    }

    @XmlAttribute(name=AccountConstants.A_STATUS /* status */, required=true)
    private SendTFACodeStatus status;

    public SendTFACodeStatus getStatus() {
        return status;
    }

    public void setStatus(SendTFACodeStatus status) {
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