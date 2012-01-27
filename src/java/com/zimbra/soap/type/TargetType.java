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

package com.zimbra.soap.type;

import java.util.Arrays;
import javax.xml.bind.annotation.XmlEnum;

import com.zimbra.common.service.ServiceException;

/**
 * JAXB analog to {@com.zimbra.cs.account.accesscontrol.TargetType}
 */
@XmlEnum
public enum TargetType {
    // case must match protocol
    account, calresource, cos, dl, group, domain, server,
    xmppcomponent, zimlet, config, global;

    public static TargetType fromString(String s)
    throws ServiceException {
        try {
            return TargetType.valueOf(s);
        } catch (IllegalArgumentException e) {
            throw ServiceException.INVALID_REQUEST(
                    "unknown 'TargetType' key: " + s + ", valid values: " +
                   Arrays.asList(TargetType.values()), null);
        }
    }
}
