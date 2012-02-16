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
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.mail.type.AddMsgSpec;
import com.zimbra.soap.type.ZmBoolean;

/**
 * @zm-api-command-description Add a message
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=MailConstants.E_ADD_MSG_REQUEST)
public class AddMsgRequest {

    /**
     * @zm-api-field-tag filter-sent
     * @zm-api-field-description If set, then do outgoing message filtering if the msg is being added to the Sent
     * folder and has been flagged as sent. Default is unset.
     */
    @XmlAttribute(name=MailConstants.A_FILTER_SENT /* filterSent */, required=false)
    private ZmBoolean filterSent;

    /**
     * @zm-api-field-description Specification of the message to add
     */
    @XmlElement(name=MailConstants.E_MSG /* m */, required=true)
    private final AddMsgSpec msg;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private AddMsgRequest() {
        this((AddMsgSpec) null);
    }

    public AddMsgRequest(AddMsgSpec msg) {
        this.msg = msg;
    }

    public void setFilterSent(Boolean filterSent) {
        this.filterSent = ZmBoolean.fromBool(filterSent);
    }
    public Boolean getFilterSent() { return ZmBoolean.toBool(filterSent); }
    public AddMsgSpec getMsg() { return msg; }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("filterSent", filterSent)
            .add("msg", msg);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
