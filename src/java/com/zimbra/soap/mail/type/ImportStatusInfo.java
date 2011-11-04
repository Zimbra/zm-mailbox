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
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.FIELD)
public class ImportStatusInfo {

    @XmlAttribute(name=MailConstants.A_ID /* id */, required=false)
    private String id;

    @XmlAttribute(name=MailConstants.A_DS_IS_RUNNING /* isRunning */, required=false)
    private ZmBoolean running;

    @XmlAttribute(name=MailConstants.A_DS_SUCCESS /* success */, required=false)
    private ZmBoolean success;

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

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("id", id)
            .add("running", running)
            .add("success", success)
            .add("error", error);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
