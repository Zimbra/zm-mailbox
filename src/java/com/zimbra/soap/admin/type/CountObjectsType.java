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

import com.google.common.base.Joiner;
import com.zimbra.common.service.ServiceException;

public enum CountObjectsType {
    userAccount,
    account,
    alias,
    dl,
    domain,
    cos,
    server;
    
    public static CountObjectsType fromString(String type) throws ServiceException {
        try {
            // for backward compatibility
            if ("userAccounts".equals(type)) {
                return userAccount;
            } else {
                return CountObjectsType.valueOf(type);
            }
        } catch (IllegalArgumentException e) {
            throw ServiceException.INVALID_REQUEST("unknown count objects type: " + type, e);
        }
    }
    
    public static String names(String separator) {
        Joiner joiner = Joiner.on(separator);
        return joiner.join(CountObjectsType.values());
    }

}
