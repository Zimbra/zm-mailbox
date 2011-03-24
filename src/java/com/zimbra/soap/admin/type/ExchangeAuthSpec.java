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

package com.zimbra.soap.admin.type;

import java.util.Arrays;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlEnum;

import com.zimbra.common.service.ServiceException;

import com.zimbra.common.soap.AdminConstants;

@XmlAccessorType(XmlAccessType.FIELD)
public class ExchangeAuthSpec {

    @XmlEnum
    public enum AuthScheme {
        // case must match protocol
        name, id, foreignPrincipal, adminName, appAdminName, krb5Principal;

        public static AuthScheme fromString(String s)
        throws ServiceException {
            try {
                return AuthScheme.valueOf(s);
            } catch (IllegalArgumentException e) {
               throw ServiceException.INVALID_REQUEST("unknown AuthScheme: " +
                       s + ", valid values: " +
                       Arrays.asList(AuthScheme.values()), null);
            }
        }
    }

    @XmlAttribute(name=AdminConstants.A_URL, required=true)
    private final String url;

    @XmlAttribute(name=AdminConstants.A_USER, required=true)
    private final String authUserName;

    @XmlAttribute(name=AdminConstants.A_PASS, required=true)
    private final String authPassword;

    @XmlAttribute(name=AdminConstants.A_SCHEME, required=true)
    private final AuthScheme scheme;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private ExchangeAuthSpec() {
        this((String) null, (String) null, (String) null, (AuthScheme) null);
    }

    public ExchangeAuthSpec(String url, String authUserName,
                    String authPassword, AuthScheme scheme) {
        this.url = url;
        this.authUserName = authUserName;
        this.authPassword = authPassword;
        this.scheme = scheme;
    }

    public String getUrl() { return url; }
    public String getAuthUserName() { return authUserName; }
    public String getAuthPassword() { return authPassword; }
    public AuthScheme getScheme() { return scheme; }
}
