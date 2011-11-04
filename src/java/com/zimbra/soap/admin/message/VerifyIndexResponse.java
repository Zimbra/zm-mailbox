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

import org.codehaus.jackson.map.annotate.JsonSerialize;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.json.jackson.ContentSerializer;
import com.zimbra.soap.json.jackson.ZmBooleanContentSerializer;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name=AdminConstants.E_VERIFY_INDEX_RESPONSE)
@XmlType(propOrder = {"status", "message"})
public class VerifyIndexResponse {

    @XmlElement(name=AdminConstants.E_STATUS, required=true)
    @JsonSerialize(using=ZmBooleanContentSerializer.class)
    private final ZmBoolean status;
    @XmlElement(name=AdminConstants.E_MESSAGE, required=true)
    @JsonSerialize(using=ContentSerializer.class)
    private final String message;

    /**
     * no-argument constructor wanted by JAXB
     */
     @SuppressWarnings("unused")
    private VerifyIndexResponse() {
        this(false,(String) null);
    }

    public VerifyIndexResponse(boolean status, String message) {
        this.status = ZmBoolean.fromBool(status);
        this.message = message;
    }

    public boolean isStatus() { return ZmBoolean.toBool(status); }
    public String getMessage() { return message; }
}
