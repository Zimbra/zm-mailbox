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

import com.zimbra.common.soap.AdminConstants;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_SET_PASSWORD_RESPONSE)
public class SetPasswordResponse {

    /**
     * @zm-api-field-tag message
     * @zm-api-field-description If the password had violated any policy, it is returned in this> element, and the
     * password is still set successfully.
     */
    @XmlElement(name=AdminConstants.E_MESSAGE, required=false)
    private final String message;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private SetPasswordResponse() {
        this(null);
    }

    public SetPasswordResponse(String message) {
        this.message = message;
    }

    public String getMessage() { return message; }
}
