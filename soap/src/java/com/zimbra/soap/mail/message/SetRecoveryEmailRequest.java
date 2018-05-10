/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2018 Synacor, Inc.
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

package com.zimbra.soap.mail.message;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.base.Objects;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = MailConstants.E_SET_RECOVERY_EMAIL_REQUEST)
public class SetRecoveryEmailRequest {

    @XmlEnum
    public static enum Op {
        sendCode, validateCode, resendCode, reset;

        public static Op fromString(String s) throws ServiceException {
            try {
                return Op.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ServiceException.INVALID_REQUEST("unknown key: " + s, e);
            }
        }
    }

    /**
     * @zm-api-field-tag op
     * @zm-api-field-description op can be sendCode, validateCode, resendCode or reset.
     */
    @XmlAttribute(name = MailConstants.A_OPERATION /* op */, required = true)
    private Op op;

    /**
     * @zm-api-field-tag recoveryEmailAddress
     * @zm-api-field-description recovery email address
     */
    @XmlAttribute(name = MailConstants.A_RECOVERY_EMAIL_ADDRESS /* recoveryEmailAddress */, required = false)
    private String recoveryEmailAddress;

    /**
     * @zm-api-field-tag recoveryEmailAddressVerificationCode
     * @zm-api-field-description recovery email address verification code
     */
    @XmlAttribute(name = MailConstants.A_RECOVERY_EMAIL_ADDRESS_VERIFICATION_CODE /* recoveryEmailAddressVerificationCode */, required = false)
    private String recoveryEmailAddressVerificationCode;

    public Op getOp() {
        return op;
    }

    public void setOp(Op op) {
        this.op = op;
    }

    public String getRecoveryEmailAddress() {
        return recoveryEmailAddress;
    }

    public void setRecoveryEmailAddress(String recoveryEmailAddress) {
        this.recoveryEmailAddress = recoveryEmailAddress;
    }

    public String getRecoveryEmailAddressVerificationCode() {
        return recoveryEmailAddressVerificationCode;
    }

    public void
        setRecoveryEmailAddressVerificationCode(String recoveryEmailAddressVerificationCode) {
        this.recoveryEmailAddressVerificationCode = recoveryEmailAddressVerificationCode;
    }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper.add("op", op).add("recoveryEmailAddress", recoveryEmailAddress)
            .add("recoveryEmailAddressVerificationCode", recoveryEmailAddressVerificationCode);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
