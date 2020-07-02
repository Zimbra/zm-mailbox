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

package com.zimbra.soap.mail.type;

import com.google.common.base.MoreObjects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.NONE)
public abstract class ImportStatusInfo {

    /**
     * @zm-api-field-tag datasource-id
     * @zm-api-field-description Data source ID
     */
    @XmlAttribute(name=MailConstants.A_ID /* id */, required=false)
    private String id;

    /**
     * @zm-api-field-tag is-running
     * @zm-api-field-description Whether data is currently being imported from this data source
     */
    @XmlAttribute(name=MailConstants.A_DS_IS_RUNNING /* isRunning */, required=false)
    private ZmBoolean running;

    /**
     * @zm-api-field-tag success
     * @zm-api-field-description Whether the last import completed successfully.  (not returned if the
     * import has not run yet)
     */
    @XmlAttribute(name=MailConstants.A_DS_SUCCESS /* success */, required=false)
    private ZmBoolean success;

    /**
     * @zm-api-field-tag error-message
     * @zm-api-field-description If the last import failed, this is the error message that was returned.  (not
     * returned if the import has not run yet)
     */
    @XmlAttribute(name=MailConstants.A_DS_ERROR /* error */, required=false)
    private String error;

    public ImportStatusInfo() {
    }

    public void setId(String id) { this.id = id; }
    public void setRunning(Boolean running) { this.running = ZmBoolean.fromBool(running); }
    public void setSuccess(Boolean success) { this.success = ZmBoolean.fromBool(success); }
    public void setError(String error) { this.error = error; }
    public String getId() { return id; }
    public Boolean getRunning() { return ZmBoolean.toBool(running); }
    public Boolean getSuccess() { return ZmBoolean.toBool(success); }
    public String getError() { return error; }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper
            .add("id", id)
            .add("running", running)
            .add("success", success)
            .add("error", error);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
