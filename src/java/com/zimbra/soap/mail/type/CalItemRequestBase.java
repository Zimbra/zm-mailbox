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
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.NONE)
@XmlType(propOrder = {})
public class CalItemRequestBase {

    @XmlAttribute(name=MailConstants.A_CAL_ECHO /* echo */, required=false)
    private ZmBoolean echo;

    @XmlAttribute(name=MailConstants.A_MAX_INLINED_LENGTH /* max */, required=false)
    private Integer maxSize;

    @XmlAttribute(name=MailConstants.A_WANT_HTML /* html */, required=false)
    private ZmBoolean wantHtml;

    @XmlAttribute(name=MailConstants.A_NEUTER /* neuter */, required=false)
    private ZmBoolean neuter;

    @XmlElement(name=MailConstants.E_MSG /* m */, required=false)
    private Msg msg;

    @XmlAttribute(name=MailConstants.A_CAL_FORCESEND /* forcesend */, required=false)
    private ZmBoolean forceSend;

    public CalItemRequestBase() {
    }

    public void setEcho(Boolean echo) { this.echo = ZmBoolean.fromBool(echo); }
    public void setMaxSize(Integer maxSize) { this.maxSize = maxSize; }
    public void setWantHtml(Boolean wantHtml) { this.wantHtml = ZmBoolean.fromBool(wantHtml); }
    public void setNeuter(Boolean neuter) { this.neuter = ZmBoolean.fromBool(neuter); }
    public void setMsg(Msg msg) { this.msg = msg; }
    public void setForceSend(Boolean force) { this.forceSend = ZmBoolean.fromBool(force); }
    public Boolean getEcho() { return ZmBoolean.toBool(echo); }
    public Integer getMaxSize() { return maxSize; }
    public Boolean getWantHtml() { return ZmBoolean.toBool(wantHtml); }
    public Boolean getNeuter() { return ZmBoolean.toBool(neuter); }
    public Msg getMsg() { return msg; }
    public Boolean getForceSend() { return ZmBoolean.toBool(forceSend); }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("echo", echo)
            .add("maxSize", maxSize)
            .add("wantHtml", wantHtml)
            .add("neuter", neuter)
            .add("msg", msg);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
