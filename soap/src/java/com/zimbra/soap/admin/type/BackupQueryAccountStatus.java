/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.soap.admin.type;

import com.google.common.base.MoreObjects;
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

    public MoreObjects.ToStringHelper addToStringInfo(
                MoreObjects.ToStringHelper helper) {
        return helper
            .add("name", name)
            .add("status", status)
            .add("errorMessage", errorMessage)
            .add("trace", trace);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this))
                .toString();
    }
}
