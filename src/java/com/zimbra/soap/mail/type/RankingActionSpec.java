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

package com.zimbra.soap.mail.type;

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class RankingActionSpec {

    // Valid values are "reset" and "delete" (case insensitive)
    /**
     * @zm-api-field-tag action-reset|delete
     * @zm-api-field-description Action to perform - <b>reset|delete</b>.
     * <table>
     * <tr> <td> <b>reset</b> </td> <td> resets the contact ranking table for the account </td> </tr>
     * <tr> <td> <b>delete</b> </td> <td> delete - delete the ranking information for the email address </td> </tr>
     * </table>
     */
    @XmlAttribute(name=MailConstants.A_OPERATION /* op */, required=true)
    private final String operation;

    /**
     * @zm-api-field-tag email-address
     * @zm-api-field-description Email address.  Required if action is "delete"
     */
    @XmlAttribute(name=MailConstants.A_EMAIL /* email */, required=false)
    private String email;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private RankingActionSpec() {
        this((String) null);
    }

    public RankingActionSpec(String operation) {
        this.operation = operation;
    }

    public void setEmail(String email) { this.email = email; }
    public String getOperation() { return operation; }
    public String getEmail() { return email; }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("operation", operation)
            .add("email", email);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
