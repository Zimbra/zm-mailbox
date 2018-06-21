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

import com.google.common.base.MoreObjects;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.type.Channel;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = MailConstants.E_SET_RECOVERY_ACCOUNT_REQUEST)
public class SetRecoveryAccountRequest {

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
     * @zm-api-field-tag recoveryAccount
     * @zm-api-field-description recovery account
     */
    @XmlAttribute(name = MailConstants.A_RECOVERY_ACCOUNT /* recoveryAccount */, required = false)
    private String recoveryAccount;

    /**
     * @zm-api-field-tag recoveryAccountVerificationCode
     * @zm-api-field-description recovery account verification code
     */
    @XmlAttribute(name = MailConstants.A_RECOVERY_ACCOUNT_VERIFICATION_CODE /* recoveryAccountVerificationCode */, required = false)
    private String recoveryAccountVerificationCode;

    /**
     * @zm-api-field-tag channel
     * @zm-api-field-description recovery channel
     */
    @XmlAttribute(name = MailConstants.A_CHANNEL /* channel */, required = false)
    private Channel channel;

    public Op getOp() {
        return op;
    }

    public void setOp(Op op) {
        this.op = op;
    }

    public String getRecoveryAccount() {
        return recoveryAccount;
    }

    public void setRecoveryAccount(String recoveryAccount) {
        this.recoveryAccount = recoveryAccount;
    }

    public String getRecoveryAccountVerificationCode() {
        return recoveryAccountVerificationCode;
    }

    public void
        setRecoveryAccountVerificationCode(String recoveryAccountVerificationCode) {
        this.recoveryAccountVerificationCode = recoveryAccountVerificationCode;
    }

    /**
     * @return the channel
     */
    public Channel getChannel() {
        return channel;
    }

    /**
     * @param channel the channel to set
     */
    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper.add("op", op).add("recoveryAccount", recoveryAccount)
            .add("recoveryAccountVerificationCode", recoveryAccountVerificationCode)
            .add("channel", channel);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}