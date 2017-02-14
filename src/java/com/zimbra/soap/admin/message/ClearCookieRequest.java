/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.soap.admin.message;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.collect.Lists;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.CookieSpec;

/**
 * @zm-api-command-auth-required false
 * @zm-api-command-admin-auth-required false - always allow clearing
 * @zm-api-command-description Clear cookie
 */
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
