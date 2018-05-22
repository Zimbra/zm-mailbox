/*
 * ***** BEGIN LICENSE BLOCK ***** Zimbra Collaboration Suite Server Copyright
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

import com.google.common.base.Objects;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.mail.type.PasswordResetOperation;

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
    private PasswordResetOperation op;

    /**
     * @zm-api-field-description email
     */
    @XmlAttribute(name = MailConstants.A_EMAIL /* email */, required = true)
    private String email;

    public RecoverAccountRequest() {
    }

    public RecoverAccountRequest(PasswordResetOperation op, String email) {
        this.op = op;
        this.email = email;
    }

    /**
     * @return the operation
     */
    public PasswordResetOperation getOp() {
        return op;
    }

    /**
     * @param op
     *            the operation
     */
    public void setOp(PasswordResetOperation op) {
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

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper.add("op", op.toString()).add("email", email);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
