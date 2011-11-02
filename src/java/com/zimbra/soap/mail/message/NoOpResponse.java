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

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=MailConstants.E_NO_OP_RESPONSE)
public final class NoOpResponse {

    @XmlAttribute(name=MailConstants.A_WAIT_DISALLOWED, required=false)
    private ZmBoolean waitDisallowed;

    public NoOpResponse() {
    }

    public NoOpResponse(Boolean waitDisallowed) {
        setWaitDisallowed(waitDisallowed);
    }

    public static NoOpResponse create(Boolean waitDisallowed) {
        return new NoOpResponse(waitDisallowed);
    }

    public void setWaitDisallowed(Boolean waitDisallowed) {
        this.waitDisallowed = ZmBoolean.fromBool(waitDisallowed);
    }
    public Boolean getWaitDisallowed() { return ZmBoolean.toBool(waitDisallowed); }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
            .add("waitDisallowed", waitDisallowed)
            .toString();
    }
}
