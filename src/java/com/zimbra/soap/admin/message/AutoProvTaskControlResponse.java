/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012 Zimbra, Inc.
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

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;

@XmlRootElement(name=AdminConstants.E_AUTO_PROV_TASK_CONTROL_RESPONSE)
public class AutoProvTaskControlResponse {
    
    @XmlEnum
    public static enum Status {
        started,
        running,
        idle,
        stopped
    }

    /**
     * @zm-api-field-description Status - one of <b>started|running|idle|stopped</b>
     */
    @XmlAttribute(name=AdminConstants.A_STATUS, required=true)
    private final Status status;
    
    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private AutoProvTaskControlResponse() {
        this((Status) null);
    }

    public AutoProvTaskControlResponse(Status status) {
        this.status = status;
    }
    
    public Status getStatus() { 
        return status; 
    }
}
