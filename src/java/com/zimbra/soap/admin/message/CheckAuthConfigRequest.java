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

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.AdminAttrsImpl;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name=AdminConstants.E_CHECK_AUTH_CONFIG_REQUEST)
public class CheckAuthConfigRequest extends AdminAttrsImpl {

    @XmlAttribute(name=AdminConstants.E_NAME, required=true)
    private String name;
    @XmlAttribute(name=AdminConstants.E_PASSWORD, required=true)
    private String password;

    public CheckAuthConfigRequest() {
        this(null, null);
    }

    public CheckAuthConfigRequest(String name, String password) {
        this.name = name;
        this.password = password;
    }

    public void setName(String name) { this.name = name; }
    public void setPassword(String password) { this.password = password; }
    public String getName() { return name; }
    public String getPassword() { return password; }
}
