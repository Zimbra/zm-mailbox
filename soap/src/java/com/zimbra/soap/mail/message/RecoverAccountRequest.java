/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server Copyright
 * (C) 2018 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details. You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>. *****
 * END LICENSE BLOCK *****
 */

package com.zimbra.soap.mail.message;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.soap.mail.type.RecoverAccountOperation;
import com.zimbra.soap.type.Channel;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required false
 * @zm-api-command-description Recover account request
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = MailConstants.E_RECOVER_ACCOUNT_REQUEST)
public final class RecoverAccountRequest {

    /**
     * @zm-api-field-description operation
     */
    @XmlAttribute(name = MailConstants.A_OPERATION /* op */, required = true)
    private RecoverAccountOperation op;

    /**
     * @zm-api-field-description email
     */
    @XmlAttribute(name = MailConstants.A_EMAIL /* email */, required = true)
    private String email;

    /**
     * @zm-api-field-description channel
     */
    @XmlAttribute(name = MailConstants.A_CHANNEL /* channel */, required = false)
    private Channel channel;

    public RecoverAccountRequest() {
    }

    public RecoverAccountRequest(RecoverAccountOperation op, String email, Channel channel) {
        this.op = op;
        this.email = email;
        this.channel = channel;
    }

    /**
     * @return the operation
     */
    public RecoverAccountOperation getOp() {
        return op;
    }

    /**
     * @param op
     *            the operation
     */
    public void setOp(RecoverAccountOperation op) {
        this.op = op;
    }

    /**
     * @return the email
     */
    public String getEmail() {
        return email;
    }

    /**
     * @param email
     *            the email
     */
    public void setEmail(String email) {
        this.email = email;
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
        return helper.add("op", op.toString()).add("email", email).add("channel", channel);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }

    public void validateRecoverAccountRequest() throws ServiceException {
        if (op == null) {
            ZimbraLog.account.debug("%s Invalid op received", "RecoverAccount");
            throw ServiceException.INVALID_REQUEST("Invalid op received", null);
        }
        if (Strings.isNullOrEmpty(email)) {
            ZimbraLog.account.debug("%s Invalid email received", "RecoverAccount");
            throw ServiceException.INVALID_REQUEST("\"email\" not received in request.", null);
        }
        if (channel == null) {
            ZimbraLog.account.debug("%s Invalid channel received, setting to default \"email\"", "RecoverAccount");
            channel = Channel.EMAIL;
        }
    }
}