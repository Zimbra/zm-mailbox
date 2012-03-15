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

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.collect.Lists;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.CookieSpec;

@XmlRootElement(name=AdminConstants.E_CLEAR_COOKIE_REQUEST)
public class ClearCookieRequest {

    /**
     * @zm-api-field-description Specifies cookies to clean
     */
    @XmlElement(name=AdminConstants.E_COOKIE)
    private List<CookieSpec> cookies = Lists.newArrayList();
    
    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private ClearCookieRequest() {
        
    }
    
    public ClearCookieRequest(List<CookieSpec> cookies) {
        this.cookies = cookies;
    }
    
    public void addCookie(CookieSpec cookie) {
        cookies.add(cookie);
    }
}
