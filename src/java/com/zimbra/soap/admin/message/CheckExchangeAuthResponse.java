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

package com.zimbra.soap.admin.message;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.StringValue;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name=AdminConstants.E_CHECK_EXCHANGE_AUTH_RESPONSE)
@XmlType(propOrder = {"code", "message"})
public class CheckExchangeAuthResponse {

    @XmlElement(name=AdminConstants.E_CODE, required=true)
    private final StringValue code;

    @XmlElement(name=AdminConstants.E_MESSAGE, required=false)
    private final StringValue message;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private CheckExchangeAuthResponse() {
        this((StringValue) null, (StringValue) null);
    }

    public CheckExchangeAuthResponse(StringValue code, StringValue message) {
        this.code = code;
        this.message = message;
    }

    public StringValue getCode() { return code; }
    public StringValue getMessage() { return message; }
}
