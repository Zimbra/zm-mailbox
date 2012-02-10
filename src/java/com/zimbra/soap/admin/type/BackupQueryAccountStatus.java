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

package com.zimbra.soap.admin.type;

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;

import com.zimbra.common.soap.BackupConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class BackupQueryAccountStatus {

    /**
     * @zm-api-field-tag account-email
     * @zm-api-field-description Account email
     */
    @XmlAttribute(name=BackupConstants.A_NAME /* name */, required=true)
    private final String name;

    /**
     * @zm-api-field-tag status
     * @zm-api-field-description Status - <b>NOTSTARTED|INPROGRESS|COMPLETED|ERROR</b>
     */
    @XmlAttribute(name=BackupConstants.A_STATUS /* status */, required=true)
    private final String status;

    /**
     * @zm-api-field-tag error-message
     * @zm-api-field-description Error message
     */
    @XmlAttribute(name=BackupConstants.A_ERROR_MESSAGE /* errorMessage */, required=false)
    private String errorMessage;

    /**
     * @zm-api-field-tag error-stack-trace
     * @zm-api-field-description Error stack trace, if available
     */
    @XmlValue
    private String trace;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private BackupQueryAccountStatus() {
        this((String) null, (String) null);
    }

    public BackupQueryAccountStatus(String name, String status) {
        this.name = name;
        this.status = status;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    public void setTrace(String trace) { this.trace = trace; }
    public String getName() { return name; }
    public String getStatus() { return status; }
    public String getErrorMessage() { return errorMessage; }
    public String getTrace() { return trace; }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("name", name)
            .add("status", status)
            .add("errorMessage", errorMessage)
            .add("trace", trace);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
