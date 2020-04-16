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
package com.zimbra.soap.admin.type;

import com.google.common.base.Joiner;
import com.zimbra.common.service.ServiceException;

public enum CountObjectsType {
    userAccount(true, false),
    account(true, false),
    alias(true, false),
    dl(true, false),
    domain(true, false),
    cos(false, false),
    server(false, false),
    calresource(true, false),

    // UC service objects
    accountOnUCService(false, true),
    cosOnUCService(false, true),
    domainOnUCService(false, true),
    
    // for license counting
    internalUserAccount(true, false),
    internalArchivingAccount(true, false),
    internalUserAccountX(true, false);
    
    private boolean allowsDomain;
    private boolean allowsUCService;
    
    private CountObjectsType(boolean allowsDomain, boolean allowsUCService) {
        this.allowsDomain = allowsDomain;
        this.allowsUCService = allowsUCService;
    }

    public static CountObjectsType fromString(String type) throws ServiceException {
        try {
            // for backward compatibility, installer uses userAccounts
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
    
    public boolean allowsDomain() {
        return allowsDomain;
    }
    
    public boolean allowsUCService() {
        return allowsUCService;
    }

}
