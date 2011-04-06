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

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name=MailConstants.E_NO_OP_RESPONSE)
public class NoOpResponse {

    @XmlAttribute(name=MailConstants.A_WAIT_DISALLOWED, required=false)
    private Boolean waitDisallowed;

    public NoOpResponse() {
    }

    public void setWaitDisallowed(Boolean waitDisallowed) {
        this.waitDisallowed = waitDisallowed;
    }
    public Boolean getWaitDisallowed() { return waitDisallowed; }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
            .add("waitDisallowed", waitDisallowed)
            .toString();
    }
}
