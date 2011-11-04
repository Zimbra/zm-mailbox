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

package com.zimbra.soap.mail.message;

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name=MailConstants.E_NO_OP_REQUEST)
public class NoOpRequest {

    @XmlAttribute(name=MailConstants.A_WAIT, required=false)
    private ZmBoolean wait;

    @XmlAttribute(name=MailConstants.A_DELEGATE, required=false)
    private ZmBoolean includeDelegates;

    @XmlAttribute(name=MailConstants.A_LIMIT_TO_ONE_BLOCKED, required=false)
    private ZmBoolean enforceLimit;

    @XmlAttribute(name=MailConstants.A_TIMEOUT, required=false)
    private Long timeout;

    public NoOpRequest() {
    }

    public void setWait(Boolean wait) { this.wait = ZmBoolean.fromBool(wait); }

    public void setIncludeDelegates(Boolean includeDelegates) {
        this.includeDelegates = ZmBoolean.fromBool(includeDelegates);
    }

    public void setEnforceLimit(Boolean enforceLimit) { this.enforceLimit = ZmBoolean.fromBool(enforceLimit); }

    public void setTimeout(Long timeout) { this.timeout = timeout; }

    public Boolean getWait() { return ZmBoolean.toBool(wait); }
    public Boolean getIncludeDelegates() { return ZmBoolean.toBool(includeDelegates); }
    public Boolean getEnforceLimit() { return ZmBoolean.toBool(enforceLimit); }
    public Long getTimeout() { return timeout; }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
            .add("wait", wait)
            .add("includeDelegates", includeDelegates)
            .add("enforceLimit", enforceLimit)
            .add("timeout", timeout)
            .toString();
    }
}
