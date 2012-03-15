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
import javax.xml.bind.annotation.XmlValue;

import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class MiniCalError {

    /**
     * @zm-api-field-tag calendar-folder-id
     * @zm-api-field-description ID for calendar folder that couldn't be accessed
     */
    @XmlAttribute(name=MailConstants.A_ID /* id */, required=true)
    private final String id;

    /**
     * @zm-api-field-tag ServiceException-error-code
     * @zm-api-field-description ServiceException error code - service.PERM_DENIED, mail.NO_SUCH_FOLDER,
     * account.NO_SUCH_ACCOUNT, etc.
     */
    @XmlAttribute(name=MailConstants.A_CAL_CODE /* code */, required=true)
    private final String code;

    /**
     * @zm-api-field-tag error-msg-from-exception
     * @zm-api-field-description Error message from the exception (but no stack trace)
     */
    @XmlValue
    private String errorMessage;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private MiniCalError() {
        this((String) null, (String) null);
    }

    public MiniCalError(String id, String code) {
        this.id = id;
        this.code = code;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    public String getId() { return id; }
    public String getCode() { return code; }
    public String getErrorMessage() { return errorMessage; }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("id", id)
            .add("code", code)
            .add("errorMessage", errorMessage);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
