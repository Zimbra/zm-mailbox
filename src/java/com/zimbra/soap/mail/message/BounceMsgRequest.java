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
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.mail.type.BounceMsgSpec;

/**
 * @zm-api-command-description Resend a message
 * <br />
 * Supports (f)rom, (t)o, (c)c, (b)cc, (s)ender "type" on &lt;e> elements
 * <br />
 * (these get mapped to Resent-From, Resent-To, Resent-CC, Resent-Bcc, Resent-Sender headers, which are prepended to
 * copy of existing message)
 * <br />
 * Aside from these prepended headers, message is reinjected verbatim
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=MailConstants.E_BOUNCE_MSG_REQUEST)
public class BounceMsgRequest {

    /**
     * @zm-api-field-description Specification of message to be resent
     */
    @XmlElement(name=MailConstants.E_MSG /* m */, required=true)
    private final BounceMsgSpec msg;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private BounceMsgRequest() {
        this((BounceMsgSpec) null);
    }

    public BounceMsgRequest(BounceMsgSpec msg) {
        this.msg = msg;
    }

    public BounceMsgSpec getMsg() { return msg; }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper.add("msg", msg);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
