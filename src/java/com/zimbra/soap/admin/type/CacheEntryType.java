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

import javax.xml.bind.annotation.XmlEnum;

import com.google.common.base.Joiner;

import com.zimbra.common.service.ServiceException;

// TODO: Use this in ZimbraServer code instead of Provisioning.CacheEntryType
@XmlEnum
public enum CacheEntryType {
    // non ldap entries
    acl, locale, skin, uistrings, license,
    // ldap entries
    account, config, globalgrant, cos, domain, galgroup, group, mime, server, zimlet;

    private static Joiner PIPE_JOINER = Joiner.on("|");

    public static CacheEntryType fromString(String s) throws ServiceException {
        try {
            return CacheEntryType.valueOf(s);
        } catch (IllegalArgumentException e) {
            throw ServiceException.INVALID_REQUEST("unknown cache type: "+s, e);
        }
    }

    public static String names() {
        return PIPE_JOINER.join(CacheEntryType.values());
    }
}
