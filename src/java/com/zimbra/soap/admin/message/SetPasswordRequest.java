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
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.AdminConstants;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_SET_PASSWORD_REQUEST)
@XmlType(propOrder = {})
public class SetPasswordRequest {

    @XmlAttribute(name=AdminConstants.E_ID, required=true)
    private final String id;
    @XmlAttribute(name=AdminConstants.E_NEW_PASSWORD, required=true)
    private final String newPassword;

    /**
     * no-argument constructor wanted by JAXB
     */
    public SetPasswordRequest() {
        this(null, null);
    }

    public SetPasswordRequest(String id, String newPassword) {
        this.id = id;
        this.newPassword = newPassword;
    }

    public String getId() { return id; }
    public String getNewPassword() { return newPassword; }

}
